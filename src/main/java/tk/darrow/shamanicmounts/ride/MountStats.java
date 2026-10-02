package tk.darrow.shamanicmounts.ride;

import java.util.Random;

/**
 * Health, speed, jump, and stamina on the animal. Numbers, not genes.
 * A foal starts from the stronger parent, is pulled toward the weaker one, then wobbles.
 * One time in eight it also sparks, so a line can be bred up by a small step.
 */
public record MountStats(int health, int speed, int jump, int stamina) {
	/** Middle of the wild band. An old save with no stats reads back as this, once. */
	public static final int MISSING = 38;
	public static final int MIN = 1;
	public static final int MAX = 100;

	public MountStats {
		health = clamp(health);
		speed = clamp(speed);
		jump = clamp(jump);
		stamina = clamp(stamina);
	}

	public static MountStats missing() {
		return new MountStats(MISSING, MISSING, MISSING, MISSING);
	}

	/** A stat tag that was not saved. Present values are clamped. */
	public static int read(boolean present, int value) {
		return present ? clamp(value) : MISSING;
	}

	public static int clamp(int stat) {
		return Math.max(MIN, Math.min(MAX, stat));
	}

	/** Each stat is 28..48, from the caller's random. */
	public static MountStats wildRoll(Random random) {
		return new MountStats(wildOne(random), wildOne(random), wildOne(random), wildOne(random));
	}

	private static int wildOne(Random random) {
		return 28 + random.nextInt(21);
	}

	/** Each stat is mixed on its own. */
	public static MountStats child(MountStats parentA, MountStats parentB, Random random) {
		return new MountStats(
				mixOne(parentA.health, parentB.health, random),
				mixOne(parentA.speed, parentB.speed, random),
				mixOne(parentA.jump, parentB.jump, random),
				mixOne(parentA.stamina, parentB.stamina, random));
	}

	private static int mixOne(int a, int b, Random random) {
		int better = Math.max(a, b);
		int worse = Math.min(a, b);
		int wobble = random.nextInt(13) - 4;
		int spark = random.nextInt(8) == 0 ? random.nextInt(6) + 1 : 0;
		return mix(better, worse, wobble, spark);
	}

	/**
	 * 0.65 of the better parent and 0.35 of the worse, then wobble and spark.
	 * Wobble is -4..+8. Spark is 0, or +1..+6 when the caller already rolled one.
	 */
	static int mix(int better, int worse, int wobble, int spark) {
		int blended = (better * 65 + worse * 35) / 100;
		return clamp(blended + wobble + spark);
	}

	/**
	 * How far a stat moves its number: exactly 1 at the wild middle, so an unbred mount (and an old save,
	 * which reads back as the middle) rides as it did before stats; about 0.70 at 1 and 1.50 at 100.
	 */
	public static double factor(int stat) {
		return 1.0 + (clamp(stat) - MISSING) * 0.008;
	}

	/** The body's own health (build and size genes) scaled by the bred stat. */
	public static double maxHealth(double bodyHealth, int healthStat) {
		return Math.max(1.0, Math.round(bodyHealth * factor(healthStat)));
	}

	/** 0.25 at the middle, the speed every mount had before stats; about 0.18 at 1 and 0.36 at 100. */
	public static double moveSpeed(int speedStat) {
		return 0.25 + (clamp(speedStat) - MISSING) * 0.0018;
	}

	/** Walked speed plus 40% of the gap up to 0.42. Never above 0.42. */
	public static double gallopSpeed(double walked) {
		if (walked >= 0.42) {
			return 0.42;
		}
		return walked + 0.4 * (0.42 - walked);
	}

	/** The mount's 0.55 hop scaled by the stat: 0.55 at the middle, about 0.39 at 1 and 0.82 at 100. */
	public static double jumpImpulse(int jumpStat) {
		return 0.55 * factor(jumpStat);
	}

	/** About 2 seconds at 1 and 12 seconds at 100, in ticks. */
	public static int capacityTicks(int staminaStat) {
		return 40 + clamp(staminaStat) * 2;
	}

	/** Gallop stays off after empty until the bar has regenned to this. */
	public static int gallopFloor(int staminaStat) {
		return capacityTicks(staminaStat) * 20 / 100;
	}
}
