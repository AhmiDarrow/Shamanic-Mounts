package tk.darrow.shamanicmounts.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Lying down and taking off settle by the game tick, whatever the frame rate. */
class MountEaseTest {
	/** Draws {@code frames} frames per tick for {@code ticks} ticks, the way the renderer calls it. */
	private static float sitAfter(int ticks, int frames) {
		MountEase ease = new MountEase(0f, 0f, 0);
		for (int tick = 1; tick <= ticks; tick++) {
			for (int frame = 0; frame < frames; frame++) {
				ease.tick(tick, 1f, 0f);
				ease.sit((float) frame / frames);
			}
		}
		ease.tick(ticks, 1f, 0f);
		return ease.sit(1f);
	}

	@Test
	void settlesAtTheSamePaceAtAnyFrameRate() {
		assertEquals(sitAfter(6, 1), sitAfter(6, 3), 1e-6f);
		assertEquals(sitAfter(6, 1), sitAfter(6, 12), 1e-6f);
		float expected = 1f - (float) Math.pow(1f - MountEase.SIT_RATE, 6);
		assertEquals(expected, sitAfter(6, 4), 1e-5f);
	}

	@Test
	void framesDrawBetweenTheLastTwoTicks() {
		MountEase ease = new MountEase(0f, 0f, 10);
		ease.tick(11, 1f, 1f);
		assertEquals(0f, ease.sit(0f), 1e-6f);
		assertEquals(MountEase.SIT_RATE, ease.sit(1f), 1e-6f);
		assertEquals(MountEase.SIT_RATE * 0.5f, ease.sit(0.5f), 1e-6f);
		assertEquals(MountEase.AIR_RATE * 0.25f, ease.air(0.25f), 1e-6f);
		// A second frame in the same tick moves nothing.
		ease.tick(11, 1f, 1f);
		assertEquals(MountEase.SIT_RATE, ease.sit(1f), 1e-6f);
	}

	@Test
	void aLongGapLandsOnTheTarget() {
		MountEase ease = new MountEase(0f, 0f, 0);
		ease.tick(500, 1f, 0f);
		assertEquals(1f, ease.sit(0f), 1e-6f);
		assertEquals(0f, ease.air(1f), 1e-6f);
	}

	@Test
	void aMountFirstSeenLyingDownIsAlreadyDown() {
		MountEase ease = new MountEase(1f, 0f, 42);
		assertEquals(1f, ease.sit(0.3f), 1e-6f);
	}
}
