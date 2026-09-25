"""Compose docs/public/shamanic-mounts-founders.png from the harness's three-quarter pictures.

Run the store section first (python tools/system_harness.py --only store); it photographs every line
with the HUD hidden. The newest picture of each line is cropped around the mount and tiled four
across with its name, on the same midnight ground as the store art.

    python tools/founders_sheet.py
"""
import glob
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SHOTS = ROOT / "build" / "harness-client" / "screenshots"
OUT = ROOT / "docs" / "public" / "shamanic-mounts-founders.png"
LINES = [
    ("eightfold", "Eightfold"), ("drum", "Drum hart"), ("elk", "Elk"),
    ("crane", "Crane"), ("nagual", "Nagual"), ("barghest", "Barghest"),
    ("roc", "Roc"), ("shade", "Shade"), ("chimera", "Chimera"), ("bear", "Bear"), ("serpent", "Serpent"),
]
TILE = (480, 300)
LABEL = 44
GAP = 12
NIGHT = (14, 18, 30)


def latest(label):
    files = glob.glob(str(SHOTS / f"showcase-*-{label}.png"))
    if not files:
        raise SystemExit(f"no picture for {label}; run the looks section first")
    return max(files, key=os.path.getmtime)


def font(size):
    for name in ("segoeui.ttf", "arial.ttf"):
        path = Path(os.environ.get("WINDIR", r"C:\Windows")) / "Fonts" / name
        if path.is_file():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def tile(label):
    image = Image.open(latest(f"look_{label}_sheet")).convert("RGB")
    # The camera aims at the mount's middle, so it stands at the centre of the frame.
    # A wider crop than the tile, scaled down, so the tallest heads and tails stay in frame.
    # The camera looks down a little, so the animal sits just above the middle of the frame.
    cx, cy = 640, 350
    w, h = TILE[0] * 11 // 10, TILE[1] * 11 // 10
    return image.crop((cx - w // 2, cy - h // 2, cx + w // 2, cy + h // 2)).resize(TILE, Image.Resampling.LANCZOS)


def main():
    cols = 4 if len(LINES) > 9 else 3
    rows = (len(LINES) + cols - 1) // cols
    width = cols * TILE[0] + (cols + 1) * GAP
    height = rows * (TILE[1] + LABEL) + (rows + 1) * GAP
    sheet = Image.new("RGB", (width, height), NIGHT)
    draw = ImageDraw.Draw(sheet)
    face = font(26)
    for index, (key, name) in enumerate(LINES):
        col = index % cols
        row = index // cols
        x = GAP + col * (TILE[0] + GAP)
        y = GAP + row * (TILE[1] + LABEL + GAP)
        sheet.paste(tile(key), (x, y))
        text_w = draw.textlength(name, font=face)
        draw.text((x + (TILE[0] - text_w) / 2, y + TILE[1] + 8), name, fill=(236, 232, 220), font=face)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    print(OUT, sheet.size)


if __name__ == "__main__":
    main()
