package tk.darrow.shamanicmounts.client;

import net.minecraft.util.Mth;

/**
 * How far one mount has settled into lying down and into the air. Both ease toward their targets once per game
 * tick, not once per frame, so a mount settles at the same pace at any frame rate; a frame draws between the
 * last two ticks' values.
 */
final class MountEase {
	/** Per tick, what the old per-frame rates (0.12 lying down, 0.15 in the air) came to at 60 frames a second. */
	static final float SIT_RATE = 0.32f;
	static final float AIR_RATE = 0.39f;
	/** A mount that went unseen for longer than this lands on its targets instead of replaying the ticks. */
	private static final int CATCH_UP = 20;

	private float sitO;
	private float sit;
	private float airO;
	private float air;
	private int tick;

	/** Starts settled, so a mount that comes into view already lying down does not stand up first. */
	MountEase(float sitTarget, float airTarget, int tick) {
		snap(sitTarget, airTarget);
		this.tick = tick;
	}

	/** Brings the ease up to game tick {@code now}, one step per tick that has passed since the last call. */
	void tick(int now, float sitTarget, float airTarget) {
		int steps = now - this.tick;
		this.tick = now;
		if (steps <= 0) {
			return;
		}
		if (steps > CATCH_UP) {
			snap(sitTarget, airTarget);
			return;
		}
		for (int i = 0; i < steps; i++) {
			this.sitO = this.sit;
			this.airO = this.air;
			this.sit += (sitTarget - this.sit) * SIT_RATE;
			this.air += (airTarget - this.air) * AIR_RATE;
		}
	}

	/** Jumps straight to the targets: for a mount that never ticks, like a book portrait. */
	void snap(float sitTarget, float airTarget) {
		this.sitO = this.sit = sitTarget;
		this.airO = this.air = airTarget;
	}

	float sit(float partialTick) {
		return Mth.lerp(partialTick, this.sitO, this.sit);
	}

	float air(float partialTick) {
		return Mth.lerp(partialTick, this.airO, this.air);
	}
}
