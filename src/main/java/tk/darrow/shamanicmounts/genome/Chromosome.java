package tk.darrow.shamanicmounts.genome;

/**
 * Loci that travel together. Wings ride with their span; the tail has its own chromosome.
 * The gifts share one chromosome, in their own slots, so a foal can collect them.
 * Feet sit with the legs. The head stays with scale and coat.
 */
public enum Chromosome {
	FRAME(Locus.TORSO, Locus.LEGS, Locus.FOOT, Locus.RACK, Locus.GAIT),
	HIDE(Locus.HEAD, Locus.SCALE, Locus.COAT),
	WING(Locus.WINGS, Locus.WINGSPAN),
	TAIL(Locus.TAIL),
	JOURNEY(Locus.REALM, Locus.PHASE, Locus.SENSE),
	SEAT(Locus.BOND, Locus.WARD, Locus.TRAIL),
	GIFT(Locus.ROAD, Locus.CALL, Locus.BEARING, Locus.FERRY, Locus.SKIN, Locus.OMEN, Locus.DREAM, Locus.VEIL, Locus.MIGHT, Locus.COIL);

	private final Locus[] loci;

	Chromosome(Locus... loci) {
		this.loci = loci;
	}

	public int length() {
		return loci.length;
	}

	public Locus at(int index) {
		return loci[index];
	}

	public int indexOf(Locus locus) {
		for (int i = 0; i < loci.length; i++) {
			if (loci[i] == locus) {
				return i;
			}
		}
		return -1;
	}

	public static Chromosome holding(Locus locus) {
		for (Chromosome chromosome : values()) {
			if (chromosome.indexOf(locus) >= 0) {
				return chromosome;
			}
		}
		throw new IllegalArgumentException(locus.name());
	}
}
