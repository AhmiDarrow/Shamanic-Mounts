"""PASS/FAIL bookkeeping, RCON, and the showcase-command hand-off for the live client.

The client ({@code gradlew runHarnessClient}) polls build/harness-client/showcase-command.txt.
One command per file. The reply lands in showcase-done.txt: line 1 is the command, line 2 is the result.
"""
import os
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verification_rcon import command as rcon  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / "build" / "harness-client"
PLAYER = "ShamanQA"
RESULTS = []
SECTION = [""]


def say(text):
    print(time.strftime("%H:%M:%S"), text, flush=True)


def section(name):
    SECTION[0] = name
    say(f"== {name}")


def check(label, ok, detail=""):
    label = f"{SECTION[0]}: {label}" if SECTION[0] else label
    RESULTS.append((label, bool(ok)))
    say(f"  {'PASS' if ok else 'FAIL'} {label}" + (f"  ({detail})" if detail else ""))
    return bool(ok)


def summary():
    passed = sum(1 for _, ok in RESULTS if ok)
    failed = [label for label, ok in RESULTS if not ok]
    say(f"done: {passed} passed, {len(failed)} failed")
    for label in failed:
        say(f"  FAILED {label}")
    return len(failed)


def run(cmd):
    out = rcon(cmd)
    if out and (
        "Unknown" in out
        or "Incorrect" in out
        or "Expected" in out
        or "No entity" in out
        or "Can't" in out
        or "cannot" in out.lower()
        or "not loaded" in out
    ):
        say(f"  !! {cmd} -> {out.strip()}")
    return out


def client(cmd, timeout=30):
    """Send one command to HarnessVerification and wait for its result line."""
    done = CLIENT / "showcase-done.txt"
    done.unlink(missing_ok=True)
    # Written whole, then renamed into place, so the client never reads half a command.
    staging = CLIENT / "showcase-command.tmp"
    staging.write_text(cmd + "\n", encoding="utf-8")
    os.replace(staging, CLIENT / "showcase-command.txt")
    end = time.time() + timeout
    while time.time() < end:
        if done.is_file():
            result = done.read_text(encoding="utf-8").splitlines()
            reply = result[1] if len(result) > 1 else "?"
            if not reply.startswith("ok"):
                say(f"  !! client {cmd} -> {reply}")
            return reply
        time.sleep(0.25)
    say(f"  !! client {cmd} timed out")
    return "timeout"


def buttons():
    """(index, class, text) for every widget on the open screen."""
    reply = client("buttons")
    found = []
    if reply.startswith("ok ") and reply[3:]:
        for part in reply[3:].split("|"):
            bits = part.split(":", 2)
            if len(bits) == 3:
                found.append((bits[0], bits[1], bits[2]))
    return reply, found


def press_button(label):
    """Click the Button whose text is exactly `label` (an edit box with the same words is left alone)."""
    reply, found = buttons()
    for index, kind, text in found:
        if kind == "Button" and text == label:
            return client(f"press {index}")
    return "error no button " + label + " in " + reply


def report():
    """The open herd book, as field=value pairs."""
    reply = client("report")
    fields = {}
    if reply.startswith("ok "):
        for part in reply[3:].split("|"):
            if "=" in part:
                key, value = part.split("=", 1)
                fields[key] = value
    return reply, fields


def inventory_count(item):
    out = rcon(f"clear {PLAYER} {item} 0")
    match = re.search(r"Found (\d+)", out)
    return int(match.group(1)) if match else 0


def player_uuid():
    out = rcon(f"data get entity {PLAYER} UUID")
    match = re.search(r"\[I;\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\]", out)
    if not match:
        raise RuntimeError(f"player UUID unavailable ({out.strip()})")
    parts = [int(value) & 0xFFFFFFFF for value in match.groups()]
    hexes = "".join(f"{value:08x}" for value in parts)
    return f"{hexes[0:8]}-{hexes[8:12]}-{hexes[12:16]}-{hexes[16:20]}-{hexes[20:32]}"


def pad():
    """A wide grass floor around the pads at y = 100, with clear air above. Mounts walk and blink far."""
    run("kill @e[type=item]")
    run("fill -24 100 -24 40 100 70 minecraft:grass_block")
    run("fill -24 101 -24 40 106 70 minecraft:air")
    run("setblock 8 101 8 minecraft:stone")
    run("setblock 10 101 8 minecraft:crafting_table")


def log_text(path):
    file = ROOT / path
    if not file.is_file():
        return ""
    return file.read_text(encoding="utf-8", errors="replace")
