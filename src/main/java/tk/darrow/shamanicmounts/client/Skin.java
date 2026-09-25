package tk.darrow.shamanicmounts.client;

import tk.darrow.shamanicmounts.genome.Phenotype;

/**
 * The materials one mount is cut from: the coat, its dark points, its pale underside, its hair, its
 * eye, and for the cats the rosetted and starred versions of the coat. Every line comes in three
 * pelts; {@link #of} picks the set for a mount's pelt and lets its coat gene choose the marking.
 */
record Skin(Mat base, Mat dark, Mat pale, Mat hair, Mat eye, Mat rosette, Mat star) {
	Skin(Mat base, Mat dark, Mat pale, Mat hair, Mat eye) {
		this(base, dark, pale, hair, eye, base, base);
	}

	static final Skin[] STEED = {
			new Skin(Mat.STEED, Mat.STEED_DARK, Mat.STEED_PALE, Mat.MANE, Mat.EYE_HORSE),
			new Skin(Mat.STEED_BLACK, Mat.STEED_BLACK_DARK, Mat.STEED_BLACK_PALE, Mat.MANE_BLACK, Mat.EYE_HORSE),
			new Skin(Mat.STEED_PALOMINO, Mat.STEED_PALOMINO_DARK, Mat.STEED_PALOMINO_PALE, Mat.MANE_FLAXEN, Mat.EYE_HORSE) };
	static final Skin[] HART = {
			new Skin(Mat.HART, Mat.HART_DARK, Mat.HART_PALE, Mat.HART_DARK, Mat.EYE_DEER),
			new Skin(Mat.HART_RED, Mat.HART_RED_DARK, Mat.HART_RED_PALE, Mat.HART_RED_DARK, Mat.EYE_DEER),
			new Skin(Mat.HART_WHITE, Mat.HART_WHITE_DARK, Mat.HART_WHITE_PALE, Mat.HART_WHITE_DARK, Mat.EYE_DEER) };
	static final Skin[] ELK = {
			new Skin(Mat.ELK, Mat.ELK_DARK, Mat.ELK_PALE, Mat.ELK_DARK, Mat.EYE_DEER),
			new Skin(Mat.ELK_BROWN, Mat.ELK_BROWN_DARK, Mat.ELK_BROWN_PALE, Mat.ELK_BROWN_DARK, Mat.EYE_DEER),
			new Skin(Mat.ELK_GREY, Mat.ELK_GREY_DARK, Mat.ELK_GREY_PALE, Mat.ELK_GREY_DARK, Mat.EYE_DEER) };
	static final Skin[] CRANE = {
			new Skin(Mat.CRANE, Mat.CRANE_DARK, Mat.CRANE, Mat.CRANE_DARK, Mat.EYE_BIRD),
			new Skin(Mat.CRANE_GREY, Mat.CRANE_GREY_DARK, Mat.CRANE_GREY, Mat.CRANE_GREY_DARK, Mat.EYE_BIRD),
			new Skin(Mat.CRANE_BLACK, Mat.CRANE_BLACK_DARK, Mat.CRANE_BLACK, Mat.CRANE_BLACK_DARK, Mat.EYE_BIRD) };
	static final Skin[] ROC = {
			new Skin(Mat.ROC, Mat.ROC_DARK, Mat.ROC, Mat.ROC_DARK, Mat.EYE_ROC),
			new Skin(Mat.ROC_STORM, Mat.ROC_STORM_DARK, Mat.ROC_STORM, Mat.ROC_STORM_DARK, Mat.EYE_ROC),
			new Skin(Mat.ROC_RED, Mat.ROC_RED_DARK, Mat.ROC_RED, Mat.ROC_RED_DARK, Mat.EYE_ROC) };
	static final Skin[] NAGUAL = {
			new Skin(Mat.JAG, Mat.JAG_DARK, Mat.JAG_PALE, Mat.JAG_DARK, Mat.EYE_CAT, Mat.JAG_ROSETTE, Mat.JAG_ROSETTE),
			new Skin(Mat.JAG_BLACK, Mat.JAG_BLACK_DARK, Mat.JAG_BLACK_PALE, Mat.JAG_BLACK_DARK, Mat.EYE_CAT_GREEN,
					Mat.JAG_BLACK_ROSETTE, Mat.JAG_BLACK_ROSETTE),
			new Skin(Mat.JAG_SNOW, Mat.JAG_SNOW_DARK, Mat.JAG_SNOW_PALE, Mat.JAG_SNOW_DARK, Mat.EYE_CAT, Mat.JAG_SNOW_ROSETTE,
					Mat.JAG_SNOW_ROSETTE) };
	static final Skin[] BARGHEST = {
			new Skin(Mat.HOUND, Mat.HOUND_DARK, Mat.HOUND_PALE, Mat.HOUND_DARK, Mat.EYE_HOUND),
			new Skin(Mat.HOUND_GREY, Mat.HOUND_GREY_DARK, Mat.HOUND_GREY_PALE, Mat.HOUND_GREY_DARK, Mat.EYE_HOUND),
			new Skin(Mat.HOUND_RED, Mat.HOUND_RED_DARK, Mat.HOUND_RED_PALE, Mat.HOUND_RED_DARK, Mat.EYE_HOUND) };
	static final Skin[] SHADE = {
			new Skin(Mat.DUSK, Mat.DUSK_DARK, Mat.DUSK_PALE, Mat.DUSK_DARK, Mat.EYE_SHADE, Mat.DUSK_ROSETTE, Mat.STAR),
			new Skin(Mat.DUSK_SILVER, Mat.DUSK_SILVER_DARK, Mat.DUSK_SILVER_PALE, Mat.DUSK_SILVER_DARK, Mat.EYE_SHADE,
					Mat.DUSK_SILVER_ROSETTE, Mat.DUSK_SILVER_STAR),
			new Skin(Mat.DUSK_INK, Mat.DUSK_INK_DARK, Mat.DUSK_INK_PALE, Mat.DUSK_INK_DARK, Mat.EYE_SHADE, Mat.DUSK_INK_ROSETTE,
					Mat.DUSK_INK_STAR) };
	static final Skin[] CHIMERA = {
			new Skin(Mat.FUR, Mat.FUR_DARK, Mat.FUR, Mat.FUR_DARK, Mat.EYE_DRAGON, Mat.SCALE, Mat.SCALE_DARK),
			new Skin(Mat.FUR_JADE, Mat.FUR_JADE_DARK, Mat.FUR_JADE, Mat.FUR_JADE_DARK, Mat.EYE_DRAGON, Mat.SCALE_JADE, Mat.SCALE_JADE_DARK),
			new Skin(Mat.FUR_NIGHT, Mat.FUR_NIGHT_DARK, Mat.FUR_NIGHT, Mat.FUR_NIGHT_DARK, Mat.EYE_DRAGON, Mat.SCALE_NIGHT,
					Mat.SCALE_NIGHT_DARK) };

