package tk.darrow.shamanicmounts.genome;

/** Bone-length multipliers on the shared body. Heterozygotes take the midpoint. */
public record Proportions(float neck, float chest, float hip, float tail, float shoulder, float muzzle) {
	public static Proportions of(Marks.Species species) {
		return new Proportions(species.neck, species.chest, species.hip, species.tail, species.shoulder, species.muzzle);
	}

	public static Proportions blend(Marks.Species maternal, Marks.Species paternal) {
		Proportions a = of(maternal);
		Proportions b = of(paternal);
		return new Proportions(mid(a.neck, b.neck), mid(a.chest, b.chest), mid(a.hip, b.hip), mid(a.tail, b.tail),
				mid(a.shoulder, b.shoulder), mid(a.muzzle, b.muzzle));
	}

	private static float mid(float a, float b) {
		return (a + b) * 0.5f;
	}
}
