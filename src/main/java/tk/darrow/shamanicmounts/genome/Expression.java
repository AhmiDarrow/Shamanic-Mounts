package tk.darrow.shamanicmounts.genome;

import java.util.EnumSet;

/**
 * Turns two alleles into a module the shared rig can show.
 * A heterozygous structural cross is a midpoint or a smaller piece.
 * Wings and the tail are their own chromosomes, and the head does not choose the feet.
 */
public final class Expression {
	private Expression() {
	}

	public static Phenotype express(Genome genome) {
		Marks.Torso torsoM = (Marks.Torso) genome.maternal(Locus.TORSO);
		Marks.Torso torsoP = (Marks.Torso) genome.paternal(Locus.TORSO);
		Proportions proportions = Proportions.blend(torsoM.species, torsoP.species);
		Marks.Torso torso = torso(torsoM, torsoP);
		Phenotype.Shape shape = shape(torso.species, proportions);
		float sizeFactor = (((Marks.Size) genome.maternal(Locus.SIZE)).factor + ((Marks.Size) genome.paternal(Locus.SIZE)).factor) * 0.5f;
		int pelt = pelt((Marks.Pelt) genome.maternal(Locus.PELT), (Marks.Pelt) genome.paternal(Locus.PELT));

		Marks.Leg legM = (Marks.Leg) genome.maternal(Locus.LEGS);
		Marks.Leg legP = (Marks.Leg) genome.paternal(Locus.LEGS);
		Marks.Rack rackM = (Marks.Rack) genome.maternal(Locus.RACK);
		Marks.Rack rackP = (Marks.Rack) genome.paternal(Locus.RACK);
		Marks.Wing wingM = (Marks.Wing) genome.maternal(Locus.WINGS);
		Marks.Wing wingP = (Marks.Wing) genome.paternal(Locus.WINGS);
		Marks.Span spanM = (Marks.Span) genome.maternal(Locus.WINGSPAN);
		Marks.Span spanP = (Marks.Span) genome.paternal(Locus.WINGSPAN);
		Marks.Scale scaleM = (Marks.Scale) genome.maternal(Locus.SCALE);
		Marks.Scale scaleP = (Marks.Scale) genome.paternal(Locus.SCALE);
		Marks.Coat coatM = (Marks.Coat) genome.maternal(Locus.COAT);
		Marks.Coat coatP = (Marks.Coat) genome.paternal(Locus.COAT);

		Phenotype.ScaleShow scale = scale(scaleM, scaleP);
		Phenotype.PhaseShow phase = phase(genome);
		Phenotype.BondShow bond = bond(genome);
		EnumSet<Marks.Ward> wards = wards(genome);
		boolean ghost = phase == Phenotype.PhaseShow.GHOST;
		boolean nagual = bond == Phenotype.BondShow.NAGUAL;
		EnumSet<Marks.Gift> gifts = gifts(genome);

		return new Phenotype(torsoM, torsoP, torso, shape, sizeFactor, pelt, proportions, genome.head(), genome.carriedHead(), foot(genome.foot()),
				legs(legM, legP), rack(rackM, rackP), rackM == Marks.Rack.CROWN && rackP == Marks.Rack.CROWN,
				wings(wingM, wingP), (spanM.factor + spanP.factor) * 0.5f, tail(genome), scale, uniform(scale) * sizeFactor, coat(coatM, coatP), ghost, gait(genome), realms(genome),
				realmPotent(genome), phase, sense(genome), bond, wards, trails(genome), gifts,
				giftWhole(genome, gifts), ghost && nagual && !wards.contains(Marks.Ward.GUARD), genome.chimera);
	}

	/** The body that shows: the higher rank. */
	public static Marks.Torso torso(Marks.Torso a, Marks.Torso b) {
		return a.rank() >= b.rank() ? a : b;
	}

	/** The pelt that shows: A over B over C. */
	public static int pelt(Marks.Pelt a, Marks.Pelt b) {
		return (a.rank() >= b.rank() ? a : b).ordinal();
	}

	/**
	 * The hidden copy pulls the showing body halfway toward its own proportions, within limits so a
	 * cross never tears the rig: neck 0.75 to 1.35, head 0.8 to 1.25, tail 0.5 to 1.6, girth 0.85 to 1.2.
	 */
	static Phenotype.Shape shape(Marks.Species shown, Proportions blend) {
		float neck = clamp(blend.neck() / shown.neck, 0.75f, 1.35f);
		float head = clamp(blend.muzzle() / shown.muzzle, 0.8f, 1.25f);
		float tail = clamp(blend.tail() / Math.max(0.2f, shown.tail), 0.5f, 1.6f);
		float girth = clamp((blend.chest() / shown.chest + blend.hip() / shown.hip) * 0.5f, 0.85f, 1.2f);
		Phenotype.Shape shape = new Phenotype.Shape(round(neck), round(head), round(tail), round(girth));
		return shape.equals(Phenotype.Shape.PURE) ? Phenotype.Shape.PURE : shape;
	}

