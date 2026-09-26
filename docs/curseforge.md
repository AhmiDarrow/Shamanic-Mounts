# CurseForge publishing

CurseForge project **1711650**, confirmed by the owner on 2026-09-25. Upload only when asked.

Release order: gates green (unit tests, the harness sections the change touches, `python tools/gates/sanitize.py`), build, commit, tag `v<version>`, push `main` and the tag, `gh release create`, then:

```
python tools/upload_curseforge.py --jar build/libs/shamanicmounts-<version>.jar --display-name "Shamanic Mounts <version> - <subtitle>" --changelog-file docs/RELEASE_<version>.md
```

The token is read from `tools/secrets/.env` or the Ninjacat Skies checkout beside this one. Record the returned file id below and commit it as `Mark <version> on CurseForge (file <id>)`. Uploads sit in moderation for a while before the public file list shows them.

## What to upload

- Jar: `build/libs/shamanicmounts-<version>.jar` from `./gradlew build`.
- Icon: `docs/public/shamanic-mounts-icon-400.png` (400x400), resampled from `art/branding/shamanic-mounts-logo.png`.
- Description: `docs/public/store-description.md`. `store-description.html` is the same text for the console's HTML editor (`python tools/render_store_html.py`). After the founders sheet is attached on the project, replace the image address in that HTML with the ForgeCDN url.
- Founders sheet: `docs/public/shamanic-mounts-founders.png` (ten lines and the chimera), composed from live harness pictures by `python tools/founders_sheet.py` after `python tools/system_harness.py --only store`.
- Changelog: `docs/RELEASE_<version>.md`, as a heading plus short player-facing bullets. Write that file for the version being sent.
- Tags: Minecraft 1.21.1, NeoForge, Client + Server. Display name `Shamanic Mounts <version> - <subtitle>`.
- Mods-list logo in the jar: `src/main/resources/icon.png` (256x256), `logoFile` in `neoforge.mods.toml`.

The cover is rebuilt with `python tools/store_cover.py` after the store section; regenerate icon sizes with `python tools/brand_icons.py`.

Before any push or upload, run the sanitize gate: `python tools/gates/sanitize.py`.

## 0.1.0

Shamanic Mounts 0.1.0 - The Spirit Herd, CurseForge file **8975876** (uploaded 2026-09-25, release). GitHub release: https://github.com/AhmiDarrow/Shamanic-Mounts/releases/tag/v0.1.0

## 0.1.1

Shamanic Mounts 0.1.1 - Lean Herd, CurseForge file **8976027** (uploaded 2026-09-25, release). GitHub release: https://github.com/AhmiDarrow/Shamanic-Mounts/releases/tag/v0.1.1

## 0.1.2

Shamanic Mounts 0.1.2 - Into the March, CurseForge file **8976434** (uploaded 2026-09-25, release). GitHub release: https://github.com/AhmiDarrow/Shamanic-Mounts/releases/tag/v0.1.2

## 0.1.3

Shamanic Mounts 0.1.3 - Condor Wings, CurseForge file **8978477** (uploaded 2026-09-26, release). GitHub release: https://github.com/AhmiDarrow/Shamanic-Mounts/releases/tag/v0.1.3
