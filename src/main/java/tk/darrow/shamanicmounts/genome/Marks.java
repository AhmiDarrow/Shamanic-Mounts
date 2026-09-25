package tk.darrow.shamanicmounts.genome;

/**
 * Alleles on the shared spirit-beast. Torso and head name a family, not a separate mesh
 * that has to be welded on: the body is one animal, and these values drive its lengths
 * and which socketed piece is showing.
 */
public final class Marks {
	private Marks() {
	}

	public enum Species {
		STEED(1.00f, 1.00f, 1.00f, 1.00f, 1.55f, 1.00f),
		HART(1.15f, 0.92f, 0.95f, 0.70f, 1.70f, 0.85f),
		CAT(0.72f, 1.10f, 0.90f, 1.25f, 1.25f, 0.55f),
		BIRD(1.55f, 0.85f, 0.80f, 0.45f, 1.80f, 1.40f),
		HOUND(0.95f, 0.96f, 0.92f, 1.15f, 1.40f, 1.20f),
		BEAR(0.80f, 1.25f, 1.15f, 0.20f, 1.50f, 0.90f),
		SERPENT(1.60f, 0.70f, 0.70f, 2.50f, 0.90f, 1.10f);

		public final float neck;
		public final float chest;
		public final float hip;
		public final float tail;
		public final float shoulder;
		public final float muzzle;

		Species(float neck, float chest, float hip, float tail, float shoulder, float muzzle) {
			this.neck = neck;
			this.chest = chest;
			this.hip = hip;
			this.tail = tail;
			this.shoulder = shoulder;
			this.muzzle = muzzle;
		}
	}

	public enum Torso implements Allele {
		STEED(Species.STEED), HART(Species.HART), CAT(Species.CAT), BIRD(Species.BIRD), HOUND(Species.HOUND),
		BEAR(Species.BEAR), SERPENT(Species.SERPENT);

		public final Species species;

		Torso(Species species) {
			this.species = species;
		}

