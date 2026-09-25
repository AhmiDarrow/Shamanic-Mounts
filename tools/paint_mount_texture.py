"""Paint the mount atlas: 512x512, sixteen columns of 32x32 material cells, matching client/Mat.java.

Every cell tiles, because the renderer reads it one texel per model pixel at the cube's own position,
so the coat continues from one cube to the next. Eye cells hold pixel eyes in three sizes and a closed
lid: 2x2 at (0,0)-(8,8), 3x2 at (8,0)-(20,8), 4x3 at (0,8)-(16,20), shut at (16,8)-(24,16).

    python tools/paint_mount_texture.py
"""

import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src" / "main" / "resources" / "assets" / "shamanicmounts" / "textures" / "entity" / "mount.png"
CELL = 32

# Must match client/Mat.java.
CELLS = {
	"MANE": (0, 0), "HOOF": (1, 0), "ANTLER": (2, 0), "BEAK": (3, 0), "TALON": (4, 0), "SADDLE": (5, 0), "GOLD": (6, 0), "NOSE": (7, 0),
	"STEED": (0, 1), "STEED_DARK": (1, 1), "STEED_PALE": (2, 1), "HART": (3, 1), "HART_DARK": (4, 1), "HART_PALE": (5, 1), "FLAG": (6, 1), "ELK_PALE": (7, 1),
	"ELK": (0, 2), "ELK_DARK": (1, 2), "CRANE": (2, 2), "CRANE_DARK": (3, 2), "CRANE_WING": (4, 2), "CRANE_WING_DARK": (5, 2), "CROWN": (6, 2), "PRIMARY": (7, 2),
	"ROC": (0, 3), "ROC_DARK": (1, 3), "ASTRAL": (2, 3), "ASTRAL_DARK": (3, 3), "JAG": (4, 3), "JAG_ROSETTE": (5, 3), "JAG_DARK": (6, 3), "JAG_PALE": (7, 3),
	"HOUND": (0, 4), "HOUND_DARK": (1, 4), "HOUND_PALE": (2, 4), "DUSK": (3, 4), "DUSK_DARK": (4, 4), "DUSK_PALE": (5, 4), "DUSK_ROSETTE": (6, 4), "STAR": (7, 4),
	"FUR": (0, 5), "FUR_DARK": (1, 5), "SCALE": (2, 5), "SCALE_DARK": (3, 5), "PEARL": (4, 5), "WISH": (5, 5), "HORN": (6, 5), "MEMBRANE": (7, 5),
	"BAG": (0, 7), "BAG_DARK": (1, 7), "SADDLE_DARK": (2, 7), "ARMOR": (3, 7), "ARMOR_TRIM": (4, 7), "ARMOR_LEATHER": (5, 7), "ARMOR_GOLD": (6, 7), "ARMOR_DIAMOND": (7, 7),
	"STEED_BLACK": (0, 8), "STEED_BLACK_DARK": (1, 8), "STEED_BLACK_PALE": (2, 8), "MANE_BLACK": (3, 8), "STEED_PALOMINO": (4, 8), "STEED_PALOMINO_DARK": (5, 8), "STEED_PALOMINO_PALE": (6, 8), "MANE_FLAXEN": (7, 8), "HART_RED": (8, 8), "HART_RED_DARK": (9, 8), "HART_RED_PALE": (10, 8), "HART_WHITE": (11, 8), "HART_WHITE_DARK": (12, 8), "HART_WHITE_PALE": (13, 8), "ELK_BROWN": (14, 8), "ELK_BROWN_DARK": (15, 8), "ELK_BROWN_PALE": (0, 9), "ELK_GREY": (1, 9), "ELK_GREY_DARK": (2, 9), "ELK_GREY_PALE": (3, 9), "CRANE_GREY": (4, 9), "CRANE_GREY_DARK": (5, 9), "CRANE_BLACK": (6, 9), "CRANE_BLACK_DARK": (7, 9), "ROC_STORM": (8, 9), "ROC_STORM_DARK": (9, 9), "ROC_RED": (10, 9), "ROC_RED_DARK": (11, 9), "JAG_BLACK": (12, 9), "JAG_BLACK_ROSETTE": (13, 9), "JAG_BLACK_DARK": (14, 9), "JAG_BLACK_PALE": (15, 9), "JAG_SNOW": (0, 10), "JAG_SNOW_ROSETTE": (1, 10), "JAG_SNOW_DARK": (2, 10), "JAG_SNOW_PALE": (3, 10), "HOUND_GREY": (4, 10), "HOUND_GREY_DARK": (5, 10), "HOUND_GREY_PALE": (6, 10), "HOUND_RED": (7, 10), "HOUND_RED_DARK": (8, 10), "HOUND_RED_PALE": (9, 10), "DUSK_SILVER": (10, 10), "DUSK_SILVER_DARK": (11, 10), "DUSK_SILVER_PALE": (12, 10), "DUSK_SILVER_ROSETTE": (13, 10), "DUSK_SILVER_STAR": (14, 10), "DUSK_INK": (15, 10), "DUSK_INK_DARK": (0, 11), "DUSK_INK_PALE": (1, 11), "DUSK_INK_ROSETTE": (2, 11), "DUSK_INK_STAR": (3, 11), "FUR_JADE": (4, 11), "FUR_JADE_DARK": (5, 11), "SCALE_JADE": (6, 11), "SCALE_JADE_DARK": (7, 11), "FUR_NIGHT": (8, 11), "FUR_NIGHT_DARK": (9, 11), "SCALE_NIGHT": (10, 11), "SCALE_NIGHT_DARK": (11, 11),
	"EYE_CAT_GREEN": (12, 11), "BEAR_BLACK": (13, 11), "BEAR_BLACK_DARK": (14, 11), "BEAR_BLACK_PALE": (15, 11), "BEAR_GRIZZLY": (0, 12), "BEAR_GRIZZLY_DARK": (1, 12), "BEAR_GRIZZLY_PALE": (2, 12), "BEAR_POLAR": (3, 12), "BEAR_POLAR_DARK": (4, 12), "BEAR_POLAR_PALE": (5, 12), "SERPENT": (6, 12), "SERPENT_DARK": (7, 12), "SERPENT_PALE": (8, 12), "SERPENT_JUNGLE": (9, 12), "SERPENT_JUNGLE_DARK": (10, 12), "SERPENT_JUNGLE_PALE": (11, 12), "SERPENT_BONE": (12, 12), "SERPENT_BONE_DARK": (13, 12), "SERPENT_BONE_PALE": (14, 12), "FRILL": (15, 12),
	"EYE_HORSE": (0, 6), "EYE_DEER": (1, 6), "EYE_CAT": (2, 6), "EYE_SHADE": (3, 6), "EYE_HOUND": (4, 6), "EYE_BIRD": (5, 6), "EYE_ROC": (6, 6), "EYE_DRAGON": (7, 6),
}


