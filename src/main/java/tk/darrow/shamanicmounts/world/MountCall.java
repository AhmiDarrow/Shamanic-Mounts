package tk.darrow.shamanicmounts.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.entity.MountHerdData;
import tk.darrow.shamanicmounts.entity.MountMode;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.trade.MountPosts;

/**
 * One blow of the mount flute. Tame mounts the player owns come from any dimension, including a chunk
 * that is not loaded, and are set to follow so a stay mount does not walk home.
 */
@EventBusSubscriber(modid = ShamanicMounts.MOD_ID)
public final class MountCall {
	public static final int CAP = 8;
	public static final int HERE_BLOCKS = 16;
	public static final double HERE_DISTANCE_SQR = 16.0 * 16.0;
	public static final int PENDING_TICKS = 100;
	public static final int COOLDOWN = 60;

	public enum Reason {
		CALL, HERE, TRIAL, BUSY, LOST, SKIP
	}

	/**
	 * One herd-book mount as the flute sees it. No world is touched here, so the rules can be tested
	 * without a level.
	 */
	public record Sight(UUID id, UUID owner, boolean tame, @Nullable MountMode mode, boolean trial, boolean leashed,
			boolean otherRider, boolean dead, boolean away, boolean loaded, boolean sameDimension, double distanceSq,
			boolean known, boolean riddenByCaller, boolean baby) {
	}

	public record Choice(List<UUID> call, boolean capped, boolean here, boolean trial, boolean busy, boolean lost) {
	}

	private enum Outcome {
		CAME, PENDING, ROOM, NO, FAIL
	}

	private record Attempt(Outcome outcome, @Nullable Component name, @Nullable Reason reason) {
		static Attempt of(Outcome outcome) {
			return new Attempt(outcome, null, null);
		}

		static Attempt skip(Reason reason) {
			return new Attempt(Outcome.NO, null, reason);
		}
	}

	private record Wait(UUID owner, long expiry) {
	}

	private static final class Wave {
		int left;
		int arrived;
		boolean seen;
		boolean brought;
	}

	private static final Map<UUID, Wait> PENDING = new HashMap<>();
	private static final Map<UUID, Wave> WAVES = new HashMap<>();
	private static final Set<UUID> SUMMONING = new HashSet<>();

	private MountCall() {
	}

	public static Reason judge(Sight sight, UUID player) {
		if (sight == null || sight.id() == null || player == null || sight.owner() == null || !player.equals(sight.owner())
				|| !sight.tame() || sight.mode() == null) {
			return Reason.SKIP;
		}
		if (sight.dead()) {
			return Reason.SKIP;
		}
		if (sight.trial()) {
			return Reason.TRIAL;
		}
		if (sight.leashed()) {
			return Reason.SKIP;
		}
		if (sight.riddenByCaller()) {
			return Reason.HERE;
		}
		if (sight.otherRider()) {
			return Reason.BUSY;
		}
		if (!sight.loaded()) {
			return sight.known() ? Reason.CALL : Reason.LOST;
		}
		if (sight.away()) {
			return Reason.CALL;
		}
		if (sight.sameDimension() && sight.distanceSq() <= HERE_DISTANCE_SQR) {
			return Reason.HERE;
		}
		return Reason.CALL;
	}

	/** Herd-book order. The first eight that qualify are called; a ninth sets {@code capped}. */
	public static Choice choose(UUID player, List<Sight> herdOrder) {
		List<UUID> call = new ArrayList<>();
		boolean capped = false;
		boolean here = false;
		boolean trial = false;
		boolean busy = false;
		boolean lost = false;
		if (herdOrder != null) {
			for (Sight sight : herdOrder) {
				switch (judge(sight, player)) {
					case CALL -> {
						if (call.size() == CAP) {
							capped = true;
						} else {
							call.add(sight.id());
						}
					}
					case HERE -> here = true;
					case TRIAL -> trial = true;
					case BUSY -> busy = true;
					case LOST -> lost = true;
					case SKIP -> {
					}
				}
			}
		}
		return new Choice(List.copyOf(call), capped, here, trial, busy, lost);
	}

