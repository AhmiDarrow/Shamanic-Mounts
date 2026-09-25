package tk.darrow.shamanicmounts.client;

import net.minecraft.util.Mth;

/** Everything the rig needs to know about one frame: gait, gaze, wings, and the small living tics. */
final class MountPose {
	/** Walk cycle position and how much of the stride is showing (0 standing, 1 full stride). */
	final float swing;
	final float amount;
	final float age;
	/** Head turn relative to the body, and the pitch of the gaze. */
	final float yaw;
	final float pitch;
	/** Wing open is 0 folded and 1 in the air; wingPose is 0 perched, 1 gliding, 2 flapping. */
	final float open;
	final int wingPose;
	/** 0 standing to 1 sitting. */
	final float sit;
	/** 0 on the ground to 1 in the air. */
	final float air;
	/** Vertical speed in blocks per tick while airborne. */
	final float climb;
	final boolean swim;
	final boolean baby;
	final boolean blink;
	/** 0 to 1 ear twitch. */
	final float ear;
	/** Breathing, -1 to 1. */
	final float breath;
	final boolean glow;

	MountPose(float swing, float amount, float age, float yaw, float pitch, float open, int wingPose, float sit, float air,
			float climb, boolean swim, boolean baby, int seed, boolean glow) {
		this.swing = swing;
		this.amount = amount;
		this.age = age;
		this.yaw = yaw;
		this.pitch = pitch;
		this.open = open;
		this.wingPose = wingPose;
		this.sit = sit;
		this.air = air;
		this.climb = climb;
		this.swim = swim;
		this.baby = baby;
		this.glow = glow;
		int tick = (int) age + seed * 37;
		int blinkPhase = Math.floorMod(tick, 96 + (seed & 31));
		// A blink needs a living clock; a mount that has never ticked, like a book portrait, keeps its eyes open.
		this.blink = age > 20f && blinkPhase < 4;
		int earPhase = Math.floorMod(tick + seed * 11, 150 + (seed & 63));
		this.ear = earPhase < 10 ? Mth.sin(earPhase / 10.0f * (float) Math.PI) : 0.0f;
		this.breath = Mth.sin(age * 0.06f);
	}

	/** Leg swing angle in degrees for a leg at {@code phase} radians into the cycle. */
	float stride(float phase, float reach) {
		return Mth.cos(swing * 0.6662f + phase) * reach * amount;
	}

	/** How lifted a leg is, 0 planted to 1 at the top of its swing forward. */
	float lift(float phase) {
		return Math.max(0.0f, -Mth.sin(swing * 0.6662f + phase)) * amount;
	}

	/** Idle head drift when standing still. */
	float idleYaw() {
		return amount < 0.15f ? Mth.sin(age * 0.045f) * 4.0f : 0.0f;
	}
}
