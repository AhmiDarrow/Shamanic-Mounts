package tk.darrow.shamanicmounts.tame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Taming, once the rider is in the saddle. Thirty seconds, fifteen prompts, two seconds each.
 * The rider presses the shown direction with the movement keys or the arrow keys. A tap at any
 * point in the two seconds counts, and another key held at the same time does not spoil it.
 * Four missed prompts are allowed. A fifth miss, or getting off, fails the try.
 * The mount steps the opposite way for the whole prompt: left asks for a step right, forward for a step back.
 */
public final class ReinTrial {
	public static final int BEATS = 15;
	public static final int BEAT_TICKS = 40;
	public static final int DURATION_TICKS = BEATS * BEAT_TICKS;
	public static final int MISSES_ALLOWED = 4;
	/** How hard the mount steps, in the same units as a rider's steer input. */
	public static final float LUNGE_STRAFE = 0.32f;
	public static final float LUNGE_FORWARD = 0.40f;
	/** The hop at the first tick of a prompt. A buck, not a launch. */
	public static final double BUCK_HOP = 0.48;
	public static final int REFUSE_TICKS = 200;

	public static final int PRESS_LEFT = 1;
	public static final int PRESS_RIGHT = 2;
	public static final int PRESS_FORWARD = 4;
	public static final int PRESS_BACK = 8;

	public enum Dir {
		LEFT, RIGHT, FORWARD, BACK;

		public String key() {
			return switch (this) {
				case LEFT -> "tame.shamanicmounts.left";
				case RIGHT -> "tame.shamanicmounts.right";
				case FORWARD -> "tame.shamanicmounts.forward";
				case BACK -> "tame.shamanicmounts.back";
			};
		}

		/** 1 left, 2 right, 3 forward, 4 back. 0 is no prompt. */
		public byte code() {
			return (byte) (ordinal() + 1);
		}

		/** Strafe input that steps the opposite way. Positive is left. */
		public float strafe() {
			return switch (this) {
				case LEFT -> -LUNGE_STRAFE;
				case RIGHT -> LUNGE_STRAFE;
				default -> 0.0f;
			};
		}

		/** Forward input that steps the opposite way. Positive is forward. */
		public float forward() {
			return switch (this) {
				case FORWARD -> -LUNGE_FORWARD;
				case BACK -> LUNGE_FORWARD;
				default -> 0.0f;
			};
		}

		public static Dir fromCode(byte code) {
			int index = (code & 255) - 1;
			Dir[] values = values();
			return index >= 0 && index < values.length ? values[index] : null;
		}
	}

	/** The opposite step is a kick at the start of the prompt, then it eases. Still the other way. */
	public static float buck(int ticksIntoBeat) {
		if (ticksIntoBeat < 8) {
			return 1.75f;
		}
		if (ticksIntoBeat < 18) {
			return 0.85f;
		}
		return 0.35f;
	}

	/** The mount leaves the ground on the first tick of a prompt. */
	public static boolean hops(int ticksIntoBeat) {
		return ticksIntoBeat == 0;
	}

	/** Which of the four directions are down this tick. */
	public record Press(boolean left, boolean right, boolean forward, boolean back) {
		public static Press of(int mask) {
			return new Press((mask & PRESS_LEFT) != 0, (mask & PRESS_RIGHT) != 0, (mask & PRESS_FORWARD) != 0,
					(mask & PRESS_BACK) != 0);
		}

		public boolean matches(Dir dir) {
			return switch (dir) {
				case LEFT -> left;
				case RIGHT -> right;
				case FORWARD -> forward;
				case BACK -> back;
			};
		}
	}

	public static final class Trial {
		private final Dir[] cues;
		private int tick;
		private int misses;
		private boolean caught;
		private boolean failed;
		private boolean done;
		private boolean caughtBeat;
		private boolean missedBeat;

		private Trial(Dir[] cues) {
			this.cues = cues;
		}

		public Dir cue() {
			return cues[Math.min(tick / BEAT_TICKS, BEATS - 1)];
		}

		public Dir cueAt(int beat) {
			return cues[beat];
		}

		public int misses() {
			return misses;
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

		/** The prompt that just ended was answered. Cleared on the next tick. */
		public boolean caughtBeat() {
			return caughtBeat;
		}

		/** The prompt that just ended was missed. Cleared on the next tick. */
		public boolean missedBeat() {
			return missedBeat;
		}
	}

	private ReinTrial() {
	}

	public static Trial start(long seed) {
		Random random = new Random(seed);
		Dir[] cues = new Dir[BEATS];
		List<Dir> opening = new ArrayList<>(EnumSet.allOf(Dir.class));
		Collections.shuffle(opening, random);
		for (int beat = 0; beat < opening.size(); beat++) {
			cues[beat] = opening.get(beat);
		}
		Dir last = cues[opening.size() - 1];
		Dir[] values = Dir.values();
		for (int beat = opening.size(); beat < BEATS; beat++) {
			Dir next;
			do {
				next = values[random.nextInt(values.length)];
			} while (next == last);
			cues[beat] = next;
			last = next;
		}
		return new Trial(cues);
	}

	public static void tick(Trial trial, Press press, boolean dismounted) {
		trial.caughtBeat = false;
		trial.missedBeat = false;
		if (trial.finished()) {
			return;
		}
		if (dismounted) {
			trial.failed = true;
			return;
		}
		if (press.matches(trial.cue())) {
			trial.caught = true;
		}
		trial.tick++;
		if (trial.tick % BEAT_TICKS != 0) {
			return;
		}
		if (!trial.caught) {
			trial.misses++;
			trial.missedBeat = true;
			if (trial.misses > MISSES_ALLOWED) {
				trial.failed = true;
			}
		} else {
			trial.caughtBeat = true;
		}
		trial.caught = false;
		if (!trial.failed && trial.tick >= DURATION_TICKS) {
			trial.done = true;
		}
	}
}
