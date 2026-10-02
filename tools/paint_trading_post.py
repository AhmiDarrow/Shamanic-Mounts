"""Paint the Mount Trading Post textures: navy post, antique gold, teal cloth.

    python tools/paint_trading_post.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "shamanicmounts" / "textures" / "block"

NAVY = (14, 27, 58, 255)
NAVY_DARK = (8, 16, 36, 255)
NAVY_LIGHT = (36, 58, 96, 255)
GOLD = (198, 161, 90, 255)
GOLD_LIGHT = (230, 201, 138, 255)
GOLD_DARK = (140, 106, 47, 255)
TEAL = (31, 138, 134, 255)
TEAL_DARK = (16, 86, 90, 255)
TEAL_LIGHT = (92, 196, 186, 255)
CLEAR = (0, 0, 0, 0)


def put(image, x, y, color):
	image.putpixel((x, y), color)


def post():
	image = Image.new("RGBA", (16, 16), NAVY)
	for y in range(16):
		for x in range(16):
			if x == 0 or y == 15:
				put(image, x, y, NAVY_DARK)
			elif x == 15 or y == 0:
				put(image, x, y, NAVY_LIGHT)
			elif x % 4 == 0 or y % 5 == 0:
				put(image, x, y, NAVY_DARK)
			elif (x + y) % 7 == 0:
				put(image, x, y, NAVY_LIGHT)
	return image


def gold():
	image = Image.new("RGBA", (16, 16), GOLD)
	for y in range(16):
		for x in range(16):
			if x < 2 or y < 2:
				put(image, x, y, GOLD_LIGHT)
			elif x > 13 or y > 13:
				put(image, x, y, GOLD_DARK)
			elif (x + y) % 5 == 0:
				put(image, x, y, GOLD_LIGHT)
	return image


def cloth():
	image = Image.new("RGBA", (16, 16), CLEAR)
	for y in range(1, 15):
		for x in range(2, 14):
			if y >= 13 and (x % 2 == 0):
				continue
			if y >= 12 and (x == 2 or x == 13):
				continue
			color = TEAL_LIGHT if x % 3 == 0 else TEAL_DARK if y % 4 == 0 else TEAL
			put(image, x, y, color)
	return image


def main():
	OUT.mkdir(parents=True, exist_ok=True)
	post().save(OUT / "trading_post.png")
	gold().save(OUT / "trading_post_gold.png")
	cloth().save(OUT / "trading_post_cloth.png")


if __name__ == "__main__":
	main()
