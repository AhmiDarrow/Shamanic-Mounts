package tk.darrow.shamanicmounts.tack;

/**
 * Every mount, including a foal and the nagual, is ridden only with a shamanic saddle on.
 * A vanilla saddle does not fit. A golden apple does not tame: it only makes a wild adult stop
 * attacking long enough for the saddle to go on. The rein trial starts once the rider is seated.
 */
public final class SaddleRules {
	public static final String ITEM_ID = "shamanic_saddle";
	/** Two minutes. A wild adult does not attack, and will take a saddle, while this lasts. */
	public static final int CALM_TICKS = 20 * 120;

	private SaddleRules() {
	}

	public static boolean canRide(boolean tamed, boolean shamanicSaddleOn) {
		return tamed && shamanicSaddleOn;
	}

	public static boolean vanillaSaddleFits() {
		return false;
	}

	/** Food never starts the trial. A golden apple only calms. */
	public static boolean foodStartsTrial() {
		return false;
	}

	/** A wild adult with no calm left and nobody in the middle of taming it. Foals are not hostile. */
	public static boolean hostile(boolean tame, boolean baby, int calmTicks, boolean inTrial) {
		return !tame && !baby && calmTicks <= 0 && !inTrial;
	}

	/** A golden apple, on a wild adult, while a trial is not already running. */
	public static boolean canCalm(boolean wildAdult, boolean goldenApple, boolean inTrial) {
		return wildAdult && goldenApple && !inTrial;
	}

	/** The saddle goes on only during the calm, and only when one is not already there. */
	public static boolean canPlaceSaddle(boolean wildAdult, boolean calm, boolean shamanicSaddle, boolean alreadySaddled,
			boolean inTrial) {
		return wildAdult && calm && shamanicSaddle && !alreadySaddled && !inTrial;
	}

	/** Getting on starts the trial. The calm has to be holding, and the saddle already on. */
	public static boolean canMount(boolean wildAdult, boolean calm, boolean saddled, int refuseTicks, boolean inTrial,
			boolean alreadyRiding) {
		return wildAdult && calm && saddled && refuseTicks <= 0 && !inTrial && !alreadyRiding;
	}
}