	private static float clamp(float value, float low, float high) {
		return Math.max(low, Math.min(high, value));
	}

	private static float round(float value) {
		return Math.round(value * 100f) / 100f;
	}

	/**
	 * Every ridden ability is actually on. Skin, veil, and the full dream need both copies.
	 * The other gifts count from one copy.
	 */
	public static boolean complete(Genome genome) {
		EnumSet<Marks.Gift> shown = gifts(genome);
		if (shown.size() != Marks.Gift.values().length - 1) {
			return false;
		}
		return genome.maternal(Locus.DREAM) == Marks.Gift.DREAM && genome.paternal(Locus.DREAM) == Marks.Gift.DREAM;
	}

	/** Four or two with eight shows the short middle pair. Two with four shows two long feet and a short pair. */
	/** No legs over any legs shows the other parent's legs: the serpent's coil is recessive in the frame. */
	static Phenotype.LegShow legs(Marks.Leg a, Marks.Leg b) {
		if (a == Marks.Leg.NONE && b != Marks.Leg.NONE) {
			return legs(b, b);
		}
		if (b == Marks.Leg.NONE && a != Marks.Leg.NONE) {
			return legs(a, a);
		}
		if (a == b) {
			return switch (a) {
				case NONE -> Phenotype.LegShow.NONE;
				case TWO -> Phenotype.LegShow.TWO;
				case FOUR -> Phenotype.LegShow.FOUR;
				case SPARE -> Phenotype.LegShow.SPARE;
				case EIGHT -> Phenotype.LegShow.EIGHT;
			};
		}
		if (is(a, b, Marks.Leg.FOUR, Marks.Leg.EIGHT) || is(a, b, Marks.Leg.TWO, Marks.Leg.EIGHT)) {
			return Phenotype.LegShow.SPARE;
		}
		if (is(a, b, Marks.Leg.TWO, Marks.Leg.FOUR) || is(a, b, Marks.Leg.TWO, Marks.Leg.SPARE)) {
			return Phenotype.LegShow.HITCH;
		}
		if (a == Marks.Leg.EIGHT || b == Marks.Leg.EIGHT) {
			return Phenotype.LegShow.EIGHT;
		}
		return Phenotype.LegShow.SPARE;
	}

	private static boolean is(Marks.Leg a, Marks.Leg b, Marks.Leg left, Marks.Leg right) {
		return (a == left && b == right) || (a == right && b == left);
	}

	/** A crown carried over nothing comes in as a full rack, not a spike stuck on a bare brow. */
	static Phenotype.RackShow rack(Marks.Rack a, Marks.Rack b) {
		if (a == b) {
			return switch (a) {
				case NONE -> Phenotype.RackShow.NONE;
				case BUDS -> Phenotype.RackShow.BUDS;
				case FULL -> Phenotype.RackShow.FULL;
				case CROWN -> Phenotype.RackShow.CROWN;
			};
		}
		Marks.Rack high = a.rank() >= b.rank() ? a : b;
		Marks.Rack low = a.rank() >= b.rank() ? b : a;
		if (low == Marks.Rack.NONE) {
			return switch (high) {
				case CROWN -> Phenotype.RackShow.FULL;
				case FULL, BUDS -> Phenotype.RackShow.BUDS;
				case NONE -> Phenotype.RackShow.NONE;
			};
		}
		return switch (high) {
			case CROWN -> Phenotype.RackShow.CROWN;
			case FULL -> Phenotype.RackShow.FULL;
			case BUDS -> Phenotype.RackShow.BUDS;
			case NONE -> Phenotype.RackShow.NONE;
		};
	}

	static Phenotype.WingShow wings(Marks.Wing a, Marks.Wing b) {
		if ((a == Marks.Wing.FULL && b == Marks.Wing.ASTRAL) || (a == Marks.Wing.ASTRAL && b == Marks.Wing.FULL)) {
			return Phenotype.WingShow.ASTRAL_FULL;
		}
		if (a == Marks.Wing.FULL && b == Marks.Wing.FULL) {
			return Phenotype.WingShow.FULL;
		}
		if (a == Marks.Wing.ASTRAL && b == Marks.Wing.ASTRAL) {
			return Phenotype.WingShow.ASTRAL;
		}
		if (a == Marks.Wing.FULL && b == Marks.Wing.VESTIGIAL || a == Marks.Wing.VESTIGIAL && b == Marks.Wing.FULL) {
			return Phenotype.WingShow.FULL;
		}
		if (a == Marks.Wing.NONE && b == Marks.Wing.NONE) {
			return Phenotype.WingShow.NONE;
		}
		return Phenotype.WingShow.PINION;
	}