	/** The bear, the ninth line, whose body is still to come. Black and grizzly look out of a hound's eye, polar a deer's. */
	static final Skin[] SERPENT = {
			new Skin(Mat.SERPENT, Mat.SERPENT_DARK, Mat.SERPENT_PALE, Mat.SERPENT_DARK, Mat.EYE_CAT, Mat.SERPENT, Mat.SERPENT),
			new Skin(Mat.SERPENT_JUNGLE, Mat.SERPENT_JUNGLE_DARK, Mat.SERPENT_JUNGLE_PALE, Mat.SERPENT_JUNGLE_DARK, Mat.EYE_CAT_GREEN,
					Mat.SERPENT_JUNGLE, Mat.SERPENT_JUNGLE),
			new Skin(Mat.SERPENT_BONE, Mat.SERPENT_BONE_DARK, Mat.SERPENT_BONE_PALE, Mat.SERPENT_BONE_DARK, Mat.EYE_SHADE, Mat.SERPENT_BONE,
					Mat.SERPENT_BONE) };
	static final Skin[] BEAR = {
			new Skin(Mat.BEAR_BLACK, Mat.BEAR_BLACK_DARK, Mat.BEAR_BLACK_PALE, Mat.BEAR_BLACK_DARK, Mat.EYE_HOUND),
			new Skin(Mat.BEAR_GRIZZLY, Mat.BEAR_GRIZZLY_DARK, Mat.BEAR_GRIZZLY_PALE, Mat.BEAR_GRIZZLY_DARK, Mat.EYE_HOUND),
			new Skin(Mat.BEAR_POLAR, Mat.BEAR_POLAR_DARK, Mat.BEAR_POLAR_PALE, Mat.BEAR_POLAR_DARK, Mat.EYE_DEER) };

	/** The names the herd book prints, per line (in {@link MountMesh.Shell} order, then the bear) and pelt. */
	static final String[][] PELT_NAMES = {
			{ "Bay", "Black", "Palomino" }, { "Tan", "Red", "White" }, { "Dark", "Brown", "Grey" }, { "White", "Grey", "Black" },
			{ "Bone", "Storm", "Red" }, { "Gold", "Panther", "Snow" }, { "Black", "Grey", "Red" }, { "Dusk", "Silver", "Ink" },
			{ "Amber", "Jade", "Night" }, { "Black", "Grizzly", "Polar" }, { "River", "Jungle", "Bone" } };

	/** The material set for one mount: its line, its pelt, and what its coat gene makes of the pelt. */
	static Skin of(MountMesh.Shell shell, int pelt, Phenotype phenotype) {
		int which = Math.floorMod(pelt, 3);
		Skin[] line = switch (shell) {
			case STEED -> STEED;
			case HART -> HART;
			case ELK -> ELK;
			case CRANE -> CRANE;
			case ROC -> ROC;
			case NAGUAL -> NAGUAL;
			case BARGHEST -> BARGHEST;
			case SHADE -> SHADE;
			case CHIMERA -> CHIMERA;
			case BEAR -> BEAR;
			case SERPENT -> SERPENT;
		};
		Skin skin = line[which];
		if (shell == MountMesh.Shell.NAGUAL || shell == MountMesh.Shell.SHADE) {
			Skin dusk = SHADE[which];
			return switch (phenotype.coat) {
				case ROSETTE -> skin.withBase(skin.rosette());
				case SOLID -> skin;
				case DUSK, DUSK_WASH -> shell == MountMesh.Shell.NAGUAL ? dusk : skin;
				case DUSK_ROSETTE -> dusk.withBase(dusk.rosette());
				case STAR, DUSK_STAR -> dusk.withBase(dusk.star());
				default -> skin;
			};
		}
		return skin;
	}

	Skin withBase(Mat coat) {
		return new Skin(coat, dark, pale, hair, eye, rosette, star);
	}
}
