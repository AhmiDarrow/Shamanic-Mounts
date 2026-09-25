package tk.darrow.shamanicmounts.genome;

import java.util.EnumMap;
import java.util.Random;

/** One gamete per parent. Neighbouring loci usually stay together; a crossover breaks the block. */
public final class Meiosis {
	public static final double CROSSOVER = 0.12;

	private Meiosis() {
	}

	public static Strand gamete(Genome parent, Random random, double crossover) {
		EnumMap<Locus, Allele> out = new EnumMap<>(Locus.class);
		for (Chromosome chromosome : Chromosome.values()) {
			boolean maternal = random.nextBoolean();
			for (int i = 0; i < chromosome.length(); i++) {
				if (i > 0 && crossover > 0.0 && random.nextDouble() < crossover) {
					maternal = !maternal;
				}
				Locus locus = chromosome.at(i);
				out.put(locus, maternal ? parent.maternal.get(locus) : parent.paternal.get(locus));
			}
		}
		return new Strand(out);
	}

	public static Genome child(Genome dam, Genome sire, Random random) {
		Strand maternal = gamete(dam, random, CROSSOVER);
		Strand paternal = gamete(sire, random, CROSSOVER);
		Genome born = new Genome(maternal, paternal, random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
		if (Expression.complete(dam) && Expression.complete(sire) && random.nextBoolean()) {
			return born.asChimera();
		}
		return born;
	}
}
