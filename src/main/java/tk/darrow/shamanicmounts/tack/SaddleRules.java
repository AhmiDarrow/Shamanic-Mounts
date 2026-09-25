package tk.darrow.shamanicmounts.tack;

/**
 * Every mount, including a foal and the nagual, is ridden only with a shamanic saddle on.
 * A vanilla saddle does not fit. Food does not start a tame.
 */
public final class SaddleRules {
	public static final String ITEM_ID = "shamanic_saddle";

	private SaddleRules() {
	}

	public static boolean canRide(boolean tamed, boolean shamanicSaddleOn) {
		return tamed && shamanicSaddleOn;
	}

	public static boolean vanillaSaddleFits() {
		return false;
	}

	public static boolean foodStartsTrial() {
		return false;
	}

	/** Wild mount, shamanic saddle in hand, and the refuse timer has run out. */
	public static boolean canOffer(boolean wild, boolean shamanicSaddleInHand, int refuseTicksLeft) {
		return wild && shamanicSaddleInHand && refuseTicksLeft <= 0;
	}
}
