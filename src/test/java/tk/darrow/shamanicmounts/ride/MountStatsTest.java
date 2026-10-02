package tk.darrow.shamanicmounts.ride;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountStatsTest {
	@Test
	void mixPullsTowardTheWeakerParentThenWobbles() {
		assertEquals(73, MountStats.mix(80, 60, 0, 0));
		assertEquals(87, MountStats.mix(80, 60, 8, 6));
		assertEquals(6, MountStats.mix(10, 10, -4, 0));
		assertEquals(100, MountStats.mix(100, 100, 8, 6));
		assertEquals(1, MountStats.mix(1, 1, -4, 0));
	}

	@Test
	void childOfEqualsStaysInATightBand() {
		MountStats parent = new MountStats(40, 40, 40, 40);
		MountStats first = MountStats.child(parent, parent, new Random(7));
		MountStats again = MountStats.child(parent, parent, new Random(7));
		assertEquals(first, again);
		assertInBand(first.health());
		assertInBand(first.speed());
		assertInBand(first.jump());
		assertInBand(first.stamina());
	}

	@Test
	void wildRollStaysInsideTheWildBand() {
		Random random = new Random(42);
		MountStats first = MountStats.wildRoll(new Random(42));
		for (int i = 0; i < 64; i++) {
			MountStats stats = i == 0 ? first : MountStats.wildRoll(random);
			assertBand(stats.health());
			assertBand(stats.speed());
			assertBand(stats.jump());
			assertBand(stats.stamina());
		}
		assertEquals(first, MountStats.wildRoll(new Random(42)));
	}

	@Test
	void missingStatsDefaultToTheMiddle() {
		assertEquals(38, MountStats.MISSING);
		assertEquals(new MountStats(38, 38, 38, 38), MountStats.missing());
		assertEquals(38, MountStats.read(false, 1));
		assertEquals(40, MountStats.read(true, 40));
		assertEquals(1, MountStats.read(true, -5));
		assertEquals(100, MountStats.read(true, 500));
	}

	private static void assertInBand(int stat) {
		assertTrue(stat >= 36 && stat <= 54, Integer.toString(stat));
	}

	private static void assertBand(int stat) {
		assertTrue(stat >= 28 && stat <= 48, Integer.toString(stat));
	}
}