	static Phenotype.ScaleShow scale(Marks.Scale a, Marks.Scale b) {
		if (a == Marks.Scale.GIANT && b == Marks.Scale.GIANT) {
			return Phenotype.ScaleShow.GREATER;
		}
		if (a == Marks.Scale.GIANT || b == Marks.Scale.GIANT) {
			return Phenotype.ScaleShow.LARGE;
		}
		if (a == Marks.Scale.SLIGHT && b == Marks.Scale.SLIGHT) {
			return Phenotype.ScaleShow.SLIGHT;
		}
		return Phenotype.ScaleShow.NORMAL;
	}

	/** 1 is a horse. Slight is that floor. Greater is the elk and the roc. */
	static float uniform(Phenotype.ScaleShow scale) {
		return switch (scale) {
			case SLIGHT -> 1.0f;
			case NORMAL -> 1.12f;
			case LARGE -> 1.28f;
			case GREATER -> 1.48f;
		};
	}

	/** A tail crossed with none comes in short. Two different tails mount one and carry the other. */
	static Phenotype.TailShow tail(Genome genome) {
		return tail((Marks.Tail) genome.maternal(Locus.TAIL), (Marks.Tail) genome.paternal(Locus.TAIL),
				genome.tailFromMaternal);
	}

	static Phenotype.TailShow tail(Marks.Tail maternal, Marks.Tail paternal, boolean fromMaternal) {
		if (maternal == paternal) {
			return shownTail(maternal);
		}
		if (maternal == Marks.Tail.NONE || paternal == Marks.Tail.NONE) {
			return Phenotype.TailShow.STUB;
		}
		return shownTail(fromMaternal ? maternal : paternal);
	}

	private static Phenotype.TailShow shownTail(Marks.Tail tail) {
		return switch (tail) {
			case NONE -> Phenotype.TailShow.NONE;
			case PLUME -> Phenotype.TailShow.PLUME;
			case FLAG -> Phenotype.TailShow.FLAG;
			case LASH -> Phenotype.TailShow.LASH;
			case FAN -> Phenotype.TailShow.FAN;
		};
	}

	/** Whichever foot allele is mounted. The head on the neck has no say. */
	static Phenotype.FootShow foot(Marks.Foot foot) {
		return switch (foot) {
			case HOOF -> Phenotype.FootShow.HOOF;
			case PAW -> Phenotype.FootShow.PAW;
			case TALON -> Phenotype.FootShow.TALON;
		};
	}

	static Phenotype.CoatShow coat(Marks.Coat a, Marks.Coat b) {
		if (a == Marks.Coat.BONE || b == Marks.Coat.BONE) {
			return Phenotype.CoatShow.BONE;
		}
		if (a == b) {
			return switch (a) {
				case SOLID -> Phenotype.CoatShow.SOLID;
				case ROSETTE -> Phenotype.CoatShow.ROSETTE;
				case STAR -> Phenotype.CoatShow.STAR;
				case DUSK -> Phenotype.CoatShow.DUSK;
				case BONE -> Phenotype.CoatShow.BONE;
			};
		}
		EnumSet<Marks.Coat> pair = EnumSet.of(a, b);
		if (pair.equals(EnumSet.of(Marks.Coat.SOLID, Marks.Coat.ROSETTE))) {
			return Phenotype.CoatShow.SOLID;
		}
		if (pair.equals(EnumSet.of(Marks.Coat.SOLID, Marks.Coat.STAR))
				|| pair.equals(EnumSet.of(Marks.Coat.ROSETTE, Marks.Coat.STAR))) {
			return Phenotype.CoatShow.SPECKLED;
		}
		if (pair.equals(EnumSet.of(Marks.Coat.SOLID, Marks.Coat.DUSK))) {
			return Phenotype.CoatShow.DUSK_WASH;
		}
		if (pair.equals(EnumSet.of(Marks.Coat.ROSETTE, Marks.Coat.DUSK))) {
			return Phenotype.CoatShow.DUSK_ROSETTE;
		}
		return Phenotype.CoatShow.DUSK_STAR;
	}

	static Phenotype.GaitShow gait(Genome genome) {
		Marks.Gait a = (Marks.Gait) genome.maternal(Locus.GAIT);
		Marks.Gait b = (Marks.Gait) genome.paternal(Locus.GAIT);
		if ((a == Marks.Gait.SEA && b == Marks.Gait.CINDER) || (a == Marks.Gait.CINDER && b == Marks.Gait.SEA)) {
			return Phenotype.GaitShow.MIST;
		}
		if (a == Marks.Gait.CINDER && b == Marks.Gait.CINDER) {
			return Phenotype.GaitShow.CINDER;
		}
		if (a == Marks.Gait.SEA || b == Marks.Gait.SEA) {
			return Phenotype.GaitShow.SEA;
		}
		return Phenotype.GaitShow.LAND;
	}

