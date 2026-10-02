package tk.darrow.shamanicmounts.trade;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * One shared book of mount offers. Oldest first. One offer per player.
 * A swap removes both offers before anyone dismounts, so getting off does not also cancel.
 */
public final class MountTrade {
	public record Offer(UUID player, String name, UUID mount, boolean gift, long tick) {
	}

	/** Five minutes. */
	public static final long EXPIRY_TICKS = 20L * 60L * 5L;

	private final Map<UUID, Offer> offers = new LinkedHashMap<>();

	public void put(Offer offer) {
		offers.remove(offer.player());
		offers.put(offer.player(), offer);
	}

	public boolean has(UUID player) {
		return offers.containsKey(player);
	}

	public Offer get(UUID player) {
		return offers.get(player);
	}

	/** Sneak while an offer is up. */
	public boolean withdraw(UUID player) {
		return offers.remove(player) != null;
	}

	public boolean drop(UUID player) {
		return offers.remove(player) != null;
	}

	/** Oldest offer that is not this player's. {@code giftOnly} keeps swaps and gifts apart. */
	public Offer oldestOther(UUID player, boolean giftOnly) {
		for (Offer offer : offers.values()) {
			if (offer.player().equals(player) || offer.gift() != giftOnly) {
				continue;
			}
			return offer;
		}
		return null;
	}

	/**
	 * The oldest other swap offer, removed together with this player's offer.
	 * Null when there is nothing to swap, and then nothing is removed.
	 */
	public Offer beginSwap(UUID player) {
		Offer other = oldestOther(player, false);
		if (other == null) {
			return null;
		}
		offers.remove(other.player());
		offers.remove(player);
		return other;
	}

	/** The oldest gift, removed. Null when there is no gift. */
	public Offer beginGift(UUID player) {
		Offer gift = oldestOther(player, true);
		if (gift == null) {
			return null;
		}
		offers.remove(gift.player());
		return gift;
	}

	/**
	 * True only when this player still had an offer of this mount, and it is now gone.
	 * A swap or a gift that already removed the offer returns false.
	 */
	public boolean dismount(UUID player, UUID mount) {
		Offer offer = offers.get(player);
		if (offer == null || mount == null || !mount.equals(offer.mount())) {
			return false;
		}
		offers.remove(player);
		return true;
	}

	/** Drop every offer of this mount. The flute does this before the mount is moved. */
	public boolean recall(UUID mount) {
		if (mount == null) {
			return false;
		}
		return offers.values().removeIf(offer -> mount.equals(offer.mount()));
	}

	/** Drop an offer whose owner is offline, or whose five minutes are up. */
	public void expire(long now, Predicate<UUID> online) {
		offers.values().removeIf(offer -> !online.test(offer.player()) || now - offer.tick() > EXPIRY_TICKS);
	}
}
