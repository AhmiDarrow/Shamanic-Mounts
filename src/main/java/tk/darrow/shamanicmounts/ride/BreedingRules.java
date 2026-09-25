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

	public static boolean canPair(UUID offerOwner, UUID otherOwner, double distanceSq) {
		return offerOwner != null && offerOwner.equals(otherOwner) && distanceSq <= REACH * REACH;
	}
}
