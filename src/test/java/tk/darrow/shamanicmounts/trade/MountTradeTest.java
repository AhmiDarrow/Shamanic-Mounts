package tk.darrow.shamanicmounts.trade;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.trade.MountTrade.Offer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountTradeTest {
	@Test
	void dismountCancelsTheOfferOfThatMount() {
		MountTrade desk = new MountTrade();
		UUID player = UUID.randomUUID();
		UUID mount = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		desk.put(offer(player, "Ada", mount, false, 1));
		assertFalse(desk.dismount(player, other));
		assertTrue(desk.has(player));
		assertTrue(desk.dismount(player, mount));
		assertFalse(desk.has(player));
		assertFalse(desk.dismount(player, mount));
	}

	@Test
	void swapTakesTheOldestOtherSwapAndRemovesBothFirst() {
		MountTrade desk = new MountTrade();
		UUID oldest = UUID.randomUUID();
		UUID gift = UUID.randomUUID();
		UUID newer = UUID.randomUUID();
		UUID rider = UUID.randomUUID();
		UUID oldestMount = UUID.randomUUID();
		UUID riderMount = UUID.randomUUID();
		desk.put(offer(oldest, "Oldest", oldestMount, false, 1));
		desk.put(offer(gift, "Gift", UUID.randomUUID(), true, 2));
		desk.put(offer(newer, "Newer", UUID.randomUUID(), false, 3));
		desk.put(offer(rider, "Rider", riderMount, false, 4));
		Offer taken = desk.beginSwap(rider);
		assertEquals(oldest, taken.player());
		assertFalse(desk.has(oldest));
		assertFalse(desk.has(rider));
		assertTrue(desk.has(newer));
		assertTrue(desk.has(gift));
		assertFalse(desk.dismount(oldest, oldestMount));
		assertFalse(desk.dismount(rider, riderMount));
	}

	@Test
	void aGiftIsNotConsumedByAMountedSwap() {
		MountTrade desk = new MountTrade();
		UUID gift = UUID.randomUUID();
		UUID rider = UUID.randomUUID();
		desk.put(offer(gift, "Gift", UUID.randomUUID(), true, 1));
		assertNull(desk.beginSwap(rider));
		assertTrue(desk.has(gift));
	}

	@Test
	void aFootRequestConsumesTheOldestGiftOnly() {
		MountTrade desk = new MountTrade();
		UUID older = UUID.randomUUID();
		UUID newer = UUID.randomUUID();
		UUID swap = UUID.randomUUID();
		UUID walker = UUID.randomUUID();
		desk.put(offer(older, "Older", UUID.randomUUID(), true, 1));
		desk.put(offer(swap, "Swap", UUID.randomUUID(), false, 2));
		desk.put(offer(newer, "Newer", UUID.randomUUID(), true, 3));
		Offer taken = desk.beginGift(walker);
		assertEquals(older, taken.player());
		assertFalse(desk.has(older));
		assertTrue(desk.has(newer));
		assertTrue(desk.has(swap));
	}

	@Test
	void sneakWithdraws() {
		MountTrade desk = new MountTrade();
		UUID player = UUID.randomUUID();
		desk.put(offer(player, "Ada", UUID.randomUUID(), true, 1));
		assertTrue(desk.withdraw(player));
		assertFalse(desk.has(player));
		assertFalse(desk.withdraw(player));
	}

	@Test
	void expiryDropsOldOffersAndOfflineOwners() {
		MountTrade desk = new MountTrade();
		UUID old = UUID.randomUUID();
		UUID fresh = UUID.randomUUID();
		UUID away = UUID.randomUUID();
		desk.put(offer(old, "Old", UUID.randomUUID(), false, 0));
		desk.put(offer(fresh, "Fresh", UUID.randomUUID(), false, 1000));
		desk.expire(MountTrade.EXPIRY_TICKS, id -> true);
		assertTrue(desk.has(old));
		desk.expire(MountTrade.EXPIRY_TICKS + 1, id -> true);
		assertFalse(desk.has(old));
		assertTrue(desk.has(fresh));
		desk.put(offer(away, "Away", UUID.randomUUID(), false, MountTrade.EXPIRY_TICKS));
		desk.expire(MountTrade.EXPIRY_TICKS, id -> false);
		assertFalse(desk.has(fresh));
		assertFalse(desk.has(away));
	}

	@Test
	void recallDropsEveryOfferOfThatMount() {
		MountTrade desk = new MountTrade();
		UUID mount = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		UUID ada = UUID.randomUUID();
		UUID bo = UUID.randomUUID();
		desk.put(offer(ada, "Ada", mount, false, 1));
		desk.put(offer(bo, "Bo", other, true, 2));
		assertFalse(desk.recall(null));
		assertTrue(desk.recall(mount), "the offer of that mount drops");
		assertFalse(desk.has(ada));
		assertTrue(desk.has(bo));
		assertFalse(desk.recall(mount));
	}

	@Test
	void dismountAfterTheOfferWasRemovedIsNotACancel() {
		MountTrade desk = new MountTrade();
		UUID player = UUID.randomUUID();
		UUID mount = UUID.randomUUID();
		desk.put(offer(player, "Ada", mount, false, 1));
		assertTrue(desk.withdraw(player));
		assertFalse(desk.dismount(player, mount));
	}

	private static Offer offer(UUID player, String name, UUID mount, boolean gift, long tick) {
		return new Offer(player, name, mount, gift, tick);
	}
}
