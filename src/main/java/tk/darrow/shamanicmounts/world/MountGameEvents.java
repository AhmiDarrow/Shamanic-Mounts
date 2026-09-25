package tk.darrow.shamanicmounts.world;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.entity.ShamanicMount;

/** Nagual send-away and shade reveal, which happen to the player rather than the mount. */
@EventBusSubscriber(modid = ShamanicMounts.MOD_ID)
public final class MountGameEvents {
	private MountGameEvents() {
	}

	@SubscribeEvent
	public static void creepers(LivingChangeTargetEvent event) {
		if (event.getEntity() instanceof Creeper && event.getNewAboutToBeSetTarget() instanceof Player player
				&& player.getPersistentData().getInt("shamanicmounts_skin") > 0) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void hurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (player.getPersistentData().hasUUID("shamanicmounts_away") && player.level() instanceof ServerLevel level) {
			UUID id = player.getPersistentData().getUUID("shamanicmounts_away");
			ShamanicMount mount = ShamanicMount.loaded(level.getServer(), id);
			if (mount != null) {
				mount.returnNow();
			}
		}
		if (player.getVehicle() instanceof ShamanicMount mount) {
			mount.reveal();
		}
	}

	/**
	 * Vanilla steps a sneaking rider off at once. A mount that hides or blinks needs the rider to
	 * stay on while sneak is held, so that dismount is refused and the mount does its own tap check.
	 */
	@SubscribeEvent
	public static void dismount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
		if (event.isMounting() || !(event.getEntityBeingMounted() instanceof ShamanicMount mount)
				|| !(event.getEntityMounting() instanceof Player player)) {
			return;
		}
		if (mount.sneakKeepsOn(player) && (player.isShiftKeyDown() || mount.riderSneak()) && player.isAlive()
				&& mount.isAlive() && !mount.isRemoved() && !player.isSpectator()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void broke(BlockEvent.BreakEvent event) {
		if (event.getPlayer().getVehicle() instanceof ShamanicMount mount) {
			mount.reveal();
		}
	}

	@SubscribeEvent
	public static void tick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}
		int skin = player.getPersistentData().getInt("shamanicmounts_skin");
		if (skin > 0) {
			player.getPersistentData().putInt("shamanicmounts_skin", skin - 1);
		}
	}

	@SubscribeEvent
	public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		event.getEntity().getPersistentData().remove("shamanicmounts_skin");
	}
}