	/** @return true when at least one mount was brought or a chunk load was filed */
	public static boolean blow(ServerPlayer player) {
		dropWaiting(player.getUUID());
		MinecraftServer server = player.serverLevel().getServer();
		MountHerdData herd = MountHerdData.get(player.serverLevel());
		List<Sight> sights = new ArrayList<>();
		for (HerdBook.Entry entry : herd.book().tames(player.getUUID())) {
			sights.add(sightOf(server, herd, player, entry));
		}
		Choice choice = choose(player.getUUID(), sights);
		int heard = 0;
		boolean brought = false;
		boolean failed = false;
		boolean trial = choice.trial();
		boolean busy = choice.busy();
		boolean lost = choice.lost();
		boolean here = choice.here();
		List<Component> rooms = new ArrayList<>();
		for (UUID id : choice.call()) {
			Attempt attempt = summon(server, player, id, brought);
			switch (attempt.outcome()) {
				case CAME -> {
					heard++;
					brought = true;
					Wave wave = WAVES.get(player.getUUID());
					if (wave != null) {
						wave.brought = true;
					}
				}
				case PENDING -> heard++;
				case ROOM -> {
					if (attempt.name() != null) {
						rooms.add(attempt.name());
					}
				}
				case FAIL -> failed = true;
				case NO -> {
					if (attempt.reason() == Reason.TRIAL) {
						trial = true;
					} else if (attempt.reason() == Reason.BUSY) {
						busy = true;
					} else if (attempt.reason() == Reason.LOST) {
						lost = true;
					} else if (attempt.reason() == Reason.HERE) {
						here = true;
					}
				}
			}
		}
		if (heard == 0) {
			if (trial) {
				bar(player, "shamanicmounts.flute.trial");
			} else if (busy) {
				bar(player, "shamanicmounts.flute.busy");
			} else if (lost || failed) {
				bar(player, "shamanicmounts.flute.lost");
			} else if (!rooms.isEmpty()) {
				for (Component name : rooms) {
					bar(player, "shamanicmounts.flute.room", name);
				}
			} else if (here) {
				bar(player, "shamanicmounts.flute.here");
			} else {
				bar(player, "shamanicmounts.flute.none");
			}
			return false;
		}
		for (Component name : rooms) {
			bar(player, "shamanicmounts.flute.room", name);
		}
		if (choice.capped() && heard >= CAP) {
			chat(player, "shamanicmounts.flute.capped");
		} else if (heard == 1) {
			chat(player, "shamanicmounts.flute.called");
		} else {
			chat(player, "shamanicmounts.flute.called_many", heard);
		}
		return true;
	}

	/** A mount just entered a level. Finish a flute call that was waiting on its chunk. */
	public static void added(ShamanicMount mount) {
		if (mount == null || mount.level().isClientSide() || SUMMONING.contains(mount.getUUID())) {
			return;
		}
		Wait wait = PENDING.remove(mount.getUUID());
		if (wait == null) {
			return;
		}
		Wave wave = WAVES.get(wait.owner());
		if (wave != null) {
			wave.seen = true;
			if (wave.left > 0) {
				wave.left--;
			}
		}
		MinecraftServer server = ((ServerLevel) mount.level()).getServer();
		ServerPlayer player = server.getPlayerList().getPlayer(wait.owner());
		if (player != null && !player.isRemoved()) {
			Attempt attempt = place(player, mount);
			if (wave != null && attempt.outcome() == Outcome.CAME) {
				wave.arrived++;
			}
			if (attempt.outcome() == Outcome.ROOM && attempt.name() != null) {
				bar(player, "shamanicmounts.flute.room", attempt.name());
			}
		}
		finishWave(server, wait.owner());
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		if (PENDING.isEmpty()) {
			return;
		}
		long now = event.getServer().getTickCount();
		Set<UUID> owners = new HashSet<>();
		Iterator<Map.Entry<UUID, Wait>> each = PENDING.entrySet().iterator();
		while (each.hasNext()) {
			Map.Entry<UUID, Wait> entry = each.next();
			if (now < entry.getValue().expiry()) {
				continue;
			}
			each.remove();
			UUID owner = entry.getValue().owner();
			Wave wave = WAVES.get(owner);
			if (wave != null && wave.left > 0) {
				wave.left--;
			}
			owners.add(owner);
		}
		for (UUID owner : owners) {
			finishWave(event.getServer(), owner);
		}
	}

	@SubscribeEvent
	public static void stopping(ServerStoppingEvent event) {
		PENDING.clear();
		WAVES.clear();
		SUMMONING.clear();
	}

	private static void dropWaiting(UUID owner) {
		PENDING.entrySet().removeIf(entry -> owner.equals(entry.getValue().owner()));
		WAVES.remove(owner);
	}

	private static void finishWave(MinecraftServer server, UUID owner) {
		Wave wave = WAVES.get(owner);
		if (wave == null || wave.left > 0) {
			return;
		}
		WAVES.remove(owner);
		if (wave.arrived == 0 && !wave.seen && !wave.brought) {
			ServerPlayer player = server.getPlayerList().getPlayer(owner);
			if (player != null) {
				bar(player, "shamanicmounts.flute.lost");
			}
		}
	}

	private static Sight sightOf(MinecraftServer server, MountHerdData herd, ServerPlayer player, HerdBook.Entry entry) {
		ShamanicMount mount = ShamanicMount.loaded(server, entry.id());
		boolean known = herd.where(entry.id()) != null;
		if (mount == null) {
			// The book does not store the order. Follow, stay, and wander are all called.
			return new Sight(entry.id(), entry.owner(), entry.tame(), MountMode.FOLLOW, false, false, false, false, false,
					false, false, Double.POSITIVE_INFINITY, known, false, false);
		}
		return live(player, mount, known);
	}

