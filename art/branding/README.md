# Shamanic Mounts identity

The master is `shamanic-mounts-logo.png`, 1024 by 1024. The CurseForge upload is `../../docs/public/shamanic-mounts-icon-400.png`, exactly 400 by 400 pixels, resampled from the master with LANCZOS interpolation.

Same cover language as Tribal Power and Chocobos Reborn: midnight navy, teal and violet aurora, floating islands, an antique-gold broken ring, two-line carved title reading SHAMANIC over MOUNTS.

Approved background 2026-09-24. Updated 2026-09-25: the first-draft cube steed inside the ring was replaced by the shipped eightfold, photographed in game and turned toward the camera, with a crane gliding high in the left sky and a small shade on the ground at lower right. All three are real in-game renders keyed off a lime stage; the crane is held in its glide by the harness `wing` command and tilted forward in the compositor. `shamanic-mounts-logo-v1.png` is the untouched original that the compositor always starts from.

To rebuild it:

```
python tools/system_harness.py --only store   # photographs the eightfold, crane, and shade on a lime stage
python tools/store_cover.py --preview         # check art/branding/cover-preview.png
python tools/store_cover.py                   # writes the master
python tools/brand_icons.py                   # resamples every size
```

`src/main/resources/icon.png` is the 256 by 256 mods-list logo (`logoFile` in `neoforge.mods.toml`). `docs/public/project-icon-512.png` is the 512 project mark. `curseforge-card-preview.png` previews the mark at 160 pixels.

`docs/public/shamanic-mounts-founders.png` is the ten-line sheet with the chimera for the store page. `tools/founders_sheet.py` composes it from the store section's pictures, so it always shows the rig that ships. The retired Blender studies of the first cubes (`art/previews/`, `art/*.blend`, and their `tools/build_*` scripts) stay local and are not in the repository.
