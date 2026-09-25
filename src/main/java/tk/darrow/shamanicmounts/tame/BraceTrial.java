package tk.darrow.shamanicmounts.tame;

/**
 * Taming is four braced jolts, in place. The mount telegraphs, then bucks.
 * The rider has to be holding jump on that tick. Food is not involved.
 * A foal is born tame and skips this. It still needs the shamanic saddle to be ridden.
 */
public final class BraceTrial {
	public static final int JOLTS = 4;
	public static final int WINDUP_TICKS = 15;
	public static final int GAP_TICKS = 10;
	public static final int REFUSE_TICKS = 200;

	public enum Step {
		WINDUP, GAP
	}

	public static final class Trial {
		private int jolts;
		private int stepTick;
		private Step step = Step.WINDUP;
		private boolean failed;
		private boolean done;

		public int jolts() {
			return jolts;
		}

		public Step step() {
			return step;
		}

		public boolean failed() {
			return failed;
		}

		public boolean done() {
			return done;
		}

		public boolean finished() {
			return failed || done;
		}
	}

	private BraceTrial() {
	}

	public static Trial start() {
		return new Trial();
	}

	public static void tick(Trial trial, boolean jumpHeld, boolean dismounted, boolean hurtMount) {
		if (trial.finished()) {
			return;
		}
		if (dismounted || hurtMount) {
			trial.failed = true;
			return;
		}
		trial.stepTick++;
		if (trial.step == Step.WINDUP && trial.stepTick == WINDUP_TICKS) {
			if (!jumpHeld) {
				trial.failed = true;
				return;
			}
			trial.jolts++;
			if (trial.jolts == JOLTS) {
				trial.done = true;
				return;
			}
			trial.step = Step.GAP;
			trial.stepTick = 0;
			return;
		}
		if (trial.step == Step.GAP && trial.stepTick == GAP_TICKS) {
			trial.step = Step.WINDUP;
			trial.stepTick = 0;
		}
	}
}