	private static Sight live(ServerPlayer player, ShamanicMount mount, boolean known) {
		boolean otherRider = false;
		for (Entity passenger : mount.getPassengers()) {
			if (passenger instanceof Player && passenger != player) {
				otherRider = true;
				break;
			}
		}
		boolean same = mount.level().dimension() == player.level().dimension();
		return new Sight(mount.getUUID(), mount.getOwnerUUID(), mount.isTame(), mount.mode(), mount.inReinTrial(),
				mount.isLeashed(), otherRider, !mount.isAlive() || mount.isRemoved(), mount.isAway(), true, same,
				mount.distanceToSqr(player), known, player.getVehicle() == mount, mount.isBaby());
	}

	private static Attempt summon(MinecraftServer server, ServerPlayer player, UUID id, boolean brought) {
		ShamanicMount mount = ShamanicMount.loaded(server, id);
		if (mount != null) {
			return place(player, mount);
		}
		return pull(server, player, id, brought);
	}

	private static Attempt pull(MinecraftServer server, ServerPlayer player, UUID id, boolean brought) {
		MountHerdData.Where at = MountHerdData.get(server.overworld()).where(id);
		if (at == null) {
			return Attempt.of(Outcome.FAIL);
		}
		ResourceLocation loc = ResourceLocation.tryParse(at.dim());
		if (loc == null) {
			return Attempt.of(Outcome.FAIL);
		}
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, loc));
		if (level == null) {
			return Attempt.of(Outcome.FAIL);
		}
		BlockPos pos = BlockPos.containing(at.x(), at.y(), at.z());
		level.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(pos), 3, pos);
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		if (level.getEntity(id) instanceof ShamanicMount mount) {
			return place(player, mount);
		}
		ShamanicMount anywhere = ShamanicMount.loaded(server, id);
		if (anywhere != null) {
			return place(player, anywhere);
		}
		PENDING.put(id, new Wait(player.getUUID(), server.getTickCount() + PENDING_TICKS));
		Wave wave = WAVES.computeIfAbsent(player.getUUID(), unused -> new Wave());
		wave.left++;
		if (brought) {
			wave.brought = true;
		}
		return Attempt.of(Outcome.PENDING);
	}

	private static Attempt place(ServerPlayer player, ShamanicMount mount) {
		if (!SUMMONING.add(mount.getUUID())) {
			return Attempt.of(Outcome.NO);
		}
		try {
			return placeUnlocked(player, mount);
		} finally {
			SUMMONING.remove(mount.getUUID());
		}
	}

	private static Attempt placeUnlocked(ServerPlayer player, ShamanicMount mount) {
		Reason reason = judge(live(player, mount, true), player.getUUID());
		if (reason != Reason.CALL) {
			return Attempt.skip(reason);
		}
		if (mount.isAway()) {
			MountPosts.recalled(mount.getUUID());
			mount.setMode(MountMode.FOLLOW);
			mount.returnNow();
			ShamanicMount arrived = ShamanicMount.loaded(player.serverLevel().getServer(), mount.getUUID());
			if (arrived == null || arrived.level() != player.level()) {
				return Attempt.of(Outcome.NO);
			}
			settle(arrived, player);
			return Attempt.of(Outcome.CAME);
		}
		Vec3 spot = MountCrossing.landing(player.serverLevel(), mount, player, false);
		if (spot == null) {
			return new Attempt(Outcome.ROOM, mount.getDisplayName(), null);
		}
		MountPosts.recalled(mount.getUUID());
		mount.setMode(MountMode.FOLLOW);
		if (mount.level() == player.level()) {
			face(mount, player);
			mount.teleportTo(spot.x, spot.y, spot.z);
			settle(mount, player);
			return Attempt.of(Outcome.CAME);
		}
		mount.ejectPassengers();
		Entity moved = mount.changeDimension(new DimensionTransition(player.serverLevel(), spot, Vec3.ZERO,
				player.getYRot(), 0.0F, DimensionTransition.DO_NOTHING));
		if (!(moved instanceof ShamanicMount arrived) || arrived.level() != player.level()) {
			return Attempt.of(Outcome.NO);
		}
		settle(arrived, player);
		return Attempt.of(Outcome.CAME);
	}

	private static void settle(ShamanicMount arrived, ServerPlayer player) {
		face(arrived, player);
		arrived.setPortalCooldown();
		arrived.getNavigation().stop();
		arrived.arrivedBeside();
		arrived.setMode(MountMode.FOLLOW);
	}

	private static void face(ShamanicMount mount, ServerPlayer player) {
		float yaw = player.getYRot();
		mount.setYRot(yaw);
		mount.setYHeadRot(yaw);
		mount.setYBodyRot(yaw);
	}

	private static void bar(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), true);
	}

	private static void chat(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable(key, args), false);
	}
}
