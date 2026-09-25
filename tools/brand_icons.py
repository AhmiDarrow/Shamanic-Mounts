"""Resample the Shamanic Mounts cover into every size the store and the mods list use.

The founder sheet is built from live harness pictures by tools/founders_sheet.py, not here.

    python tools/brand_icons.py [path-to-approved-cover.jpg]

With no argument, resamples art/branding/shamanic-mounts-logo.png.
A jpg argument is saved over that master first (square, PNG).
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "art" / "branding"
PUBLIC = ROOT / "docs" / "public"
MASTER = BRAND / "shamanic-mounts-logo.png"


def square(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    side = min(im.size)
    left = (im.width - side) // 2
    top = (im.height - side) // 2
    return im.crop((left, top, left + side, top + side))


def save_size(im: Image.Image, path: Path, side: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    out = im.resize((side, side), Image.Resampling.LANCZOS)
    out.save(path, "PNG")
    print(f"{path.relative_to(ROOT)} {out.size[0]}x{out.size[1]}")


def main() -> None:
    BRAND.mkdir(parents=True, exist_ok=True)
    PUBLIC.mkdir(parents=True, exist_ok=True)
    if len(sys.argv) > 1:
        src = square(Image.open(sys.argv[1]))
        src.save(MASTER, "PNG")
        print(f"master {src.size[0]}x{src.size[1]}")
    master = square(Image.open(MASTER))
    master.save(MASTER, "PNG")
    master.save(PUBLIC / "shamanic-mounts-logo.png", "PNG")
    save_size(master, PUBLIC / "shamanic-mounts-icon-400.png", 400)
    save_size(master, PUBLIC / "project-icon-512.png", 512)
    save_size(master, ROOT / "src" / "main" / "resources" / "icon.png", 256)
    save_size(master, BRAND / "curseforge-card-preview.png", 160)


if __name__ == "__main__":
    main()