		@Override
		public Locus locus() {
			return Locus.TORSO;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Head implements Allele {
		STEED(Species.STEED), HART(Species.HART), CAT(Species.CAT), BIRD(Species.BIRD), HOUND(Species.HOUND),
		BEAR(Species.BEAR), SERPENT(Species.SERPENT);

		public final Species species;

		Head(Species species) {
			this.species = species;
		}

		@Override
		public Locus locus() {
			return Locus.HEAD;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Leg implements Allele {
		NONE, TWO, FOUR, SPARE, EIGHT;

		@Override
		public Locus locus() {
			return Locus.LEGS;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	/** Bolts to the fetlock. Not chosen by the head. */
	public enum Foot implements Allele {
		HOOF, PAW, TALON;

		@Override
		public Locus locus() {
			return Locus.FOOT;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Rack implements Allele {
		NONE(0), BUDS(1), FULL(2), CROWN(3);

		private final int rank;

		Rack(int rank) {
			this.rank = rank;
		}

		@Override
		public Locus locus() {
			return Locus.RACK;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return rank;
		}
	}

	/** Land is quiet. Sea shows over it. Cinder hides until homozygous, and meets sea as mist. */
	public enum Gait implements Allele {
		LAND, SEA, CINDER;

		@Override
		public Locus locus() {
			return Locus.GAIT;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Wing implements Allele {
		NONE, VESTIGIAL, FULL, ASTRAL;

		@Override
		public Locus locus() {
			return Locus.WINGS;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	/** How big the wings grow. The two copies average, so a vast wing over a small one is a large one. */
	public enum Span implements Allele {
		SMALL(0.6f), MID(1.0f), LARGE(1.6f), GREAT(2.2f), VAST(3.0f);

		public final float factor;

		Span(float factor) {
			this.factor = factor;
		}

		@Override
		public Locus locus() {
			return Locus.WINGSPAN;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return ordinal();
		}
	}

	/** One tail on the dock, when the animal has one. */
	public enum Tail implements Allele {
		NONE, PLUME, FLAG, LASH, FAN;

		@Override
		public Locus locus() {
			return Locus.TAIL;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	/** Slight still stands as tall as a horse. Giant is larger than that. */
	public enum Scale implements Allele {
		SLIGHT, NORMAL, GIANT;

		@Override
		public Locus locus() {
			return Locus.SCALE;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Coat implements Allele {
		SOLID, ROSETTE, STAR, DUSK, BONE;

		@Override
		public Locus locus() {
			return Locus.COAT;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return this == BONE ? 2 : 1;
		}
	}

	/** Hearth is home. Any other realm shows, and two different realms show together. */
	public enum Realm implements Allele {
		HEARTH, UPPER, LOWER, SIDEWAYS;

		@Override
		public Locus locus() {
			return Locus.REALM;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return this == HEARTH ? 0 : 1;
		}
	}

	public enum Phase implements Allele {
		SOLID, VEIL, GHOST;

		@Override
		public Locus locus() {
			return Locus.PHASE;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Sense implements Allele {
		EYE, SCENT;

		@Override
		public Locus locus() {
			return Locus.SENSE;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Bond implements Allele {
		SADDLE, DRUM, NAGUAL;

		@Override
		public Locus locus() {
			return Locus.BOND;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return 0;
		}
	}

	public enum Ward implements Allele {
		NONE, LONGEVITY, GUARD;

		@Override
		public Locus locus() {
			return Locus.WARD;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return this == NONE ? 0 : 1;
		}
	}

	/**
	 * What the animal can do. Each gift has its own locus, so they can stack.
	 * Skin and veil hide unless both copies agree. A dream crossed with none is only a glimpse.
	 * Any other gift shows from one copy.
	 */
	public enum Gift implements Allele {
		NONE, ROAD, CALL, BEARING, FERRY, SKIN, OMEN, DREAM, VEIL, MIGHT, COIL;

		@Override
		public Locus locus() {
			return switch (this) {
				case ROAD -> Locus.ROAD;
				case CALL -> Locus.CALL;
				case BEARING -> Locus.BEARING;
				case FERRY -> Locus.FERRY;
				case SKIN -> Locus.SKIN;
				case OMEN -> Locus.OMEN;
				case DREAM -> Locus.DREAM;
				case VEIL -> Locus.VEIL;
				case MIGHT -> Locus.MIGHT;
				case COIL -> Locus.COIL;
				case NONE -> throw new IllegalStateException("none is not stored on a locus");
			};
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return this == NONE ? 0 : 1;
		}

		public boolean recessive() {
			return this == SKIN || this == VEIL;
		}
	}

	public enum Trail implements Allele {
		NONE, STAR, SHADOW, FROST;

		@Override
		public Locus locus() {
			return Locus.TRAIL;
		}

		@Override
		public String code() {
			return name().toLowerCase();
		}

		@Override
		public int rank() {
			return this == NONE ? 0 : 1;
		}
	}

	/** The quiet copy of one gift locus. */
	public static final class Off implements Allele {
		private static final Off ROAD = new Off(Locus.ROAD);
		private static final Off CALL = new Off(Locus.CALL);
		private static final Off BEARING = new Off(Locus.BEARING);
		private static final Off FERRY = new Off(Locus.FERRY);
		private static final Off SKIN = new Off(Locus.SKIN);
		private static final Off OMEN = new Off(Locus.OMEN);
		private static final Off DREAM = new Off(Locus.DREAM);
		private static final Off VEIL = new Off(Locus.VEIL);
		private static final Off MIGHT = new Off(Locus.MIGHT);
		private static final Off COIL = new Off(Locus.COIL);

		private final Locus locus;

		private Off(Locus locus) {
			this.locus = locus;
		}

		public static Off of(Locus locus) {
			return switch (locus) {
				case ROAD -> ROAD;
				case CALL -> CALL;
				case BEARING -> BEARING;
				case FERRY -> FERRY;
				case SKIN -> SKIN;
				case OMEN -> OMEN;
				case DREAM -> DREAM;
				case VEIL -> VEIL;
				case MIGHT -> MIGHT;
				case COIL -> COIL;
				default -> throw new IllegalArgumentException(locus.name());
			};
		}

		@Override
		public Locus locus() {
			return locus;
		}

		@Override
		public String code() {
			return "none";
		}

		@Override
		public int rank() {
			return 0;
		}
	}
}
