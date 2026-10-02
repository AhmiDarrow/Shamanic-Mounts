"""Paint the Mount Flute item icon: a short wood flute, gold bands, teal tassel.

    python tools/paint_flute.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "shamanicmounts" / "textures" / "item" / "mount_flute.png"

WOOD = (196, 165, 116, 255)
SHADE = (107, 74, 42, 255)
GOLD = (224, 176, 64, 255)
TEAL = (42, 143, 146, 255)
STITCH = (26, 92, 96, 255)
CLEAR = (0, 0, 0, 0)


def put(image, x, y, color):
	if 0 <= x < 16 and 0 <= y < 16:
		image.putpixel((x, y), color)


def paint():
	image = Image.new("RGBA", (16, 16), CLEAR)
	# Short diagonal tube. Near end sits low-left; the far end rises to the right.
	for i in range(10):
		x = 3 + i
		y = 12 - i
		band = i in (2, 6)
		# The shade sits beside the wood, edge to edge: one step down-right left a clear seam
		# through the tube that showed as a see-through stripe when held.
		put(image, x, y, GOLD if band else WOOD)
		put(image, x + 1, y, GOLD if band else SHADE)
		if band:
			put(image, x + 1, y + 1, GOLD)
	# Gold end cap.
	put(image, 13, 2, GOLD)
	put(image, 14, 2, GOLD)
	put(image, 14, 3, GOLD)
	put(image, 13, 3, WOOD)
	# Teal tassel off the near end, with one darker stitch.
	put(image, 2, 13, TEAL)
	put(image, 3, 13, TEAL)
	put(image, 2, 14, TEAL)
	put(image, 3, 14, STITCH)
	put(image, 4, 14, TEAL)
	put(image, 3, 15, TEAL)
	OUT.parent.mkdir(parents=True, exist_ok=True)
	image.save(OUT)
	return image


if __name__ == "__main__":
	paint()
	print(OUT)
