package tk.darrow.shamanicmounts.genome;

import java.util.EnumMap;
import java.util.Map;

/** One copy of every locus. A mount keeps two of these. */
public final class Strand {
	private final Map<Locus, Allele> alleles;

	public Strand(Map<Locus, Allele> alleles) {
		EnumMap<Locus, Allele> copy = new EnumMap<>(Locus.class);
		copy.putAll(alleles);
		if (copy.size() != Locus.values().length) {
			throw new IllegalArgumentException("strand is missing a locus");
		}
		for (Locus locus : Locus.values()) {
			Allele allele = copy.get(locus);
			if (allele == null || allele.locus() != locus) {
				throw new IllegalArgumentException(locus.name());
			}
		}
		this.alleles = Map.copyOf(copy);
	}

	public Allele get(Locus locus) {
		return alleles.get(locus);
	}

	public Strand with(Allele allele) {
		EnumMap<Locus, Allele> copy = new EnumMap<>(Locus.class);
		copy.putAll(alleles);
		copy.put(allele.locus(), allele);
		return new Strand(copy);
	}

	/** Quiet defaults: four hoofed legs, no wings, hearth only. Torso and head still have to be named. */
	public static Strand wild(Marks.Torso torso, Marks.Head head) {
		EnumMap<Locus, Allele> map = new EnumMap<>(Locus.class);
		map.put(Locus.TORSO, torso);
		map.put(Locus.HEAD, head);
		map.put(Locus.LEGS, Marks.Leg.FOUR);
		map.put(Locus.FOOT, Marks.Foot.HOOF);
		map.put(Locus.RACK, Marks.Rack.NONE);
		map.put(Locus.GAIT, Marks.Gait.LAND);
		map.put(Locus.WINGS, Marks.Wing.NONE);
		map.put(Locus.WINGSPAN, Marks.Span.MID);
		map.put(Locus.TAIL, Marks.Tail.NONE);
		map.put(Locus.SCALE, Marks.Scale.NORMAL);
		map.put(Locus.COAT, Marks.Coat.SOLID);
		map.put(Locus.REALM, Marks.Realm.HEARTH);
		map.put(Locus.PHASE, Marks.Phase.SOLID);
		map.put(Locus.SENSE, Marks.Sense.EYE);
		map.put(Locus.BOND, Marks.Bond.SADDLE);
		map.put(Locus.WARD, Marks.Ward.NONE);
		map.put(Locus.TRAIL, Marks.Trail.NONE);
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift != Marks.Gift.NONE) {
				map.put(gift.locus(), Marks.Off.of(gift.locus()));
			}
		}
		return new Strand(map);
	}
}
