package tk.darrow.shamanicmounts.ride;

import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Locus;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** What a ridden mount is allowed to do. The gift is the on-switch; a few body traits count on their own. */
public final class GiftRules {
	public static final int DRUM_COOLDOWN = 400;
	public static final int DRUM_DURATION = 100;
	public static final int DRUM_RANGE = 8;
	public static final int RAM_COOLDOWN = 80;
	public static final int MAUL_COOLDOWN = 60;
	public static final int COIL_COOLDOWN = 100;
	public static final int COIL_TICKS = 60;
	public static final float MAUL_DAMAGE = 7.0f;
	public static final int AWAY_DURATION = 600;
	public static final int AWAY_COOLDOWN = 1200;
	public static final int BLINK_COOLDOWN = 60;
	public static final int BLINK_BLOCKS = 6;
	/** A sneak released within this many ticks is a tap, which steps off a mount that uses sneak. */
	public static final int SNEAK_TAP_TICKS = 12;
	public static final int REVEAL_TICKS = 100;
	public static final int CLIMB_TICKS = 160;
	public static final int GLOW_NEAR = 32;
	public static final int GLOW_FAR = 48;
	public static final int CHEST_SLOTS = 15;

	private GiftRules() {
	}

	public static boolean waterWalk(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.ROAD)
				|| phenotype.gait == Phenotype.GaitShow.SEA
				|| phenotype.gait == Phenotype.GaitShow.MIST;
	}

	public static boolean fullStep(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.ROAD) || phenotype.legs == Phenotype.LegShow.EIGHT;
	}

	public static int glowRange(Phenotype phenotype) {
		if (!phenotype.gifts.contains(Marks.Gift.OMEN)) {
			return 0;
		}
		return phenotype.sense == Phenotype.SenseShow.SCENT ? GLOW_FAR : GLOW_NEAR;
	}

	public static boolean drum(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.CALL);
	}

	public static boolean ram(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.BEARING);
	}

	/** The serpent's coil: the attack control constricts, and it swims like it was born to. */
	public static boolean coil(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.COIL);
	}

	/** Both copies of the coil: the rider breathes under water. */
	public static boolean deepCoil(Genome genome) {
		return genome.maternal(Locus.COIL) == Marks.Gift.COIL && genome.paternal(Locus.COIL) == Marks.Gift.COIL;
	}

	/** The bear's might: the attack control mauls. */
	public static boolean might(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.MIGHT);
	}

	/** Both copies of the might: the rider takes less damage while mounted. */
	public static boolean thickHide(Genome genome) {
		return genome.maternal(Locus.MIGHT) == Marks.Gift.MIGHT && genome.paternal(Locus.MIGHT) == Marks.Gift.MIGHT;
	}

	public static boolean chest(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.BEARING);
	}

	public static boolean secondSeat(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.FERRY);
	}

	/** Full wings of either kind glide. The roc glides between climbs while its bar refills. */
	public static boolean glide(Phenotype phenotype) {
		return phenotype.wings == Phenotype.WingShow.FULL || phenotype.wings == Phenotype.WingShow.ASTRAL
				|| phenotype.wings == Phenotype.WingShow.ASTRAL_FULL;
	}

	public static boolean pinion(Phenotype phenotype) {
		return phenotype.wings == Phenotype.WingShow.PINION;
	}

	public static boolean nagual(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.SKIN);
	}

	/** Both ghost copies. One copy does not hide anyone. */
	public static boolean sneakHide(Phenotype phenotype) {
		return phenotype.phase == Phenotype.PhaseShow.GHOST;
	}

	/** Both veil copies. One copy does not blink. */
	public static boolean blink(Phenotype phenotype) {
		return phenotype.gifts.contains(Marks.Gift.VEIL);
	}

	public static boolean guard(Phenotype phenotype) {
		return phenotype.wards.contains(Marks.Ward.GUARD);
	}

	public static boolean longevity(Phenotype phenotype) {
		return phenotype.wards.contains(Marks.Ward.LONGEVITY);
	}

	public static boolean shadowSpeed(Phenotype phenotype) {
		return phenotype.trails.contains(Marks.Trail.SHADOW);
	}

	public static boolean dreamBoth(Genome genome) {
		return genome.maternal(Locus.DREAM) == Marks.Gift.DREAM && genome.paternal(Locus.DREAM) == Marks.Gift.DREAM;
	}

	/** One dream copy flies only at night. Both copies fly any time. */
	public static boolean canClimb(Phenotype phenotype, Genome genome, boolean night) {
		if (!phenotype.gifts.contains(Marks.Gift.DREAM)) {
			return false;
		}
		return dreamBoth(genome) || night;
	}

	/** Both dream copies: the climb does not drain hunger. */
	public static boolean climbFeedsHunger(Genome genome) {
		return !dreamBoth(genome);
	}

	/** Half of whatever exhaustion was added since the last sample. */
	public static float halvedExhaustion(float previous, float current) {
		float added = current - previous;
		if (added <= 0.0f) {
			return current;
		}
		return previous + added * 0.5f;
	}
}
