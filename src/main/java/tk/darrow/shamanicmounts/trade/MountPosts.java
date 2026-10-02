package tk.darrow.shamanicmounts.trade;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.block.MountTradingPostBlock;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.trade.MountTrade.Offer;

/**
 * The one desk every Mount Trading Post reads. Offers are not saved.
 * A swap or a gift removes the offers before anyone dismounts.
 */
@EventBusSubscriber(modid = ShamanicMounts.MOD_ID)
public final class MountPosts {
	private static final MountTrade DESK = new MountTrade();
	public static final double REACH = 8.0;

	private MountPosts() {
	}

	public static boolean dismounted(ServerPlayer player, UUID mount) {
		return DESK.dismount(player.getUUID(), mount);
	}

	/** The flute is taking this mount. Drop the offer and do not say the rider got off. */
	public static void recalled(UUID mountId) {
		DESK.recall(mountId);
	}

	public static void use(ServerPlayer player, BlockPos pos, boolean sneaking) {
		ServerLevel level = player.serverLevel();
		if (sneaking && DESK.has(player.getUUID())) {
			DESK.withdraw(player.getUUID());
			bar(player, "shamanicmounts.trade.withdrawn");
			return;
		}
		ShamanicMount mine = player.getVehicle() instanceof ShamanicMount mount ? mount : null;
		if (mine != null && mine.inReinTrial()) {
			bar(player, "shamanicmounts.trade.trial");
			return;
		}
		boolean own = mine != null && mine.isTame() && mine.isOwnedBy(player) && !mine.isBaby();
		if (own && !sneaking) {
			Live live = nextLive(level, player.getUUID(), false);
			Offer other = live.offer();
			if (other != null) {
				Held theirs = held(level, pos, other);
				if (theirs != null) {
					DESK.beginSwap(player.getUUID());
					dismount(player);
					dismount(theirs.player());
					mine.transferTo(theirs.player());
					theirs.mount().transferTo(player);
					chat(player, "shamanicmounts.trade.swapped", other.name());
					chat(theirs.player(), "shamanicmounts.trade.swapped", player.getName().getString());
					return;
				}
				bar(player, "shamanicmounts.trade.away");
			}
		}
		if (own) {
			long tick = level.getServer().overworld().getGameTime();
			DESK.put(new Offer(player.getUUID(), player.getName().getString(), mine.getUUID(), sneaking, tick));
			chat(player, sneaking ? "shamanicmounts.trade.offered_gift" : "shamanicmounts.trade.offered");
			return;
		}
		// A wild mount, someone else's mount, or a baby is not an offer and does not take a gift.
		if (mine != null) {
			bar(player, "shamanicmounts.trade.hint");
			return;
		}
		Live gifts = nextLive(level, player.getUUID(), true);
		Offer gift = gifts.offer();
		if (gift == null) {
			bar(player, gifts.dropped() ? "shamanicmounts.trade.gone" : "shamanicmounts.trade.hint");
			return;
		}
		Held given = held(level, pos, gift);
		if (given == null) {
			bar(player, "shamanicmounts.trade.away");
			return;
		}
		if (DESK.beginGift(player.getUUID()) == null) {
			bar(player, "shamanicmounts.trade.gone");
			return;
		}
		dismount(given.player());
		given.mount().transferTo(player);
		chat(player, "shamanicmounts.trade.received", gift.name());
		chat(given.player(), "shamanicmounts.trade.gave", player.getName().getString());
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		long now = event.getServer().overworld().getGameTime();
		if (now % 100 != 0) {
			return;
		}
		DESK.expire(now, id -> event.getServer().getPlayerList().getPlayer(id) != null);
	}

	/** Sneak with an item skips the block unless the block is forced. */
	@SubscribeEvent
	public static void forceSneakUse(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MountTradingPostBlock)) {
			return;
		}
		Player player = event.getEntity();
		boolean holding = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
		if (player.isSecondaryUseActive() && holding) {
			event.setUseBlock(TriState.TRUE);
			event.setUseItem(TriState.FALSE);
		}
	}

	private record Held(ServerPlayer player, ShamanicMount mount) {
	}

	private record Live(Offer offer, boolean dropped) {
	}

	/** Drops offline, dismounted, and trial offers. A rider who is not at this post stays on the desk. */
	private static Live nextLive(ServerLevel level, UUID player, boolean giftOnly) {
		boolean dropped = false;
		Offer offer = DESK.oldestOther(player, giftOnly);
		while (offer != null && stale(level, offer)) {
			DESK.drop(offer.player());
			dropped = true;
			offer = DESK.oldestOther(player, giftOnly);
		}
		return new Live(offer, dropped);
	}

	/** Offline, not riding that mount, or a mount that cannot be offered. */
	private static boolean stale(ServerLevel level, Offer offer) {
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(offer.player());
		if (owner == null) {
			return true;
		}
		if (!(owner.getVehicle() instanceof ShamanicMount mount) || !offer.mount().equals(mount.getUUID())) {
			return true;
		}
		return !mount.isTame() || !mount.isOwnedBy(owner) || mount.inReinTrial();
	}

	/** The offerer is online, still riding that mount, and within 8 blocks of this post. */
	private static Held held(ServerLevel level, BlockPos pos, Offer offer) {
		if (!(level.getEntity(offer.mount()) instanceof ShamanicMount mount)) {
			return null;
		}
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(offer.player());
		if (owner == null || owner.level() != level || owner.distanceToSqr(Vec3.atCenterOf(pos)) > REACH * REACH) {
			return null;
		}
		if (owner.getVehicle() != mount || !mount.isTame() || !mount.isOwnedBy(owner) || mount.inReinTrial()) {
			return null;
		}
		return new Held(owner, mount);
	}

	private static void dismount(ServerPlayer rider) {
		if (rider != null) {
			rider.stopRiding();
		}
	}

	private static void bar(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), true);
	}

	private static void chat(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), false);
	}
}
