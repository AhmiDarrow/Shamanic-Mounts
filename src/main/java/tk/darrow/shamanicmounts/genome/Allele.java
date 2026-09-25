package tk.darrow.shamanicmounts.genome;

/** One version of a locus. Rank breaks complete-dominance ties; equal ranks can share the phenotype. */
public interface Allele {
	Locus locus();

	String code();

	int rank();
}
