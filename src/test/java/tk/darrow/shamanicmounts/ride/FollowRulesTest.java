package tk.darrow.shamanicmounts.ride;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.entity.MountMode;

class FollowRulesTest {
	private static final int NOW = FollowRules.ORDERS_VERSION;

	@Test
	void aNewOrUnknownOrderIsFollow() {
		assertEquals(MountMode.FOLLOW, FollowRules.DEFAULT);
		assertEquals(MountMode.FOLLOW, FollowRules.loaded(null, 0, false, false));
		assertEquals(MountMode.FOLLOW, FollowRules.loaded("", NOW, false, true));
		assertEquals(MountMode.FOLLOW, FollowRules.loaded("GALLOP", NOW, false, false));
		// A save from before orders were kept, left sitting, was told to stay.
		assertEquals(MountMode.STAY, FollowRules.loaded(null, 0, true, false));
	}

	@Test
	void currentSavesKeepEveryOrder() {
		for (MountMode mode : MountMode.values()) {
			assertEquals(mode, FollowRules.loaded(mode.name(), NOW, mode == MountMode.STAY, false));
			assertEquals(mode, FollowRules.loaded(mode.name(), NOW, mode == MountMode.STAY, true));
		}
	}

	@Test
	void olderSavesMoveTheOldDefaultsToFollow() {
		// Wander was what taming gave; a guarding mount's stay was what stepping off gave.
		assertEquals(MountMode.FOLLOW, FollowRules.loaded("WANDER", 0, false, false));
		assertEquals(MountMode.FOLLOW, FollowRules.loaded("WANDER", 0, false, true));
		assertEquals(MountMode.FOLLOW, FollowRules.loaded("STAY", 0, true, true));
		// Any other mount only stayed when its owner said so.
		assertEquals(MountMode.STAY, FollowRules.loaded("STAY", 0, true, false));
		assertEquals(MountMode.FOLLOW, FollowRules.loaded("FOLLOW", 0, false, true));
	}

	@Test
	void onlyAFreeFollowingTameNearItsOwnerCrosses() {
		double near = 10 * 10;
		assertTrue(FollowRules.crosses(true, true, MountMode.FOLLOW, false, false, false, false, false, near));
		double edge = FollowRules.CROSSING_RANGE * FollowRules.CROSSING_RANGE;
		assertTrue(FollowRules.crosses(true, true, MountMode.FOLLOW, false, false, false, false, false, edge));
		assertFalse(FollowRules.crosses(true, true, MountMode.FOLLOW, false, false, false, false, false, edge + 1));
		assertFalse(FollowRules.crosses(false, true, MountMode.FOLLOW, false, false, false, false, false, near), "wild");
		assertFalse(FollowRules.crosses(true, false, MountMode.FOLLOW, false, false, false, false, false, near), "someone else's");
		assertFalse(FollowRules.crosses(true, true, MountMode.STAY, true, false, false, false, false, near), "staying");
		assertFalse(FollowRules.crosses(true, true, MountMode.WANDER, false, false, false, false, false, near), "wandering");
		assertFalse(FollowRules.crosses(true, true, MountMode.FOLLOW, true, false, false, false, false, near), "sitting");
		assertFalse(FollowRules.crosses(true, true, MountMode.FOLLOW, false, true, false, false, false, near), "leashed");
		assertFalse(FollowRules.crosses(true, true, MountMode.FOLLOW, false, false, true, false, false, near), "in a boat");
		assertFalse(FollowRules.crosses(true, true, MountMode.FOLLOW, false, false, false, true, false, near), "another rider");
		assertFalse(FollowRules.crosses(true, true, MountMode.FOLLOW, false, false, false, false, true, near), "sent away");
	}

	@Test
	void aRiderUnseatedByTheTeleportItselfWasRiding() {
		assertTrue(FollowRules.wasRiding(100, 100));
		assertTrue(FollowRules.wasRiding(100, 100 + FollowRules.RIDER_GRACE_TICKS));
		assertFalse(FollowRules.wasRiding(100, 101 + FollowRules.RIDER_GRACE_TICKS));
		assertFalse(FollowRules.wasRiding(-1, 0));
		assertFalse(FollowRules.wasRiding(100, 99));
	}
}
