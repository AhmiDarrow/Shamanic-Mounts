package tk.darrow.shamanicmounts.genome;

/**
 * Standing size in blocks, and where the body and the saddle sit on the rig. The floor is a
 * vanilla horse. The rig's numbers are blender pixels with the feet on zero and the chest front
 * near zero along the body; see {@code client/Torsos}.
 */
public final class MountSize {
	public static final float HORSE_HEIGHT = 1.6f;
	public static final float HORSE_WIDTH = 1.3964844f;

	/** Which body the rig builds. Client and server agree on this, so the seat lands on the saddle. */
	public enum Form {
		STEED(11f, 8f, 21.5f),
		HART(9f, 7f, 22.5f),
		ELK(11f, 9f, 24.5f),
		CRANE(6f, 6f, 28.5f),
		ROC(9f, 7f, 23.5f),
		NAGUAL(11f, 8f, 18.5f),
		BARGHEST(9f, 7f, 21.5f),
		SHADE(9f, 7f, 16.5f),
		CHIMERA(12f, 10f, 22.5f),
		BEAR(11f, 8f, 22.5f),
		SERPENT(10f, 6f, 15.5f);

		/** The middle of the body along its length, which the hitbox is centred on. */
		public final float centre;
		/** The middle of the saddle along the body, and the top of the saddle. */
		public final float seatAlong;
		public final float seatHigh;

		Form(float centre, float seatAlong, float seatHigh) {
			this.centre = centre;
			this.seatAlong = seatAlong;
			this.seatHigh = seatHigh;
		}
	}

	private MountSize() {
	}

	public static float height(Phenotype phenotype) {
		return HORSE_HEIGHT * phenotype.uniformScale;
	}

	public static float width(Phenotype phenotype) {
		return HORSE_WIDTH * phenotype.uniformScale;
	}

	public static Form form(Phenotype phenotype) {
		if (phenotype.chimera) {
			return Form.CHIMERA;
		}
		Marks.Torso torso = phenotype.torso;
		if (torso == Marks.Torso.HART && phenotype.crownHeavy) {
			return Form.ELK;
		}
		if (torso == Marks.Torso.BIRD
				&& (phenotype.wings == Phenotype.WingShow.ASTRAL || phenotype.wings == Phenotype.WingShow.ASTRAL_FULL)) {
			return Form.ROC;
		}
		if (torso == Marks.Torso.CAT && phenotype.bond == Phenotype.BondShow.NAGUAL) {
			return Form.NAGUAL;
		}
		return switch (torso) {
			case STEED -> Form.STEED;
			case HART -> Form.HART;
			case BIRD -> Form.CRANE;
			case HOUND -> Form.BARGHEST;
			case CAT -> Form.SHADE;
			case BEAR -> Form.BEAR;
			case SERPENT -> Form.SERPENT;
		};
	}

	/** How far the rig is drawn forward of the entity, in blocks, so the hitbox centres on the body. */
	public static float centreOffset(Phenotype phenotype) {
		return form(phenotype).centre / 16f * phenotype.uniformScale;
	}

	/**
	 * The rider's seat in blocks: how far toward the tail from the entity's centre (negative is
	 * forward) and how high above its feet. {@code index} 1 is the second seat, behind the first.
	 */
	public static float[] seat(Phenotype phenotype, int index) {
		Form form = form(phenotype);
		float scale = phenotype.uniformScale / 16f;
		float along = (form.seatAlong - form.centre + (index > 0 ? 9f : 0f)) * scale;
		return new float[] { along, form.seatHigh * scale };
	}
}
