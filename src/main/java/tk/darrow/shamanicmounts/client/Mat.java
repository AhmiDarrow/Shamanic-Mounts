package tk.darrow.shamanicmounts.client;

/**
 * One 32×32 material cell on the 512×512 mount atlas. Faces read the cell one texel per model pixel,
 * in the part's own coordinates, so a coat runs unbroken across stacked cubes. Eye cells hold pixel
 * eyes in three sizes and a closed lid; see {@code tools/paint_mount_texture.py} for the layout.
 */
enum Mat {
	// Row 0: shared.
	MANE(0, 0),
	HOOF(1, 0),
	ANTLER(2, 0),
	BEAK(3, 0),
	TALON(4, 0),
	SADDLE(5, 0),
	GOLD(6, 0),
	NOSE(7, 0),
	// Row 1: steed and hart.
	STEED(0, 1),
	STEED_DARK(1, 1),
	STEED_PALE(2, 1),
	HART(3, 1),
	HART_DARK(4, 1),
	HART_PALE(5, 1),
	FLAG(6, 1),
	ELK_PALE(7, 1),
	// Row 2: elk and crane.
	ELK(0, 2),
	ELK_DARK(1, 2),
	CRANE(2, 2),
	CRANE_DARK(3, 2),
	CRANE_WING(4, 2),
	CRANE_WING_DARK(5, 2),
	CROWN(6, 2),
	PRIMARY(7, 2),
	// Row 3: roc and nagual.
	ROC(0, 3),
	ROC_DARK(1, 3),
	ASTRAL(2, 3),
	ASTRAL_DARK(3, 3),
	JAG(4, 3),
	JAG_ROSETTE(5, 3),
	JAG_DARK(6, 3),
	JAG_PALE(7, 3),
	// Row 4: barghest and shade.
	HOUND(0, 4),
	HOUND_DARK(1, 4),
	HOUND_PALE(2, 4),
	DUSK(3, 4),
	DUSK_DARK(4, 4),
	DUSK_PALE(5, 4),
	DUSK_ROSETTE(6, 4),
	STAR(7, 4),
	// Row 5: chimera.
	FUR(0, 5),
	FUR_DARK(1, 5),
	SCALE(2, 5),
	SCALE_DARK(3, 5),
	PEARL(4, 5),
	WISH(5, 5),
	HORN(6, 5),
	MEMBRANE(7, 5),
	// Row 6: eyes.
	EYE_HORSE(0, 6),
	EYE_DEER(1, 6),
	EYE_CAT(2, 6),
	EYE_SHADE(3, 6),
	EYE_HOUND(4, 6),
	EYE_BIRD(5, 6),
	EYE_ROC(6, 6),
	EYE_DRAGON(7, 6),
	// Row 7: tack.
	BAG(0, 7),
	BAG_DARK(1, 7),
	SADDLE_DARK(2, 7),
	ARMOR(3, 7),
	ARMOR_TRIM(4, 7),
	ARMOR_LEATHER(5, 7),
	ARMOR_GOLD(6, 7),
	ARMOR_DIAMOND(7, 7),
	// Rows 8 to 11: the second and third pelt of every line.
	STEED_BLACK(0, 8),
	STEED_BLACK_DARK(1, 8),
	STEED_BLACK_PALE(2, 8),
	MANE_BLACK(3, 8),
	STEED_PALOMINO(4, 8),
	STEED_PALOMINO_DARK(5, 8),
	STEED_PALOMINO_PALE(6, 8),
	MANE_FLAXEN(7, 8),
	HART_RED(8, 8),
	HART_RED_DARK(9, 8),
	HART_RED_PALE(10, 8),
	HART_WHITE(11, 8),
	HART_WHITE_DARK(12, 8),
	HART_WHITE_PALE(13, 8),
	ELK_BROWN(14, 8),
	ELK_BROWN_DARK(15, 8),
	ELK_BROWN_PALE(0, 9),
	ELK_GREY(1, 9),
	ELK_GREY_DARK(2, 9),
	ELK_GREY_PALE(3, 9),
	CRANE_GREY(4, 9),
	CRANE_GREY_DARK(5, 9),
	CRANE_BLACK(6, 9),
	CRANE_BLACK_DARK(7, 9),
	ROC_STORM(8, 9),
	ROC_STORM_DARK(9, 9),
	ROC_RED(10, 9),
	ROC_RED_DARK(11, 9),
	JAG_BLACK(12, 9),
	JAG_BLACK_ROSETTE(13, 9),
	JAG_BLACK_DARK(14, 9),
	JAG_BLACK_PALE(15, 9),
	JAG_SNOW(0, 10),
	JAG_SNOW_ROSETTE(1, 10),
	JAG_SNOW_DARK(2, 10),
	JAG_SNOW_PALE(3, 10),
	HOUND_GREY(4, 10),
	HOUND_GREY_DARK(5, 10),
	HOUND_GREY_PALE(6, 10),
	HOUND_RED(7, 10),
	HOUND_RED_DARK(8, 10),
	HOUND_RED_PALE(9, 10),
	DUSK_SILVER(10, 10),
	DUSK_SILVER_DARK(11, 10),
	DUSK_SILVER_PALE(12, 10),
	DUSK_SILVER_ROSETTE(13, 10),
	DUSK_SILVER_STAR(14, 10),
	DUSK_INK(15, 10),
	DUSK_INK_DARK(0, 11),
	DUSK_INK_PALE(1, 11),
	DUSK_INK_ROSETTE(2, 11),
	DUSK_INK_STAR(3, 11),
	FUR_JADE(4, 11),
	FUR_JADE_DARK(5, 11),
	SCALE_JADE(6, 11),
	SCALE_JADE_DARK(7, 11),
	FUR_NIGHT(8, 11),
	FUR_NIGHT_DARK(9, 11),
	SCALE_NIGHT(10, 11),
	SCALE_NIGHT_DARK(11, 11),
	// The panther's green eye, and the bear's three pelts.
	EYE_CAT_GREEN(12, 11),
	BEAR_BLACK(13, 11),
	BEAR_BLACK_DARK(14, 11),
	BEAR_BLACK_PALE(15, 11),
	BEAR_GRIZZLY(0, 12),
	BEAR_GRIZZLY_DARK(1, 12),
	BEAR_GRIZZLY_PALE(2, 12),
	BEAR_POLAR(3, 12),
	BEAR_POLAR_DARK(4, 12),
	BEAR_POLAR_PALE(5, 12),
	// Row 12 on: the serpent.
	SERPENT(6, 12),
	SERPENT_DARK(7, 12),
	SERPENT_PALE(8, 12),
	SERPENT_JUNGLE(9, 12),
	SERPENT_JUNGLE_DARK(10, 12),
	SERPENT_JUNGLE_PALE(11, 12),
	SERPENT_BONE(12, 12),
	SERPENT_BONE_DARK(13, 12),
	SERPENT_BONE_PALE(14, 12),
	FRILL(15, 12);

	/** Atlas texels per side. */
	static final float ATLAS = 512.0f;
	/** Cell texels per side. */
	static final int CELL = 32;

	final int column;
	final int row;

	Mat(int column, int row) {
		this.column = column;
		this.row = row;
	}

	/** Texel column of the cell's left edge. */
	int left() {
		return column * CELL;
	}

	/** Texel row of the cell's top edge. */
	int top() {
		return row * CELL;
	}
}