	static EnumSet<Marks.Realm> realms(Genome genome) {
		EnumSet<Marks.Realm> realms = EnumSet.noneOf(Marks.Realm.class);
		addRealm(realms, (Marks.Realm) genome.maternal(Locus.REALM));
		addRealm(realms, (Marks.Realm) genome.paternal(Locus.REALM));
		return realms;
	}

	private static void addRealm(EnumSet<Marks.Realm> realms, Marks.Realm realm) {
		if (realm != Marks.Realm.HEARTH) {
			realms.add(realm);
		}
	}

	static boolean realmPotent(Genome genome) {
		Marks.Realm a = (Marks.Realm) genome.maternal(Locus.REALM);
		return a != Marks.Realm.HEARTH && a == genome.paternal(Locus.REALM);
	}

	static Phenotype.PhaseShow phase(Genome genome) {
		Marks.Phase a = (Marks.Phase) genome.maternal(Locus.PHASE);
		Marks.Phase b = (Marks.Phase) genome.paternal(Locus.PHASE);
		if (a == Marks.Phase.GHOST && b == Marks.Phase.GHOST) {
			return Phenotype.PhaseShow.GHOST;
		}
		if ((a == Marks.Phase.VEIL && b == Marks.Phase.GHOST) || (a == Marks.Phase.GHOST && b == Marks.Phase.VEIL)) {
			return Phenotype.PhaseShow.DEEP_VEIL;
		}
		if (a == Marks.Phase.VEIL || b == Marks.Phase.VEIL) {
			return Phenotype.PhaseShow.VEIL;
		}
		return Phenotype.PhaseShow.SOLID;
	}

	static Phenotype.SenseShow sense(Genome genome) {
		Marks.Sense a = (Marks.Sense) genome.maternal(Locus.SENSE);
		Marks.Sense b = (Marks.Sense) genome.paternal(Locus.SENSE);
		return a == Marks.Sense.SCENT && b == Marks.Sense.SCENT
				? Phenotype.SenseShow.SCENT
				: Phenotype.SenseShow.EYE;
	}

	static Phenotype.BondShow bond(Genome genome) {
		Marks.Bond a = (Marks.Bond) genome.maternal(Locus.BOND);
		Marks.Bond b = (Marks.Bond) genome.paternal(Locus.BOND);
		if (a == Marks.Bond.NAGUAL && b == Marks.Bond.NAGUAL) {
			return Phenotype.BondShow.NAGUAL;
		}
		if (a == Marks.Bond.DRUM || b == Marks.Bond.DRUM) {
			return Phenotype.BondShow.DRUM;
		}
		return Phenotype.BondShow.SADDLE;
	}

	static EnumSet<Marks.Ward> wards(Genome genome) {
		return actives(Marks.Ward.class, (Marks.Ward) genome.maternal(Locus.WARD),
				(Marks.Ward) genome.paternal(Locus.WARD), Marks.Ward.NONE);
	}

	/** Skin and veil stay hidden unless the two copies match. Every other gift shows from one copy. */
	static EnumSet<Marks.Gift> gifts(Genome genome) {
		EnumSet<Marks.Gift> shown = EnumSet.noneOf(Marks.Gift.class);
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift == Marks.Gift.NONE) {
				continue;
			}
			boolean maternal = genome.maternal(gift.locus()) == gift;
			boolean paternal = genome.paternal(gift.locus()) == gift;
			if (maternal && paternal) {
				shown.add(gift);
			} else if ((maternal || paternal) && !gift.recessive()) {
				shown.add(gift);
			}
		}
		return shown;
	}

	static boolean giftWhole(Genome genome, EnumSet<Marks.Gift> shown) {
		if (shown.isEmpty()) {
			return false;
		}
		for (Marks.Gift gift : shown) {
			if (genome.maternal(gift.locus()) != gift || genome.paternal(gift.locus()) != gift) {
				return false;
			}
		}
		return true;
	}

	static EnumSet<Marks.Trail> trails(Genome genome) {
		return actives(Marks.Trail.class, (Marks.Trail) genome.maternal(Locus.TRAIL),
				(Marks.Trail) genome.paternal(Locus.TRAIL), Marks.Trail.NONE);
	}

	private static <T extends Enum<T>> EnumSet<T> actives(Class<T> type, T a, T b, T none) {
		EnumSet<T> set = EnumSet.noneOf(type);
		if (a != none) {
			set.add(a);
		}
		if (b != none) {
			set.add(b);
		}
		return set;
	}
}
