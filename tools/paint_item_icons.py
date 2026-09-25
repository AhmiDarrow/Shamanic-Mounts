"""Paint the item icons and the mods-list logo as deliberate pixel art.

Eight founder eggs share one silhouette and lighting (top-left) and each carries its line's emblem.
The saddle, herd book, and diamond apple are drawn as string rows against a palette, the way
classic sprite source is written. The 256x256 mods-list icon is a 32x32 emblem scaled with no
smoothing, so it keeps the same hand as the items.

    python tools/paint_item_icons.py
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / "src" / "main" / "resources" / "assets" / "shamanicmounts" / "textures" / "item"
LOGO = ROOT / "src" / "main" / "resources" / "icon.png"

CLEAR = (0, 0, 0, 0)


def hexa(value, alpha=255):
	return ((value >> 16) & 255, (value >> 8) & 255, value & 255, alpha)


def shade(color, delta):
	return tuple(max(0, min(255, c + delta)) for c in color[:3]) + (color[3],)


def sprite(rows, palette):
	"""Rows of palette keys. '.' is clear."""
	height = len(rows)
	width = max(len(row) for row in rows)
	image = Image.new("RGBA", (width, height), CLEAR)
	for y, row in enumerate(rows):
		for x, key in enumerate(row):
			if key != ".":
				image.putpixel((x, y), palette[key])
	return image


def blit(target, source, dx, dy):
	for y in range(source.height):
		for x in range(source.width):
			pixel = source.getpixel((x, y))
			if pixel[3]:
				tx, ty = x + dx, y + dy
				if 0 <= tx < target.width and 0 <= ty < target.height:
					target.putpixel((tx, ty), pixel)


# One silhouette for every egg. '#' outline, 'o' shell.
EGG = [
	"......####......",
	".....#oooo#.....",
	"....#oooooo#....",
	"...#oooooooo#...",
	"...#oooooooo#...",
	"..#oooooooooo#..",
	"..#oooooooooo#..",
	"..#oooooooooo#..",
	"..#oooooooooo#..",
	"..#oooooooooo#..",
	"..#oooooooooo#..",
	"...#oooooooo#...",
	"...#oooooooo#...",
	"....#oooooo#....",
	".....######.....",
	"................",
]

# Emblems sit in the lower two thirds of the shell. 'e' is the mark, 'l' its light, 'd' its dark.
EMBLEMS = {
	"eightfold": [
		".eeee.",
		"eeeeee",
		"ee..ee",
		"ed..de",
	],
	"drum_hart": [
		"e.....e",
		"e.e.e.e",
		".ee.ee.",
		"..eee..",
		"...e...",
		"...e...",
	],
	"elk": [
		"e.....e",
		"ee.e.ee",
		".eeeee.",
		"..eee..",
		"...e...",
		"...e...",
	],
	"crane": [
		".....ee",
		"...eeel",
		".eeeel.",
		"eeell..",
		"ll.....",
	],
	"nagual": [
		".ee..ee.",
		"eleeelee",
		".ee..ee.",
		"...ee...",
		"..elee..",
		"...ee...",
	],
	"barghest": [
		"..eeee..",
		".el..le.",
		"e.lddl.e",
		".el..le.",
		"..eeee..",
	],
	"roc": [
		"...e...",
		".l.e.l.",
		"..lel..",
		"eeeleee",
		"..lel..",
		".l.e.l.",
		"...e...",
	],
	"shade": [
		"..llll..",
		".le.dle.",
		"le..d.el",
		".le.dle.",
		"..llll..",
	],
	"bear": [
		".e.ee.e.",
		"e.e..e.e",
		"..elle..",
		".eeeeee.",
		".edeede.",
		"..eeee..",
	],
	"serpent": [
		"..eee...",
		".e...e..",
		"....e...",
		"...e....",
		"..e...e.",
		"...eee..",
	],
}

# (shell, shell dark, emblem, emblem light, emblem dark)
EGG_COLORS = {
	"eightfold": (0x9C6A3A, 0x5A3418, 0xEAD7B4, 0xFFFFFF, 0xC4A882),
	"drum_hart": (0xE6C98A, 0xA8834A, 0x5C3E1C, 0xF2E3B8, 0x40280F),
	"elk": (0x7A4A2C, 0x3F2314, 0xEADFC4, 0xFFFFFF, 0xB8A27E),
	"crane": (0xF4F1EA, 0xB9B4A6, 0x2A8F92, 0x74D0CE, 0x1A5C60),
	"nagual": (0xE39B2B, 0xA36616, 0x2C160C, 0xF6D28A, 0x1A0C06),
	"barghest": (0x6D6873, 0x36323B, 0x1A171C, 0xFFD24D, 0x120E14),
	"roc": (0x8C6DD1, 0x4C3496, 0xF3E3B0, 0xFFFFFF, 0xC9B074),
	"shade": (0x6F6390, 0x3A3252, 0xD9C8F2, 0x9C7FD0, 0x0C0A14),
	"bear": (0x4B3621, 0x2A1D12, 0xEDE6D6, 0xFFFFFF, 0xB8AE98),
	"serpent": (0x3F6B2E, 0x22401A, 0xD9C15A, 0xF4E6A0, 0x8C7A2A),
}


def egg(name):
	shell, dark, mark, light, deep = (hexa(value) for value in EGG_COLORS[name])
	outline = shade(dark, -40)
	image = Image.new("RGBA", (16, 16), CLEAR)
	for y, row in enumerate(EGG):
		for x, key in enumerate(row):
			if key == "#":
				image.putpixel((x, y), outline)
			elif key == "o":
				# Light from the top-left: a highlight crescent, a shadow band on the right and bottom.
				color = shell
				if x >= 11 or y >= 12 or (x >= 10 and y >= 10):
					color = dark
				if (x, y) in {(5, 2), (6, 2), (4, 3), (5, 3), (4, 4), (4, 5), (3, 6)}:
					color = shade(shell, 34)
				if (x, y) in {(5, 1), (6, 1), (7, 1)}:
					color = shade(shell, 18)
				image.putpixel((x, y), color)
	emblem = sprite(EMBLEMS[name], {"e": mark, "l": light, "d": deep})
	blit(image, emblem, (16 - emblem.width) // 2, 12 - emblem.height + (1 if emblem.height <= 5 else 0))
	return image


SADDLE = [
	"................",
	"...........AGGg.",
	"..........ABCCDB",
	"........AABDDECB",
	".......ABDDDDECB",
	"....GGgEDDDDDEDB",
	"..BBCCDBEDDDDBEB",
	".BCCCDDBEDDDDEBF",
	".ACCEDDAEDDDEBAF",
	".AAECDDAEEDEEAF.",
	".ABAEDEEAEEEAF..",
	"..BBAEBBAEBFA...",
	"....ABBBFF..A...",
	"....FFFF...AG...",
	"...........HI...",
	"................",
]

SADDLE_PALETTE = {
	# The vanilla saddle's leather, drawn a size up, with a gold rim on the pommel and the cantle.
	"A": hexa(0x51210C),
	"B": hexa(0x7F3919),
	"C": hexa(0xF18B5C),
	"D": hexa(0xDA662C),
	"E": hexa(0xA74E21),
	"F": hexa(0x35170A),
	"G": hexa(0xF2C94C),
	"g": hexa(0xB9902A),
	"H": hexa(0xB09988),
	"I": hexa(0x554843),
}

BAGS = [
	"................",
	".......KK.......",
	"......KSSK......",
	"..KKKKKSSKKKKK..",
	".KHLLLKSSKLLLHK.",
	".KLLLLKKKKLLLLK.",
	".KDDDDDKKDDDDDK.",
	".KLGLLLK.KLLLGLK",
	".KLgLLLK.KLLLgLK",
	".KLLLLLK.KLLLLLK",
	".KLLLLLK.KLLLLLK",
	".KDLLLDK.KDLLLDK",
	"..KDDDK...KDDDK.",
	"...KKK.....KKK..",
	"................",
	"................",
]

BAGS_PALETTE = {
	"K": hexa(0x2B1A10),
	"L": hexa(0xB08655),
	"H": hexa(0xD2A878),
	"D": hexa(0x5E3C20),
	"S": hexa(0x4A1D14),
	"G": hexa(0xF2C94C),
	"g": hexa(0xB9902A),
}

BOOK = [
	"................",
	"..KKKKKKKKKKK...",
	".KSCCCCCCCCCCK..",
	".KSCCCCCCCCCCKP.",
	".KSCA..C..ACCKP.",
	".KSCAA.C.AACCKP.",
	".KSC.AAAAA.CCKP.",
	".KSCC.AAA..CCKP.",
	".KSCC..A...CCKP.",
	".KSCC..A...CCKP.",
	".KSCCCCCCCCGGKP.",
	".KSCCCCCCCCGgKP.",
	".KSCCCCCCCCCCKP.",
	".KKKKKKKKKKKKKP.",
	"..KPPPPPPPPPPP..",
	"...KKKKKKKKKK...",
]

BOOK_PALETTE = {
	"K": hexa(0x1E1410),
	"S": hexa(0x3B2418),
	"C": hexa(0x6E3A2A),
	"A": hexa(0xEADFC4),
	"G": hexa(0xF2C94C),
	"g": hexa(0xB9902A),
	"P": hexa(0xF1E6C8),
}

APPLE = [
	"................",
	".......SS.......",
	"......SS.LL.....",
	".....S.LLLL.....",
	"...KKKKLLKKK....",
	"..KBBWBKBBBBK...",
	".KBWWBBBBBBbbK..",
	".KBWBBBWBBBbbK..",
	".KBBBBWBBBBbbK..",
	".KBBBBBBBWbbbK..",
	".KbBBBWBBBbbbK..",
	".KbbBBBBBbbbbK..",
	"..KbbbBbbbbbK...",
	"...KKbbbbbKK....",
	".....KKKK.......",
	"................",
]

APPLE_PALETTE = {
	"K": hexa(0x123A52),
	"B": hexa(0x4FC3F7),
	"b": hexa(0x2688BE),
	"W": hexa(0xD9F6FF),
	"S": hexa(0x4A6B25),
	"L": hexa(0xF08A2A),
}

# 32x32 emblem for the mods list: an eightfold head on a dark disc with a gold ring.
LOGO_ROWS = [
	"..........RRRRRRRRRRRR..........",
	".......RRRDDDDDDDDDDDDRRR.......",
	".....RRDDDDDDDDDDDDDDDDDDRR.....",
	"....RDDDDDDDDDDDDDDDDDDDDDDR....",
	"...RDDDDDDDDDDDDDBDDDBDDDDDDR...",
	"..RDDDDDDDDDDDDDBHBDBHBDDDDDDR..",
	"..RDDDDDDDDDDDDDBHHBHHBBDDDDDR..",
	".RDDDDDDDDDDDDDBHHHHHHHMBDDDDDR.",
	".RDDDDDDDDDDDDBHHHHHHHHMMBDDDDR.",
	"RDDDDDDDDDDDDBHHHHKHHHHMMMBDDDDR",
	"RDDDDDDDDDDDBHHHHKWKHHHMMMBDDDDR",
	"RDDDDDDDDDDBHHHHHKKHHHHMMMBDDDDR",
	"RDDDDDDDDDBHHHHHHHHHHHHMMMBDDDDR",
	"RDDDDDDDDBHHHHHHHHHHHHHMMMBDDDDR",
	"RDDDDDDDBHHHHHHHHHHHHHNMMMBDDDDR",
	"RDDDDDDBHHHHHHHHHHHHHNNMMMBDDDDR",
	"RDDDDDDBHHHNHHHHHHHHNNNMMMBDDDDR",
	"RDDDDDDBHHNHHHHHHHHHNNNMMMBDDDDR",
	"RDDDDDDBHNHHHHHHHHHHNNNMMMBDDDDR",
	"RDDDDDDDBBHHHHHHHHHBNNNMMMBDDDDR",
	"RDDDDDDDDDBBBBBHHHHBNNNMMMBDDDDR",
	"RDDDDDDDDDDDDDDBHHHBNNNMMMBDDDDR",
	".RDDDDDDDDDDDDDBHHHBNNNMMBDDDDR.",
	".RDDDDDDDDDDDDDBHHHHBBBBBDDDDDR.",
	"..RDDDDDDDDDDDDDBHHHHBDDDDDDDR..",
	"..RDDDDDDDDDDDDDBHHHHBDDDDDDDR..",
	"...RDDDDDDDDDDDDDBBBBDDDDDDDR...",
	"....RDDDDDDDDDDDDDDDDDDDDDDR....",
	".....RRDDDDDDDDDDDDDDDDDDRR.....",
	".......RRRDDDDDDDDDDDDRRR.......",
	"..........RRRRRRRRRRRR..........",
	"................................",
]

LOGO_PALETTE = {
	"R": hexa(0xF2C94C),
	"D": hexa(0x1C1220),
	"B": hexa(0x2E1A0E),
	"H": hexa(0xA8703C),
	"M": hexa(0x3A2414),
	"N": hexa(0x5A3418),
	"K": hexa(0x1A0E08),
	"W": hexa(0xFFFFFF),
}


def logo():
	small = sprite(LOGO_ROWS, LOGO_PALETTE)
	# A soft inner ring so the gold rim reads as a bevel.
	for y in range(32):
		for x in range(32):
			pixel = small.getpixel((x, y))
			if pixel == LOGO_PALETTE["D"]:
				dx = x - 15.5
				dy = y - 15.5
				if dx * dx + dy * dy > 13.2 * 13.2:
					small.putpixel((x, y), hexa(0x2A1C30))
	return small.resize((256, 256), Image.NEAREST)


def main():
	ITEMS.mkdir(parents=True, exist_ok=True)
	for name in EGG_COLORS:
		egg(name).save(ITEMS / f"egg_{name}.png")
	sprite(SADDLE, SADDLE_PALETTE).save(ITEMS / "shamanic_saddle.png")
	sprite(BAGS, BAGS_PALETTE).save(ITEMS / "saddle_bags.png")
	sprite(BOOK, BOOK_PALETTE).save(ITEMS / "herd_book.png")
	sprite(APPLE, APPLE_PALETTE).save(ITEMS / "diamond_apple.png")
	logo().save(LOGO)
	print(ITEMS)
	print(LOGO)


if __name__ == "__main__":
	main()
