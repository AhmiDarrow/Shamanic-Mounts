package tk.darrow.shamanicmounts.tack;

/**
 * Meals for a tame. A spirit mount is not tamed by food and does not breed on it (that is the Diamond Apple), but any
 * meat mends it: raw or cooked, vanilla or another mod's, by the meat's own nutrition. Rotten flesh is not a meal.
 */
public final class FeedRules {
	/** The least a meal mends, so a thin cut still does something. */
	public static final float MIN_HEAL = 2.0f;

	private FeedRules() {
	}

	/**
	 * Health a meal restores, or 0 when the mount will not take it: a wild mount, something that is not meat, or a
	 * mount already at full health (the meal stays in hand and the click goes on to riding).
	 */
	public static float mealHeal(boolean tame, boolean meat, float health, float maxHealth, int nutrition) {
		if (!tame || !meat || health >= maxHealth) {
			return 0.0f;
		}
		return Math.min(maxHealth - health, Math.max(MIN_HEAL, nutrition));
	}
}
