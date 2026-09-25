package tk.darrow.shamanicmounts.genome;

/** Wild lines. Any two of them can breed; the child is still the same rig with a new mix of pieces. */
public final class Founders {
	private Founders() {
	}

	public static Genome eightfold() {
		return homozygous(Strand.wild(Marks.Torso.STEED, Marks.Head.STEED)
				.with(Marks.Leg.EIGHT)
				.with(Marks.Tail.PLUME)
				.with(Marks.Gait.SEA)
				.with(Marks.Realm.LOWER)
				.with(Marks.Gift.ROAD));
	}

	/** Crown over a full rack. The call and the star trail breed true; the rack does not. */
	public static Genome drumHart() {
		Strand maternal = Strand.wild(Marks.Torso.HART, Marks.Head.HART)
				.with(Marks.Rack.CROWN)
				.with(Marks.Tail.FLAG)
				.with(Marks.Bond.DRUM)
				.with(Marks.Realm.UPPER)
				.with(Marks.Ward.LONGEVITY)
				.with(Marks.Trail.STAR)
				.with(Marks.Gift.CALL);
		Strand paternal = Strand.wild(Marks.Torso.HART, Marks.Head.HART)
				.with(Marks.Rack.FULL)
				.with(Marks.Tail.FLAG)
				.with(Marks.Bond.DRUM)
				.with(Marks.Realm.UPPER)
				.with(Marks.Ward.LONGEVITY)
				.with(Marks.Trail.STAR)
				.with(Marks.Gift.CALL);
		return new Genome(maternal, paternal, true, true, true);
	}

	/** Both crowns, the greater size, and the bearing. The drum hart carries its crown over a full rack. */
	public static Genome elk() {
		return homozygous(Strand.wild(Marks.Torso.HART, Marks.Head.HART)
				.with(Marks.Rack.CROWN)
				.with(Marks.Scale.GIANT)
				.with(Marks.Tail.FLAG)
				.with(Marks.Realm.UPPER)
				.with(Marks.Gift.BEARING));
	}

	public static Genome crane() {
		return homozygous(Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD)
				.with(Marks.Leg.TWO)
				.with(Marks.Foot.TALON)
				.with(Marks.Wing.FULL)
				.with(Marks.Tail.FAN)
				.with(Marks.Coat.BONE)
				.with(Marks.Realm.UPPER)
				.with(Marks.Ward.LONGEVITY)
				.with(Marks.Gift.FERRY));
	}

	public static Genome nagual() {
		return homozygous(Strand.wild(Marks.Torso.CAT, Marks.Head.CAT)
				.with(Marks.Foot.PAW)
				.with(Marks.Tail.LASH)
				.with(Marks.Coat.ROSETTE)
				.with(Marks.Bond.NAGUAL)
				.with(Marks.Realm.LOWER)
				.with(Marks.Gift.SKIN));
	}

	public static Genome barghest() {
		return homozygous(Strand.wild(Marks.Torso.HOUND, Marks.Head.HOUND)
				.with(Marks.Foot.PAW)
				.with(Marks.Tail.LASH)
				.with(Marks.Sense.SCENT)
				.with(Marks.Realm.SIDEWAYS)
				.with(Marks.Trail.SHADOW)
				.with(Marks.Ward.GUARD)
				.with(Marks.Gift.OMEN));
	}

	public static Genome roc() {
		Strand maternal = Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD)
				.with(Marks.Leg.TWO)
				.with(Marks.Foot.TALON)
				.with(Marks.Wing.ASTRAL)
				.with(Marks.Span.LARGE)
				.with(Marks.Tail.FAN)
				.with(Marks.Scale.GIANT)
				.with(Marks.Realm.UPPER)
				.with(Marks.Coat.BONE)
				.with(Marks.Gift.DREAM);
		Strand paternal = Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD)
				.with(Marks.Leg.TWO)
				.with(Marks.Foot.TALON)
				.with(Marks.Wing.ASTRAL)
				.with(Marks.Span.VAST)
				.with(Marks.Tail.FAN)
				.with(Marks.Scale.GIANT)
				.with(Marks.Realm.SIDEWAYS)
				.with(Marks.Coat.BONE)
				.with(Marks.Gift.DREAM);
		return new Genome(maternal, paternal, true, true, true);
	}

	public static Genome shade() {
		return homozygous(Strand.wild(Marks.Torso.CAT, Marks.Head.CAT)
				.with(Marks.Foot.PAW)
				.with(Marks.Tail.LASH)
				.with(Marks.Coat.DUSK)
				.with(Marks.Phase.GHOST)
				.with(Marks.Realm.SIDEWAYS)
				.with(Marks.Trail.SHADOW)
				.with(Marks.Gift.VEIL));
	}

	/** Heavy, on paws, no tail to speak of. The might breeds true, and it stands guard like the hound. */
	public static Genome bear() {
		return homozygous(Strand.wild(Marks.Torso.BEAR, Marks.Head.BEAR)
				.with(Marks.Foot.PAW)
				.with(Marks.Scale.GIANT)
				.with(Marks.Realm.LOWER)
				.with(Marks.Ward.GUARD)
				.with(Marks.Gift.MIGHT));
	}

	/** No legs, a long body, a frilled head. It swims, and the coil breeds true. */
	public static Genome serpent() {
		return homozygous(Strand.wild(Marks.Torso.SERPENT, Marks.Head.SERPENT)
				.with(Marks.Leg.NONE)
				.with(Marks.Foot.PAW)
				.with(Marks.Tail.LASH)
				.with(Marks.Coat.ROSETTE)
				.with(Marks.Realm.LOWER)
				.with(Marks.Gait.SEA)
				.with(Marks.Gift.COIL));
	}

	private static Genome homozygous(Strand strand) {
		return Genome.homozygous(strand);
	}
}
