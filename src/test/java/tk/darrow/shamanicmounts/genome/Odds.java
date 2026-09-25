package tk.darrow.shamanicmounts.genome;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact gamete odds, including how often two neighbouring traits stay on the same strand. */
public final class Odds {
	private Odds() {
	}

	public static double coupled(Locus a, Locus b, double crossover) {
		Chromosome left = Chromosome.holding(a);
		Chromosome right = Chromosome.holding(b);
		if (left != right) {
			return 0.5;
		}
		int distance = Math.abs(left.indexOf(a) - right.indexOf(b));
		if (distance == 0) {
			return 1.0;
		}
		double stay = 1.0;
		double step = 1.0 - 2.0 * crossover;
		for (int i = 0; i < distance; i++) {
			stay *= step;
		}
		return (1.0 + stay) / 2.0;
	}

	public static Map<Allele, Double> gameteDistribution(Genome genome, Locus locus, double crossover) {
		Chromosome chromosome = Chromosome.holding(locus);
		int index = chromosome.indexOf(locus);
		int intervals = chromosome.length() - 1;
		int masks = 1 << intervals;
		Map<Allele, Double> out = new LinkedHashMap<>();
		for (int start = 0; start < 2; start++) {
			for (int mask = 0; mask < masks; mask++) {
				double path = 0.5;
				boolean maternal = start == 0;
				Allele allele = null;
				for (int i = 0; i < chromosome.length(); i++) {
					if (i > 0) {
						boolean cross = ((mask >> (i - 1)) & 1) == 1;
						path *= cross ? crossover : (1.0 - crossover);
						if (cross) {
							maternal = !maternal;
						}
					}
					if (i == index) {
						allele = maternal ? genome.maternal.get(chromosome.at(i)) : genome.paternal.get(chromosome.at(i));
					}
				}
				out.merge(allele, path, Double::sum);
			}
		}
		return out;
	}

	public static Map<Marks.Head, Double> shownHead(Genome dam, Genome sire) {
		Map<Allele, Double> fromDam = gameteDistribution(dam, Locus.HEAD, Meiosis.CROSSOVER);
		Map<Allele, Double> fromSire = gameteDistribution(sire, Locus.HEAD, Meiosis.CROSSOVER);
		Map<Marks.Head, Double> shown = new LinkedHashMap<>();
		for (Map.Entry<Allele, Double> damAllele : fromDam.entrySet()) {
			for (Map.Entry<Allele, Double> sireAllele : fromSire.entrySet()) {
				double pair = damAllele.getValue() * sireAllele.getValue();
				shown.merge((Marks.Head) damAllele.getKey(), 0.5 * pair, Double::sum);
				shown.merge((Marks.Head) sireAllele.getKey(), 0.5 * pair, Double::sum);
			}
		}
		return shown;
	}
}
