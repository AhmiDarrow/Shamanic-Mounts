package tk.darrow.shamanicmounts.ride;

import java.util.UUID;

/**
 * Feed a Diamond Apple to one tame adult you own, then the other.
 * After a foal, those two rest for five minutes.
 */
public final class BreedingRules {
	public static final String ITEM_ID = "diamond_apple";
	/** How long a fed mount stays ready, in ticks. */
	public static final int OFFER_TICKS = 200;
	/** Rest after a foal. Five minutes. */
	public static final int REST_TICKS = 6000;
	public static final double REACH = 8.0;
	/** A foal's age at birth: it grows up over twenty minutes. */
	public static final int FOAL_AGE = -24000;
	/** A newborn foal's size, as a share of its grown size. */
	public static final float FOAL_SIZE = 0.3f;
	/** The foal grows a step a minute, so the hitbox does not change every tick. */
	public static final int GROWTH_STEPS = 20;

	private BreedingRules() {
	}

	/**
	 * An operator ignores the rest. Everyone else waits it out.
	 * The ready window is a separate timer and stays open for an operator until the pair is made.
	 */
	public static boolean canFeed(boolean tame, boolean baby, boolean owner, int restTicks, boolean breedItem,
			boolean ignoresTimers) {
		return tame && !baby && owner && breedItem && (ignoresTimers || restTicks <= 0);
	}

	/** The growth step for an age: 0 for a newborn, {@link #GROWTH_STEPS} once grown. */
	public static int growthStep(int age) {
		if (age >= 0) {
			return GROWTH_STEPS;
		}
		long left = Math.min(-(long) age, -FOAL_AGE);
		return GROWTH_STEPS - (int) ((left * GROWTH_STEPS - FOAL_AGE - 1) / -FOAL_AGE);
	}

	/** Size as a share of the grown mount: {@link #FOAL_SIZE} at birth, even steps up to whole. */
	public static float growthScale(int step) {
		int clamped = Math.max(0, Math.min(GROWTH_STEPS, step));
		return FOAL_SIZE + (1.0f - FOAL_SIZE) * clamped / GROWTH_STEPS;
	}

	public static boolean canPair(UUID offerOwner, UUID otherOwner, double distanceSq) {
		return offerOwner != null && offerOwner.equals(otherOwner) && distanceSq <= REACH * REACH;
	}
}
