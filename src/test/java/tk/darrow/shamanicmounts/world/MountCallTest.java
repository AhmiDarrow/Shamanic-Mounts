package tk.darrow.shamanicmounts.world;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.entity.MountMode;
import tk.darrow.shamanicmounts.world.MountCall.Choice;
import tk.darrow.shamanicmounts.world.MountCall.Reason;
import tk.darrow.shamanicmounts.world.MountCall.Sight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountCallTest {
	private static final double FAR = 20.0 * 20.0;

	@Test
	void followStayAndWanderAreEligible() {
		UUID owner = UUID.randomUUID();
		Sight follow = seen(owner, MountMode.FOLLOW);
		Sight stay = seen(owner, MountMode.STAY);
		Sight wander = seen(owner, MountMode.WANDER);
		assertEquals(Reason.CALL, MountCall.judge(follow, owner));
		assertEquals(Reason.CALL, MountCall.judge(stay, owner));
		assertEquals(Reason.CALL, MountCall.judge(wander, owner));
		Choice choice = MountCall.choose(owner, List.of(follow, stay, wander));
		assertEquals(List.of(follow.id(), stay.id(), wander.id()), choice.call());
		assertFalse(choice.capped());
	}

	@Test
	void reinTrialLeashOtherRiderWrongOwnerAndReleasedAreRejected() {
		UUID owner = UUID.randomUUID();
		Sight trial = copy(seen(owner, MountMode.FOLLOW), true, false, false, false, false, true, true, 0.0, true, false, true, false);
		Sight leash = copy(seen(owner, MountMode.STAY), false, true, false, false, false, true, true, 0.0, true, false, true, false);
		Sight busy = copy(seen(owner, MountMode.WANDER), false, false, true, false, false, true, true, 0.0, true, false, true, false);
		Sight dead = copy(seen(owner, MountMode.FOLLOW), false, false, false, true, false, true, true, 0.0, true, false, true, false);
		Sight stranger = copy(seen(UUID.randomUUID(), MountMode.FOLLOW), false, false, false, false, false, true, true, FAR, true, false, true, false);
		Sight released = copy(seen(owner, MountMode.FOLLOW), false, false, false, false, false, true, true, FAR, true, false, false, false);
		assertEquals(Reason.TRIAL, MountCall.judge(trial, owner), "a rein trial is not called");
		assertEquals(Reason.SKIP, MountCall.judge(leash, owner), "a leashed mount is not called");
		assertEquals(Reason.BUSY, MountCall.judge(busy, owner), "someone else is riding");
		assertEquals(Reason.SKIP, MountCall.judge(dead, owner), "a dead mount is not called");
		assertEquals(Reason.SKIP, MountCall.judge(stranger, owner), "another player's mount stays");
		assertEquals(Reason.SKIP, MountCall.judge(released, owner), "a released mount is not called");
		Choice choice = MountCall.choose(owner, List.of(trial, leash, busy, dead, stranger, released));
		assertTrue(choice.call().isEmpty(), "none of the rejected mounts are called");
		assertTrue(choice.trial(), "the trial is reported");
		assertTrue(choice.busy(), "the ridden mount is reported");
	}

	@Test
	void anAwayMountIsEligible() {
		UUID owner = UUID.randomUUID();
		Sight away = copy(seen(owner, MountMode.STAY), false, false, false, false, true, true, true, 0.0, true, false, true, false);
		assertEquals(Reason.CALL, MountCall.judge(away, owner), "an away mount still comes");
	}

	@Test
	void withinSixteenSameDimensionIsHereUnlessAway() {
		UUID owner = UUID.randomUUID();
		Sight here = copy(seen(owner, MountMode.FOLLOW), false, false, false, false, false, true, true, MountCall.HERE_DISTANCE_SQR, true, false, true, false);
		Sight step = copy(seen(owner, MountMode.FOLLOW), false, false, false, false, false, true, true, MountCall.HERE_DISTANCE_SQR + 1.0, true, false, true, false);
		Sight away = copy(seen(owner, MountMode.FOLLOW), false, false, false, false, true, true, true, 0.0, true, false, true, false);
		assertEquals(Reason.HERE, MountCall.judge(here, owner), "sixteen blocks is already here");
		assertEquals(Reason.CALL, MountCall.judge(step, owner), "past sixteen blocks is called");
		assertEquals(Reason.CALL, MountCall.judge(away, owner), "an away mount snaps even when it is close");
		Choice choice = MountCall.choose(owner, List.of(here));
		assertTrue(choice.here(), "the near mount is already with you");
		assertTrue(choice.call().isEmpty(), "a near mount is not summoned");
	}

	@Test
	void nineIdsReturnEightAndTheRestAreCapped() {
		UUID owner = UUID.randomUUID();
		List<Sight> herd = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			herd.add(seen(owner, MountMode.FOLLOW));
		}
		Choice choice = MountCall.choose(owner, herd);
		assertEquals(MountCall.CAP, choice.call().size(), "eight mounts");
		assertEquals(8, choice.call().size(), "the cap is eight");
		List<UUID> first = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			first.add(herd.get(i).id());
		}
		assertEquals(first, choice.call());
		assertFalse(choice.call().contains(herd.get(8).id()), "the ninth stays out");
		assertTrue(choice.capped(), "more than eight were waiting");
	}

	@Test
	void farSameDimensionAndOtherDimensionAreCalled() {
		UUID owner = UUID.randomUUID();
		Sight far = copy(seen(owner, MountMode.WANDER), false, false, false, false, false, true, true, FAR, true, false, true, false);
		Sight other = copy(seen(owner, MountMode.STAY), false, false, false, false, false, true, false, 0.0, true, false, true, false);
		assertEquals(Reason.CALL, MountCall.judge(far, owner), "a far mount in this dimension is called");
		assertEquals(Reason.CALL, MountCall.judge(other, owner), "another dimension is called");
		Choice choice = MountCall.choose(owner, List.of(far, other));
		assertEquals(List.of(far.id(), other.id()), choice.call());
	}

	@Test
	void aTameOwnedBabyIsCalled() {
		UUID owner = UUID.randomUUID();
		Sight baby = copy(seen(owner, MountMode.FOLLOW), false, false, false, false, false, true, true, FAR, true, false, true, true);
		assertEquals(Reason.CALL, MountCall.judge(baby, owner), "a tame foal is called");
		assertTrue(MountCall.choose(owner, List.of(baby)).call().contains(baby.id()), "the foal is in the call");
	}

	@Test
	void anUnloadedMountWithNoWhereaboutsIsLost() {
		UUID owner = UUID.randomUUID();
		Sight lost = copy(seen(owner, MountMode.FOLLOW), false, false, false, false, false, false, false, 0.0, false, false, true, false);
		assertEquals(Reason.LOST, MountCall.judge(lost, owner), "no recorded place and not loaded");
		assertTrue(MountCall.choose(owner, List.of(lost)).lost(), "the flute says it is too far to hear");
	}

	@Test
	void anUnloadedMountWithWhereaboutsIsCalled() {
		UUID owner = UUID.randomUUID();
		Sight known = copy(seen(owner, MountMode.STAY), false, false, false, false, false, false, false, 0.0, true, false, true, false);
		assertEquals(Reason.CALL, MountCall.judge(known, owner), "a remembered chunk can be loaded");
	}

	@Test
	void theMountYouAreRidingStays() {
		UUID owner = UUID.randomUUID();
		Sight ridden = copy(seen(owner, MountMode.FOLLOW), false, false, true, false, false, true, false, FAR, true, true, true, false);
		assertEquals(Reason.HERE, MountCall.judge(ridden, owner), "the mount under you stays");
		Choice choice = MountCall.choose(owner, List.of(ridden, seen(owner, MountMode.WANDER)));
		assertEquals(1, choice.call().size(), "the others still come");
		assertFalse(choice.call().contains(ridden.id()), "you are not dismounted");
		assertTrue(choice.here(), "that mount is already with you");
	}

	@Test
	void rejectedMountsDoNotCountTowardTheCap() {
		UUID owner = UUID.randomUUID();
		List<Sight> herd = new ArrayList<>();
		herd.add(copy(seen(owner, MountMode.FOLLOW), true, false, false, false, false, true, true, 0.0, true, false, true, false));
		herd.add(copy(seen(owner, MountMode.FOLLOW), false, true, false, false, false, true, true, 0.0, true, false, true, false));
		herd.add(copy(seen(UUID.randomUUID(), MountMode.FOLLOW), false, false, false, false, false, true, true, FAR, true, false, true, false));
		for (int i = 0; i < 9; i++) {
			herd.add(seen(owner, MountMode.WANDER));
		}
		Choice choice = MountCall.choose(owner, herd);
		assertEquals(8, choice.call().size(), "rejects leave room under the cap");
		assertEquals(herd.get(3).id(), choice.call().get(0));
		assertFalse(choice.call().contains(herd.get(11).id()), "the ninth call stays out");
		assertTrue(choice.capped(), "nine calls are still capped");
	}

	private static Sight seen(UUID owner, MountMode mode) {
		return new Sight(UUID.randomUUID(), owner, true, mode, false, false, false, false, false, true, true, FAR, true, false, false);
	}

	private static Sight copy(Sight sight, boolean trial, boolean leashed, boolean otherRider, boolean dead, boolean away,
			boolean loaded, boolean sameDimension, double distanceSq, boolean known, boolean riddenByCaller, boolean tame,
			boolean baby) {
		return new Sight(sight.id(), sight.owner(), tame, sight.mode(), trial, leashed, otherRider, dead, away, loaded,
				sameDimension, distanceSq, known, riddenByCaller, baby);
	}
}
