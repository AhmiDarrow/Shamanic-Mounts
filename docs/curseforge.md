# CurseForge publishing

The CurseForge project has not been created. Do not infer an id from search results. When the project exists, record the id and the slug here before any upload.

Do not upload until asked. The store page describes the mod as it will ship. The jar spawns the mount, the ten founder eggs, the brace tame, breeding, and the ridden abilities. There is still no CurseForge project id.

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
