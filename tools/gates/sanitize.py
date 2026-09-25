#!/usr/bin/env python3
"""Sanitize gate: nothing that would be pushed or shipped may leak secrets, personal paths, or process notes.

    python tools/gates/sanitize.py

Adapted from the Ninjacat Skies gate (tools/gates/test_sanitized_public_surface.py there). It scans
exactly what git would push (tracked plus untracked-but-not-ignored files), so an ignored file can
never trip it and a new file can never slip past it. Exit 1 on any hit. Run it before every push and
every CurseForge upload.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SELF = "tools/gates/"

SECRETS = {
    "CF_API_KEY-assignment": r"CF_API_KEY\s*=\s*\S+",
    "CF_AUTHOR_TOKEN-assignment": r"CF_AUTHOR_TOKEN\s*=\s*\S+",
    "github-token": r"gh[pousr]_[A-Za-z0-9]{20,}",
    "github-fine-grained-pat": r"github_pat_[A-Za-z0-9_]{20,}",
    "openai-style-key": r"sk-[A-Za-z0-9]{20,}",
    "anthropic-key": r"sk-ant-[A-Za-z0-9_-]{20,}",
    "private-key-block": r"BEGIN (RSA |OPENSSH |EC )?PRIVATE KEY",
    "bcrypt-hash": r"\$2a\$\d{2}\$",
    "windows-user-profile": r"(?i)C:[/\\]+Users[/\\]+[^\/\s\"'`]+",
    "short-windows-profile": r"(?i)[A-Z]:[/\\]+(?:Users|Documents and Settings)[/\\]",
    "unix-user-home": r"(?i)(?:^|[\s\"'`(])(?:/Users|/home)/[a-z0-9_.-]+/",
    "email-address": r"[A-Za-z0-9._%+-]+@(?:gmail|outlook|hotmail|yahoo|proton(?:mail)?|icloud)\.[a-z.]+",
}
# Process notes that belong to the workstation, not to the public mod.
FORBIDDEN = [r"\bgrok\b", r"\.grok[/\\]", r"\bsubagent", r"plan mode", r"agent conversation", r"claude outputs",
             r"scratchpad", r"INTERNAL/", r"AppData[/\\]"]
TEXTISH = re.compile(r"\.(md|txt|json|json5|mcmeta|js|java|toml|gradle|properties|lang|ps1|py|html|xml|yml|yaml|cfg|bat|sh)$")
MUST_IGNORE = ["tools/secrets/.env", ".env", "build/", "run/", ".gradle/"]


def push_set() -> list[str]:
    out = subprocess.run(["git", "ls-files", "--cached", "--others", "--exclude-standard"], cwd=ROOT,
                         capture_output=True, text=True)
    if out.returncode != 0:
        raise SystemExit("sanitize: not a git repository yet (git init first)")
    return [line for line in out.stdout.splitlines() if line]


def main() -> int:
    fails: list[str] = []
    files = push_set()
    for rel in files:
        if rel.startswith(SELF):
            continue
        path = ROOT / rel
        if re.search(r"(^|/)(\.env|.*\.key|credentials.*)$", rel) or rel.startswith("tools/secrets/"):
            fails.append(f"{rel}: secret-like file would be pushed")
            continue
        if not (TEXTISH.search(rel) or path.name in ("README", "LICENSE", ".gitignore", "gradlew")):
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        for name, pattern in SECRETS.items():
            hit = re.search(pattern, text, re.M)
            if hit:
                fails.append(f"{rel}: {name} ({hit.group(0)[:40]})")
        # The ignore list may name what it keeps out; nothing else may.
        for pattern in ([] if rel == ".gitignore" else FORBIDDEN):
            hit = re.search(pattern, text, 0 if pattern == "INTERNAL/" else re.I)
            if hit:
                fails.append(f"{rel}: process note /{pattern}/ ({hit.group(0)})")
        # Docs inside the jar would ship to every player.
        if rel.startswith("src/main/resources/") and path.suffix in (".md", ".txt"):
            fails.append(f"{rel}: document inside the mod's resources would ship in the jar")
    for rel in MUST_IGNORE:
        check = subprocess.run(["git", "check-ignore", "-q", "--no-index", rel], cwd=ROOT)
        if check.returncode != 0:
            fails.append(f".gitignore does not cover {rel}")
    big = [(rel, (ROOT / rel).stat().st_size) for rel in files if (ROOT / rel).is_file()]
    for rel, size in big:
        if size > 5 * 1024 * 1024:
            fails.append(f"{rel}: {size // 1024} KiB is too large to push")
    if fails:
        print(f"FAIL sanitize ({len(fails)} issue(s))")
        for line in fails:
            print(" -", line)
        return 1
    print(f"PASS sanitize ({len(files)} files would be pushed)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