def hexc(text):
	text = text.lstrip("#")
	return tuple(int(text[i:i + 2], 16) for i in (0, 2, 4))


def clamp(value):
	return max(0, min(255, int(round(value))))


def shade(color, delta):
	return tuple(clamp(c + delta) for c in color)


def mix(a, b, t):
	return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def hash2(x, y, seed):
	n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
	n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
	return (n & 255) / 255.0


def blank(color):
	return [[color for _ in range(CELL)] for _ in range(CELL)]


def put(tile, x, y, color):
	tile[y % CELL][x % CELL] = color


def noise(tile, base, seed, amount):
	"""Two octaves of soft mottling, tileable by construction."""
	for y in range(CELL):
		for x in range(CELL):
			n = (hash2(x, y, seed) - 0.5) * amount
			n += (hash2(x // 3, y // 3, seed + 5) - 0.5) * amount * 1.4
			n += (hash2(x // 8, y // 8, seed + 9) - 0.5) * amount * 0.8
			tile[y][x] = shade(tile[y][x], n)


def fur(base, dark, pale, seed, length=(4, 8), density=34, wave=0.0):
	"""Hair falls downward. Strands a step darker or lighter than the coat, clumped by a broad mottle."""
	tile = blank(base)
	noise(tile, base, seed, 14)
	rng = random.Random(seed)
	strand = mix(base, dark, 0.5)
	glint = mix(base, pale, 0.55)
	for _ in range(density):
		x = rng.randrange(CELL)
		y0 = rng.randrange(CELL)
		span = rng.randint(*length)
		roll = rng.random()
		color = shade(strand, rng.randint(-6, 8)) if roll < 0.74 else shade(glint, rng.randint(-6, 6))
		if roll > 0.96:
			color = mix(base, dark, 0.8)
		for step in range(span):
			xx = x + int(round(math.sin((y0 + step) * 0.5 + x) * wave))
			put(tile, xx, y0 + step, color)
	return tile


def short_fur(base, dark, pale, seed):
	return fur(base, dark, pale, seed, length=(2, 4), density=56)


def long_hair(base, dark, pale, seed):
	"""A mane: long waving locks with light catching the crest of each wave."""
	tile = blank(base)
	noise(tile, base, seed, 14)
	rng = random.Random(seed)
	for lock in range(11):
		x = lock * 3 + rng.randint(0, 1)
		phase = rng.random() * 6.28
		color = shade(mix(base, dark, 0.7), rng.randint(-8, 8))
		light = shade(mix(base, pale, 0.6), rng.randint(-8, 6))
		for y in range(CELL):
			xx = x + int(round(math.sin(y * 0.28 + phase) * 1.4))
			put(tile, xx, y, color)
			if y % 9 == lock % 9:
				put(tile, xx + 1, y, light)
	return tile


def feathers(base, dark, pale, row_h=6, width=8, seed=1):
	"""Overlapping body feathers, each tip a rounded scallop, rows offset by half a feather."""
	tile = blank(base)
	noise(tile, base, seed, 10)
	for y in range(CELL):
		row = y // row_h
		local = y % row_h
		offset = (width // 2) if row % 2 else 0
		for x in range(CELL):
			cx = (x + offset) % width
			edge = abs(cx - width / 2 + 0.5)
			tip_line = row_h - 1 - int(edge * 0.7)
			if local == tip_line:
				tile[y][x] = dark
			elif local == tip_line - 1 and edge < 2.5:
				tile[y][x] = shade(base, -10)
			elif local == 0 and edge < 1.0:
				tile[y][x] = pale
			elif cx == width // 2 - 1 and local < tip_line:
				tile[y][x] = shade(base, -14)
	return tile


def flight(base, dark, pale, seed=2):
	"""Long flight feathers: a dark shaft down each column and barbs leaning off it."""
	tile = blank(base)
	noise(tile, base, seed, 12)
	width = 8
	for y in range(CELL):
		for x in range(CELL):
			cx = x % width
			if cx == 3:
				tile[y][x] = dark
			elif cx == 4 and y % 2 == 0:
				tile[y][x] = shade(dark, 30)
			elif (x + y) % 3 == 0 and cx != 3:
				tile[y][x] = shade(base, -12 if cx < 3 else -6)
			elif cx == 0 or cx == 7:
				tile[y][x] = shade(base, -18)
			if y % 16 == 15 and cx in (1, 2, 5, 6):
				tile[y][x] = dark
			if y % 16 == 0 and cx in (2, 5):
				tile[y][x] = pale
	return tile


def scales(base, dark, pale, size=6, seed=3):
	"""Rounded scales in staggered rows, each with a bright rim on top and a dark seam below."""
	tile = blank(base)
	noise(tile, base, seed, 8)
	rows = CELL // size + 1
	for row in range(-1, rows + 1):
		offset = (size // 2) if row % 2 else 0
		cy = row * size
		for col in range(-1, CELL // size + 2):
			cx = col * size + offset
			for y in range(cy, cy + size):
				for x in range(cx, cx + size):
					dx = (x - cx) - (size - 1) / 2
					dy = (y - cy) - (size - 1) / 2
					r = math.hypot(dx / (size * 0.5), dy / (size * 0.62))
					if r > 1.0:
						continue
					color = base
					if r > 0.78:
						color = dark if dy > 0 else pale
					elif r > 0.55 and dy > 0.2 * size:
						color = shade(base, -10)
					elif dy < -0.15 * size and abs(dx) < size * 0.2:
						color = shade(pale, -14)
					put(tile, x, y, color)
	return tile


def plates(base, dark, pale, seed=4):
	"""Smooth armour with a soft sheen and a seam every eight pixels."""
	tile = blank(base)
	noise(tile, base, seed, 6)
	for y in range(CELL):
		for x in range(CELL):
			if y % 8 == 7:
				tile[y][x] = dark
			elif y % 8 == 0:
				tile[y][x] = pale
			elif (x + y * 2) % 16 == 0:
				tile[y][x] = shade(base, 12)
	return tile


def horn(base, dark, seed=5):
	tile = blank(base)
	noise(tile, base, seed, 10)
	for y in range(CELL):
		for x in range(CELL):
			if x % 5 == 0:
				tile[y][x] = dark
			elif x % 5 == 1 and y % 4 != 0:
				tile[y][x] = shade(base, 14)
			elif y % 11 == 0:
				tile[y][x] = shade(base, -10)
	return tile


def bone(base, dark, seed=6):
	"""Antler: velvet speckle over bone with faint length lines."""
	tile = blank(base)
	noise(tile, base, seed, 14)
	rng = random.Random(seed)
	for _ in range(70):
		put(tile, rng.randrange(CELL), rng.randrange(CELL), shade(dark, rng.randint(-8, 12)))
	for y in range(CELL):
		for x in range(CELL):
			if (x * 3 + y) % 13 == 0:
				tile[y][x] = shade(tile[y][x], -8)
	return tile


def keratin(base, dark, pale, seed=7):
	"""Beak and claw: hard, glossy, with growth bands."""
	tile = blank(base)
	noise(tile, base, seed, 6)
	for y in range(CELL):
		for x in range(CELL):
			if (y + x // 6) % 7 == 0:
				tile[y][x] = shade(base, -12)
			if (x - y) % 16 == 3:
				tile[y][x] = pale
			if (x - y) % 16 == 4:
				tile[y][x] = shade(pale, -18)
	return tile


def leather(base, seed=8):
	tile = fur(base, shade(base, -34), shade(base, 22), seed, length=(2, 3), density=40)
	for y in range(CELL):
		for x in range(CELL):
			if x % 16 in (3, 12) and y % 3 != 1:
				tile[y][x] = shade(base, -36)
			if x % 16 in (3, 12) and y % 3 == 1:
				tile[y][x] = shade(base, 30)
	return tile


def metal(base, dark, pale, seed=9):
	tile = blank(base)
	noise(tile, base, seed, 6)
	for y in range(CELL):
		for x in range(CELL):
			d = (x + y) % 16
			if d in (2, 3):
				tile[y][x] = pale
			elif d in (9, 10):
				tile[y][x] = dark
			elif d == 4:
				tile[y][x] = shade(pale, -20)
	return tile


def skin(base, dark, seed=10):
	"""Nose leather: dark, dimpled, damp."""
	tile = blank(base)
	noise(tile, base, seed, 18)
	for y in range(CELL):
		for x in range(CELL):
			if (x + 2 * y) % 5 == 0:
				tile[y][x] = shade(dark, -8)
			elif (x * 3 + y) % 7 == 1:
				tile[y][x] = shade(base, 12)
	return tile


def rosettes(tile, dark, core, seed=11):
	"""Jaguar rings: broken rings of dark around a warmer core, laid out to tile."""
	rng = random.Random(seed)
	centres = [(5, 5), (21, 3), (13, 15), (28, 18), (4, 24), (18, 27)]
	for cx, cy in centres:
		rx = rng.randint(3, 4)
		ry = rng.randint(2, 4)
		for angle in range(0, 360, 12):
			if rng.random() < 0.18:
				continue
			x = cx + int(round(math.cos(math.radians(angle)) * rx))
			y = cy + int(round(math.sin(math.radians(angle)) * ry))
			put(tile, x, y, shade(dark, rng.randint(-10, 10)))
		put(tile, cx, cy, core)
		if rng.random() < 0.7:
			put(tile, cx + 1, cy, shade(core, -10))
	for _ in range(14):
		put(tile, rng.randrange(CELL), rng.randrange(CELL), dark)
	return tile


def stars(tile, pale, bright, seed=12):
	rng = random.Random(seed)
	for _ in range(26):
		put(tile, rng.randrange(CELL), rng.randrange(CELL), pale)
	for _ in range(4):
		x = rng.randrange(CELL)
		y = rng.randrange(CELL)
		for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
			put(tile, x + dx, y + dy, bright if (dx, dy) == (0, 0) else pale)
	return tile


EYE_2 = (
	"LLLLLLLL",
	"LLSSSSLL",
	"LSIIIISL",
	"SIIIIIIS",
	"SIIIIIIS",
	"LSIIIISL",
	"LLSSSSLL",
	"KKKKKKKK",
)
EYE_3 = (
	"LLLLLLLLLLLL",
	"LLLSSSSSSLLL",
	"LSSIIIIIISSL",
	"SIIIIIIIIIIS",
	"SIIIIIIIIIIS",
	"LSSIIIIIISSL",
	"LLLSSSSSSLLL",
	"KKKKKKKKKKKK",
)
EYE_4 = (
	"LLLLLLLLLLLLLLLL",
	"LLLLLSSSSSSLLLLL",
	"LLLSSIIIIIISSLLL",
	"LSSIIIIIIIIIISSL",
	"SIIIIIIIIIIIIIIS",
	"SIIIIIIIIIIIIIIS",
	"SIIIIIIIIIIIIIIS",
	"LSSIIIIIIIIIISSL",
	"LLLSSIIIIIISSLLL",
	"LLLLLSSSSSSLLLLL",
	"KKKKKKKKKKKKKKKK",
	"KKKKKKKKKKKKKKKK",
)
PUPILS = {
	"bar": {2: {(2, 3), (3, 3), (4, 3), (5, 3), (2, 4), (3, 4), (4, 4), (5, 4)},
			3: {(x, y) for x in range(3, 9) for y in (3, 4)},
			4: {(x, y) for x in range(4, 12) for y in (4, 5, 6)}},
	"slit": {2: {(3, 2), (4, 2), (3, 3), (4, 3), (3, 4), (4, 4), (3, 5), (4, 5)},
			3: {(x, y) for x in (5, 6) for y in range(2, 6)},
			4: {(x, y) for x in (7, 8) for y in range(2, 9)}},
	"round": {2: {(3, 3), (4, 3), (3, 4), (4, 4)},
			3: {(x, y) for x in (5, 6) for y in (3, 4)},
			4: {(x, y) for x in range(6, 10) for y in range(4, 7)}},
}
CATCH = {2: (2, 2), 3: (3, 2), 4: (5, 3)}


def eye(lid, sclera, iris, pupil, kind):
	"""One eye cell: three sizes of open eye and a shut lid, on the lid colour."""
	tile = blank(lid)
	noise(tile, lid, 13, 8)
	iris_dark = shade(iris, -42)
	iris_light = shade(iris, 34)
	for size, rows, ox, oy in ((2, EYE_2, 0, 0), (3, EYE_3, 8, 0), (4, EYE_4, 0, 8)):
		high = len(rows)
		for y, row in enumerate(rows):
			for x, key in enumerate(row):
				if key == "L":
					color = lid
				elif key == "K":
					color = shade(lid, 36 if kind != "bar" else 10)
				elif key == "S":
					color = sclera if 1 < y < high - 3 else shade(sclera, -26)
				else:
					t = y / max(1, high - 3)
					color = iris_dark if t < 0.34 else iris_light if t > 0.72 else iris
				if key == "I" and (x, y) in PUPILS[kind][size]:
					color = pupil
				if (x, y) == CATCH[size]:
					color = mix(color, (255, 252, 244), 0.8)
				tile[oy + y][ox + x] = color
	# Shut lid: a soft closed line with lashes, stretched to any size.
	for y in range(8):
		for x in range(8):
			color = lid
			if y == 3 and 0 < x < 7:
				color = shade(lid, -34)
			elif y == 4 and 1 < x < 6:
				color = shade(lid, 30)
			elif y == 2 and x in (1, 6):
				color = shade(lid, -20)
			tile[8 + y][16 + x] = color
	return tile


def paste(image, tile, name):
	col, row = CELLS[name]
	for y in range(CELL):
		for x in range(CELL):
			image.putpixel((col * CELL + x, row * CELL + y), tile[y][x])


def main():
	image = Image.new("RGB", (512, 512), (18, 12, 10))
	steed, steed_d, steed_p = hexc("8A5A32"), hexc("4A2E18"), hexc("B58657")
	hart, hart_d, hart_p = hexc("C08E5A"), hexc("7A5230"), hexc("EBD6B0")
	elk, elk_d, elk_p = hexc("5C3A22"), hexc("2E1C10"), hexc("C9B48C")
	crane, crane_d = hexc("F1EEE6"), hexc("8E8C86")
	wing, wing_d = hexc("2E8C8E"), hexc("16585C")
	roc, roc_d = hexc("B99A6A"), hexc("6E5334")
	astral, astral_d = hexc("5A47B8"), hexc("2E2270")
	jag, jag_d, jag_p = hexc("DDA032"), hexc("26140A"), hexc("F0DEB0")
	hound, hound_d, hound_p = hexc("3A3740"), hexc("17151A"), hexc("8A868E")
	dusk, dusk_d, dusk_p = hexc("7C709A"), hexc("3B3252"), hexc("BDB2D0")
	fur_c, fur_d = hexc("EAC782"), hexc("A0703A")
	scale, scale_d = hexc("7BCFC2"), hexc("3A8A94")
	pearl, wish = hexc("F4EFD8"), hexc("F5D46A")
	mane = hexc("1C1410")

	cells = {
		"MANE": long_hair(mane, shade(mane, -12), hexc("4A3A30"), 21),
		"HOOF": horn(hexc("2E211A"), hexc("15100C")),
		"ANTLER": bone(hexc("E6D3A8"), hexc("A88E62")),
		"BEAK": keratin(hexc("D9942A"), hexc("8C5A12"), hexc("F2C878")),
		"TALON": keratin(hexc("3A2A20"), hexc("1A1210"), hexc("6A5548")),
		"SADDLE": leather(hexc("7E2F1F")),
		"SADDLE_DARK": leather(hexc("4A1D14")),
		"ARMOR": plates(hexc("9AA2AC"), hexc("5C636C"), hexc("D8DEE6")),
		"ARMOR_TRIM": metal(hexc("D8DEE6"), hexc("8A929C"), hexc("FFFFFF")),
		"ARMOR_LEATHER": leather(hexc("A0522D")),
		"ARMOR_GOLD": plates(hexc("E8C04A"), hexc("A8801E"), hexc("FFF0B0")),
		"ARMOR_DIAMOND": plates(hexc("5FD6D4"), hexc("2A8A96"), hexc("C8FFFF")),
		"BAG": leather(hexc("B08655")),
		"BAG_DARK": leather(hexc("5E3C20")),
		"GOLD": metal(hexc("F2C84B"), hexc("A8801E"), hexc("FFF0B0")),
		"NOSE": skin(hexc("4A3A34"), hexc("2A1F1B")),
		"STEED": fur(steed, steed_d, steed_p, 1),
		"STEED_DARK": fur(steed_d, shade(steed_d, -30), steed, 2),
		"STEED_PALE": fur(steed_p, steed, shade(steed_p, 24), 3),
		"HART": fur(hart, hart_d, hart_p, 4, length=(3, 6)),
		"HART_DARK": fur(hart_d, shade(hart_d, -30), hart, 5, length=(3, 6)),
		"HART_PALE": fur(hart_p, hart, shade(hart_p, 16), 6, length=(3, 6)),
		"FLAG": fur(hexc("F2EEE4"), hexc("C9C2B4"), hexc("FFFFFF"), 7, length=(3, 7)),
		"ELK_PALE": fur(elk_p, elk, shade(elk_p, 20), 8),
		"ELK": fur(elk, elk_d, mix(elk, elk_p, 0.5), 9, length=(5, 9), density=40),
		"ELK_DARK": long_hair(elk_d, shade(elk_d, -14), elk, 10),
		"CRANE": feathers(crane, crane_d, hexc("FFFFFF")),
		"CRANE_DARK": feathers(crane_d, hexc("4E4C48"), crane, row_h=5, width=6),
		"CRANE_WING": flight(wing, wing_d, hexc("6CC4C4")),
		"CRANE_WING_DARK": flight(wing_d, hexc("0A3236"), wing),
		"CROWN": skin(hexc("C43A2E"), hexc("7A1E18")),
		"PRIMARY": flight(hexc("1E1F22"), hexc("0A0A0C"), hexc("4A4C52")),
		"ROC": feathers(roc, roc_d, hexc("E6D2A8"), row_h=7, width=10),
		"ROC_DARK": feathers(roc_d, hexc("3E2E1C"), roc, row_h=6, width=8),
		"ASTRAL": flight(astral, astral_d, hexc("9C8CE8")),
		"ASTRAL_DARK": flight(astral_d, hexc("160F3A"), astral),
		"JAG": short_fur(jag, hexc("A8701C"), jag_p, 11),
		"JAG_ROSETTE": rosettes(short_fur(jag, hexc("A8701C"), jag_p, 12), jag_d, hexc("B8741E")),
		"JAG_DARK": short_fur(jag_d, hexc("0E0806"), hexc("5A3818"), 13),
		"JAG_PALE": short_fur(jag_p, hexc("C9A870"), hexc("FFF8E8"), 14),
		"HOUND": fur(hound, hound_d, hound_p, 15, length=(3, 6), density=44),
		"HOUND_DARK": fur(hound_d, hexc("060508"), hound, 16, length=(3, 6), density=44),
		"HOUND_PALE": fur(hound_p, hound, hexc("C0BCC4"), 17, length=(3, 6)),
		"DUSK": short_fur(dusk, dusk_d, dusk_p, 18),
		"DUSK_DARK": short_fur(dusk_d, hexc("1C1628"), dusk, 19),
		"DUSK_PALE": short_fur(dusk_p, dusk, hexc("E6E0F0"), 20),
		"DUSK_ROSETTE": rosettes(short_fur(dusk, dusk_d, dusk_p, 22), hexc("2A2240"), hexc("6A5E86")),
		"STAR": stars(short_fur(dusk_d, hexc("1C1628"), dusk, 23), hexc("C8C0E0"), hexc("FFFFFF")),
		"FUR": fur(fur_c, fur_d, hexc("FFF0C8"), 24, length=(5, 9), density=38),
		"FUR_DARK": fur(fur_d, hexc("5E3C1A"), fur_c, 25, length=(5, 9), density=38),
		"SCALE": scales(scale, scale_d, hexc("C4F0EA")),
		"SCALE_DARK": scales(scale_d, hexc("1C4E58"), scale),
		"PEARL": plates(pearl, hexc("C8BE9C"), hexc("FFFFFF")),
		"WISH": plates(wish, hexc("B89428"), hexc("FFF4C0")),
		"HORN": horn(hexc("D8C8A8"), hexc("8E7A58")),
		"MEMBRANE": skin(hexc("C99A5A"), hexc("8E6430")),
		# Second and third pelts.
		"STEED_BLACK": fur(hexc("2A2624"), hexc("15120F"), hexc("4A4340"), 31),
		"STEED_BLACK_DARK": fur(hexc("15120F"), hexc("080706"), hexc("2A2624"), 32),
		"STEED_BLACK_PALE": fur(hexc("4A4340"), hexc("2A2624"), hexc("6A625E"), 33),
		"MANE_BLACK": long_hair(hexc("0E0C0B"), hexc("050404"), hexc("2A2624"), 34),
		"STEED_PALOMINO": fur(hexc("D9B36A"), hexc("A67C3A"), hexc("F2E2B8"), 35),
		"STEED_PALOMINO_DARK": fur(hexc("A67C3A"), hexc("6E4E20"), hexc("D9B36A"), 36),
		"STEED_PALOMINO_PALE": fur(hexc("F2E2B8"), hexc("D9B36A"), hexc("FFF8E6"), 37),
		"MANE_FLAXEN": long_hair(hexc("F0E4C4"), hexc("C8B88C"), hexc("FFFBEE"), 38),
		"HART_RED": fur(hexc("A8552E"), hexc("6B3218"), hexc("E8C7A6"), 39, length=(3, 6)),
		"HART_RED_DARK": fur(hexc("6B3218"), hexc("3E1C0C"), hexc("A8552E"), 40, length=(3, 6)),
		"HART_RED_PALE": fur(hexc("E8C7A6"), hexc("A8552E"), hexc("FCEBD8"), 41, length=(3, 6)),
		"HART_WHITE": fur(hexc("E9E3D6"), hexc("B8AE9C"), hexc("FFFFFF"), 42, length=(3, 6)),
		"HART_WHITE_DARK": fur(hexc("B8AE9C"), hexc("7E7566"), hexc("E9E3D6"), 43, length=(3, 6)),
		"HART_WHITE_PALE": fur(hexc("FFFFFF"), hexc("E9E3D6"), hexc("FFFFFF"), 44, length=(3, 6)),
		"ELK_BROWN": fur(hexc("8A5A34"), hexc("4E3018"), hexc("B08860"), 45, length=(5, 9), density=40),
		"ELK_BROWN_DARK": long_hair(hexc("4E3018"), hexc("2E1A0C"), hexc("8A5A34"), 46),
		"ELK_BROWN_PALE": fur(hexc("D6B889"), hexc("8A5A34"), hexc("F0DCB8"), 47),
		"ELK_GREY": fur(hexc("6E6A64"), hexc("3A3834"), hexc("9A9690"), 48, length=(5, 9), density=40),
		"ELK_GREY_DARK": long_hair(hexc("3A3834"), hexc("201E1C"), hexc("6E6A64"), 49),
		"ELK_GREY_PALE": fur(hexc("B8B4AC"), hexc("6E6A64"), hexc("DCD8D0"), 50),
		"CRANE_GREY": feathers(hexc("A9ABAE"), hexc("5E6064"), hexc("E0E2E4")),
		"CRANE_GREY_DARK": feathers(hexc("5E6064"), hexc("303236"), hexc("A9ABAE"), row_h=5, width=6),
		"CRANE_BLACK": feathers(hexc("2A2C30"), hexc("121316"), hexc("50535A")),
		"CRANE_BLACK_DARK": feathers(hexc("121316"), hexc("060607"), hexc("2A2C30"), row_h=5, width=6),
		"ROC_STORM": feathers(hexc("7C838C"), hexc("444A52"), hexc("B4BAC2"), row_h=7, width=10),
		"ROC_STORM_DARK": feathers(hexc("444A52"), hexc("24282E"), hexc("7C838C"), row_h=6, width=8),
		"ROC_RED": feathers(hexc("9A4A2E"), hexc("5A2A18"), hexc("D08C68"), row_h=7, width=10),
		"ROC_RED_DARK": feathers(hexc("5A2A18"), hexc("30160C"), hexc("9A4A2E"), row_h=6, width=8),
		"JAG_BLACK": short_fur(hexc("1E1A1C"), hexc("0B0A0A"), hexc("3A3436"), 51),
		"JAG_BLACK_ROSETTE": rosettes(short_fur(hexc("1E1A1C"), hexc("0B0A0A"), hexc("3A3436"), 52), hexc("0A0809"), hexc("2A2426")),
		"JAG_BLACK_DARK": short_fur(hexc("0B0A0A"), hexc("030303"), hexc("1E1A1C"), 53),
		"JAG_BLACK_PALE": short_fur(hexc("3A3436"), hexc("1E1A1C"), hexc("58525A"), 54),
		"JAG_SNOW": short_fur(hexc("F0EEE8"), hexc("BDBAB4"), hexc("FFFFFF"), 55),
		"JAG_SNOW_ROSETTE": rosettes(short_fur(hexc("F0EEE8"), hexc("BDBAB4"), hexc("FFFFFF"), 56), hexc("6E6C68"), hexc("C8C6C0")),
		"JAG_SNOW_DARK": short_fur(hexc("6A6664"), hexc("3E3C3A"), hexc("9A9692"), 57),
		"JAG_SNOW_PALE": short_fur(hexc("FFFFFF"), hexc("E0DEDA"), hexc("FFFFFF"), 58),
		"HOUND_GREY": fur(hexc("7A7C82"), hexc("3C3E44"), hexc("B8BAC0"), 59, length=(3, 6), density=44),
		"HOUND_GREY_DARK": fur(hexc("3C3E44"), hexc("1C1E22"), hexc("7A7C82"), 60, length=(3, 6), density=44),
		"HOUND_GREY_PALE": fur(hexc("B8BAC0"), hexc("7A7C82"), hexc("E0E2E6"), 61, length=(3, 6)),
		"HOUND_RED": fur(hexc("8E4A2A"), hexc("4A2412"), hexc("C89060"), 62, length=(3, 6), density=44),
		"HOUND_RED_DARK": fur(hexc("4A2412"), hexc("261108"), hexc("8E4A2A"), 63, length=(3, 6), density=44),
		"HOUND_RED_PALE": fur(hexc("C89060"), hexc("8E4A2A"), hexc("E8C098"), 64, length=(3, 6)),
		"DUSK_SILVER": short_fur(hexc("C0C2CC"), hexc("787A86"), hexc("ECEDF2"), 65),
		"DUSK_SILVER_DARK": short_fur(hexc("787A86"), hexc("44464F"), hexc("C0C2CC"), 66),
		"DUSK_SILVER_PALE": short_fur(hexc("ECEDF2"), hexc("C0C2CC"), hexc("FFFFFF"), 67),
		"DUSK_SILVER_ROSETTE": rosettes(short_fur(hexc("C0C2CC"), hexc("787A86"), hexc("ECEDF2"), 68), hexc("585A66"), hexc("9A9CA8")),
		"DUSK_SILVER_STAR": stars(short_fur(hexc("787A86"), hexc("44464F"), hexc("C0C2CC"), 69), hexc("E8EAF0"), hexc("FFFFFF")),
		"DUSK_INK": short_fur(hexc("1C1A26"), hexc("0C0B12"), hexc("3E3A50"), 70),
		"DUSK_INK_DARK": short_fur(hexc("0C0B12"), hexc("040306"), hexc("1C1A26"), 71),
		"DUSK_INK_PALE": short_fur(hexc("3E3A50"), hexc("1C1A26"), hexc("5E5876"), 72),
		"DUSK_INK_ROSETTE": rosettes(short_fur(hexc("1C1A26"), hexc("0C0B12"), hexc("3E3A50"), 73), hexc("06050A"), hexc("2A2638")),
		"DUSK_INK_STAR": stars(short_fur(hexc("0C0B12"), hexc("040306"), hexc("1C1A26"), 74), hexc("B0A8D0"), hexc("FFFFFF")),
		"SERPENT": scales(hexc("5A7A3A"), hexc("2E4420"), hexc("9CB86A"), size=4),
		"SERPENT_DARK": scales(hexc("2E4420"), hexc("162410"), hexc("5A7A3A"), size=4),
		"SERPENT_PALE": scales(hexc("C9C48A"), hexc("8A8656"), hexc("F0EDC0"), size=4),
		"SERPENT_JUNGLE": scales(hexc("2E8A4A"), hexc("174A28"), hexc("7ED890"), size=4),
		"SERPENT_JUNGLE_DARK": scales(hexc("174A28"), hexc("0A2614"), hexc("2E8A4A"), size=4),
		"SERPENT_JUNGLE_PALE": scales(hexc("D8E060"), hexc("8E9430"), hexc("F4F8A8"), size=4),
		"SERPENT_BONE": scales(hexc("D8CFB8"), hexc("8E8570"), hexc("FFFBEE"), size=4),
		"SERPENT_BONE_DARK": scales(hexc("6A6152"), hexc("3A3428"), hexc("D8CFB8"), size=4),
		"SERPENT_BONE_PALE": scales(hexc("F4EEDC"), hexc("B8B09A"), hexc("FFFFFF"), size=4),
		"FRILL": skin(hexc("C4442A"), hexc("7A1E12")),
		"FUR_JADE": fur(hexc("6FB89A"), hexc("3A7A60"), hexc("C8F0DC"), 75, length=(5, 9), density=38),
		"FUR_JADE_DARK": fur(hexc("3A7A60"), hexc("1E4A38"), hexc("6FB89A"), 76, length=(5, 9), density=38),
		"SCALE_JADE": scales(hexc("3AA0A8"), hexc("1E5C66"), hexc("9AE0E4")),
		"SCALE_JADE_DARK": scales(hexc("1E5C66"), hexc("0E3038"), hexc("3AA0A8")),
		"FUR_NIGHT": fur(hexc("3A2E48"), hexc("1E1728"), hexc("6A5A80"), 77, length=(5, 9), density=38),
		"FUR_NIGHT_DARK": fur(hexc("1E1728"), hexc("0E0A14"), hexc("3A2E48"), 78, length=(5, 9), density=38),
		"SCALE_NIGHT": scales(hexc("7A48B0"), hexc("3E2260"), hexc("C4A0EC")),
		"SCALE_NIGHT_DARK": scales(hexc("3E2260"), hexc("1E1030"), hexc("7A48B0")),
		# The bear: shaggy, in black, grizzly, and polar.
		"BEAR_BLACK": fur(hexc("1E1A18"), hexc("0E0C0B"), hexc("3A3430"), 79, length=(5, 9), density=40),
		"BEAR_BLACK_DARK": fur(hexc("0E0C0B"), hexc("050404"), hexc("1E1A18"), 80, length=(5, 9), density=40),
		"BEAR_BLACK_PALE": fur(hexc("A98B62"), hexc("6E5838"), hexc("D0B890"), 81, length=(4, 7)),
		"BEAR_GRIZZLY": fur(hexc("6B4A2B"), hexc("3E2A17"), hexc("B08E63"), 82, length=(5, 9), density=40),
		"BEAR_GRIZZLY_DARK": fur(hexc("3E2A17"), hexc("22160B"), hexc("6B4A2B"), 83, length=(5, 9), density=40),
		"BEAR_GRIZZLY_PALE": fur(hexc("B08E63"), hexc("6B4A2B"), hexc("D8BC94"), 84, length=(4, 7)),
		"BEAR_POLAR": fur(hexc("EDE6D6"), hexc("B8AE98"), hexc("FFFFFF"), 85, length=(5, 9), density=40),
		"BEAR_POLAR_DARK": fur(hexc("B8AE98"), hexc("7E7664"), hexc("EDE6D6"), 86, length=(5, 9), density=40),
		"BEAR_POLAR_PALE": fur(hexc("FFFFFF"), hexc("EDE6D6"), hexc("FFFFFF"), 87, length=(4, 7)),
	}
	for name, tile in cells.items():
		paste(image, tile, name)
	lid = hexc("1C1410")
	sclera = (236, 230, 214)
	eyes = {
		"EYE_HORSE": eye(lid, shade(lid, 14), hexc("A25A18"), (14, 9, 6), "bar"),
		"EYE_DEER": eye(lid, shade(lid, 12), hexc("7A4A18"), (12, 7, 4), "bar"),
		"EYE_CAT": eye(hexc("26140A"), (232, 220, 170), hexc("F2B829"), (8, 6, 4), "slit"),
		"EYE_CAT_GREEN": eye(hexc("0E0C0B"), (200, 232, 200), hexc("3FBF6A"), (4, 6, 4), "slit"),
		"EYE_SHADE": eye(hexc("2A2038"), (210, 206, 230), hexc("9E8CD0"), (6, 4, 10), "slit"),
		"EYE_HOUND": eye(hexc("17151A"), (226, 222, 214), hexc("E8B23A"), (10, 8, 6), "round"),
		"EYE_BIRD": eye(hexc("33231A"), (250, 246, 230), hexc("E68C1A"), (8, 6, 4), "round"),
		"EYE_ROC": eye(hexc("3E2E1C"), (246, 240, 220), hexc("F2C040"), (6, 4, 2), "round"),
		"EYE_DRAGON": eye(hexc("5E3C1A"), (236, 220, 170), hexc("F2A01E"), (6, 4, 2), "slit"),
	}
	for name, tile in eyes.items():
		paste(image, tile, name)
	image.save(OUT)
	print(OUT)


if __name__ == "__main__":
	main()
