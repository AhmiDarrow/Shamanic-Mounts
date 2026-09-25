package tk.darrow.shamanicmounts.genome;

/** Maternal strand, paternal strand, and which copy of the head, feet, and tail is showing. */
public final class Genome {
	public final Strand maternal;
	public final Strand paternal;
	public final boolean headFromMaternal;
	public final boolean footFromMaternal;
	public final boolean tailFromMaternal;
	/** Birth flag. The body genes underneath are still the cross that produced it. */
	public final boolean chimera;

	public Genome(Strand maternal, Strand paternal, boolean headFromMaternal, boolean footFromMaternal,
			boolean tailFromMaternal) {
		this(maternal, paternal, headFromMaternal, footFromMaternal, tailFromMaternal, false);
	}

	public Genome(Strand maternal, Strand paternal, boolean headFromMaternal, boolean footFromMaternal,
			boolean tailFromMaternal, boolean chimera) {
		this.maternal = maternal;
		this.paternal = paternal;
		this.headFromMaternal = headFromMaternal;
		this.footFromMaternal = footFromMaternal;
		this.tailFromMaternal = tailFromMaternal;
		this.chimera = chimera;
	}

	/** The dragon form, with every gift turned on. The bred body stays in the strands. */
	public Genome asChimera() {
		return new Genome(everyGift(maternal), everyGift(paternal), headFromMaternal, footFromMaternal,
				tailFromMaternal, true);
	}

	private static Strand everyGift(Strand strand) {
		Strand out = strand;
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift != Marks.Gift.NONE) {
				out = out.with(gift);
			}
		}
		return out;
	}

	public static Genome homozygous(Strand strand) {
		return new Genome(strand, strand, true, true, true);
	}

	public Allele maternal(Locus locus) {
		return maternal.get(locus);
	}

	public Allele paternal(Locus locus) {
		return paternal.get(locus);
	}

	public Marks.Head head() {
		return (Marks.Head) (headFromMaternal ? maternal.get(Locus.HEAD) : paternal.get(Locus.HEAD));
	}

	public Marks.Head carriedHead() {
		return (Marks.Head) (headFromMaternal ? paternal.get(Locus.HEAD) : maternal.get(Locus.HEAD));
	}

	public Marks.Foot foot() {
		return (Marks.Foot) (footFromMaternal ? maternal.get(Locus.FOOT) : paternal.get(Locus.FOOT));
	}

	public Marks.Foot carriedFoot() {
		return (Marks.Foot) (footFromMaternal ? paternal.get(Locus.FOOT) : maternal.get(Locus.FOOT));
	}

	public Marks.Tail tail() {
		return (Marks.Tail) (tailFromMaternal ? maternal.get(Locus.TAIL) : paternal.get(Locus.TAIL));
	}

	public Marks.Tail carriedTail() {
		return (Marks.Tail) (tailFromMaternal ? paternal.get(Locus.TAIL) : maternal.get(Locus.TAIL));
	}
}
