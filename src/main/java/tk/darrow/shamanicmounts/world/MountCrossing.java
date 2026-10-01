package tk.darrow.shamanicmounts.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.ride.FollowRules;

/**
 * Following mounts go with their owner into another dimension: through a portal, a command, the end's
 * way home, or a teleport from another mod. They are noted the moment the owner leaves, and moved over
 * once the owner has arrived, each set down on safe ground beside them.
 *
 * <p>Every dimension change takes the player out of the level they were in, so that one moment covers
 * them all. A mount ridden through a vanilla portal already carries its rider across and is skipped,
 * since it is no longer in the old level. Each mount moves by {@link Entity#changeDimension}, which
 * retires the old entity, so a mount is never copied.
 */
@EventBusSubscriber(modid = ShamanicMounts.MOD_ID)
public final class MountCrossing {
	/** Holds the chunks around the departure loaded while mounts wait for an owner between dimensions. */
	private static final TicketType<ChunkPos> WAITING = TicketType.create(ShamanicMounts.MOD_ID + ":crossing",
			Comparator.comparingLong(ChunkPos::toLong), FollowRules.CROSSING_WAIT_TICKS);
	/** Chunk radius of that ticket: the crossing range, rounded up, with one chunk of margin. */
	private static final int WAITING_RADIUS = 3;
	/** Offsets around the owner to try, nearest and level first. */
	private static final List<BlockPos> LANDING = landing();

	private static final Map<UUID, Crossing> PENDING = new HashMap<>();

	private record Crossing(ResourceKey<Level> from, ChunkPos chunk, List<UUID> mounts, @Nullable UUID ridden,
			long since) {
	}

	private MountCrossing() {
	}

	@SubscribeEvent
	public static void left(EntityLeaveLevelEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel from)
				|| player.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION || player.isSpectator()) {
			return;
		}
		long now = from.getGameTime();
		Crossing old = PENDING.remove(player.getUUID());
		if (old != null) {
			release(from.getServer(), old);
		}
		List<ShamanicMount> near = from.getEntitiesOfClass(ShamanicMount.class,
				player.getBoundingBox().inflate(FollowRules.CROSSING_RANGE), mount -> mount.crossesWith(player));
		if (near.isEmpty()) {
			return;
		}
		near.sort(Comparator.comparingDouble(mount -> mount.distanceToSqr(player)));
		List<UUID> ids = new ArrayList<>();
		UUID ridden = null;
		for (ShamanicMount mount : near) {
			if (ids.size() >= FollowRules.CROSSING_CAP) {
				break;
			}
			ids.add(mount.getUUID());
			if (ridden == null && mount.riddenBy(player, now)) {
				ridden = mount.getUUID();
			}
		}
		ChunkPos chunk = player.chunkPosition();
		from.getChunkSource().addRegionTicket(WAITING, chunk, WAITING_RADIUS, chunk);
		PENDING.put(player.getUUID(), new Crossing(from.dimension(), chunk, ids, ridden, from.getServer().getTickCount()));
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		if (PENDING.isEmpty()) {
			return;
		}
		MinecraftServer server = event.getServer();
		Iterator<Map.Entry<UUID, Crossing>> each = PENDING.entrySet().iterator();
		while (each.hasNext()) {
			Map.Entry<UUID, Crossing> entry = each.next();
			Crossing crossing = entry.getValue();
			if (server.getTickCount() <= crossing.since()) {
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			boolean expired = server.getTickCount() - crossing.since() > FollowRules.CROSSING_WAIT_TICKS;
			if (player != null && player.isRemoved() && !expired) {
				// Still between dimensions, as on the end credits. The mounts wait with the chunks held.
				continue;
			}
			each.remove();
			release(server, crossing);
			if (player != null && !player.isRemoved() && player.isAlive() && !player.isSpectator()
					&& player.level().dimension() != crossing.from()) {
				arrive(server, player, crossing);
			}
		}
	}

	@SubscribeEvent
	public static void stopping(ServerStoppingEvent event) {
		PENDING.clear();
	}

	private static void release(MinecraftServer server, Crossing crossing) {
		ServerLevel from = server.getLevel(crossing.from());
		if (from != null) {
			from.getChunkSource().removeRegionTicket(WAITING, crossing.chunk(), WAITING_RADIUS, crossing.chunk());
		}
	}

	private static void arrive(MinecraftServer server, ServerPlayer player, Crossing crossing) {
		ServerLevel from = server.getLevel(crossing.from());
		ServerLevel dest = player.serverLevel();
		if (from == null) {
			return;
		}
		for (UUID id : crossing.mounts()) {
			// Already gone from the old level (ridden through a portal, died, or unloaded): nothing to move.
			if (!(from.getEntity(id) instanceof ShamanicMount mount) || mount.isRemoved() || dest.getEntity(id) != null) {
				continue;
			}
			boolean ridden = id.equals(crossing.ridden());
			// The order may have changed since. Distance no longer counts: the owner is elsewhere now.
			if (!mount.isAlive() || mount.isLeashed() || mount.isPassenger() || mount.isAway() || !mount.isTame()
					|| !player.getUUID().equals(mount.getOwnerUUID()) || mount.isOrderedToSit()
					|| mount.mode() != tk.darrow.shamanicmounts.entity.MountMode.FOLLOW) {
				continue;
			}
			Vec3 spot = landing(dest, mount, player, ridden);
			if (spot == null && ridden) {
				ridden = false;
				spot = landing(dest, mount, player, false);
			}
			if (spot == null) {
				player.displayClientMessage(Component.translatable("mount.shamanicmounts.no_room", mount.getDisplayName()), true);
				continue;
			}
			// A rider a teleport did not unseat is still on it in the old level; step them off first so the
			// move does not drag them back with it.
			mount.ejectPassengers();
			Entity moved = mount.changeDimension(new DimensionTransition(dest, spot, Vec3.ZERO, player.getYRot(), 0.0f,
					DimensionTransition.DO_NOTHING));
			if (!(moved instanceof ShamanicMount arrived)) {
				continue;
			}
			// Set down beside a portal, it must not walk straight back through.
			arrived.setPortalCooldown();
			arrived.getNavigation().stop();
			arrived.arrivedBeside();
			if (ridden && player.getVehicle() == null && player.distanceToSqr(arrived) < 36.0) {
				player.startRiding(arrived, true);
			}
		}
	}

	/** Safe ground beside the owner that the mount's standing body fits on, with room for its rider if one goes on. */
	@Nullable
	static Vec3 landing(ServerLevel level, ShamanicMount mount, ServerPlayer player, boolean withRider) {
		EntityDimensions body = mount.getDimensions(Pose.STANDING);
		BlockPos base = player.blockPosition();
		for (BlockPos offset : LANDING) {
			if (offset.equals(BlockPos.ZERO) && !withRider) {
				continue;
			}
			BlockPos at = base.offset(offset);
			if (fits(level, body, at, withRider ? player.getBbHeight() : 0.0)) {
				return new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			}
		}
		// Nowhere beside: the owner's own block, which they stand in safely.
		if (!withRider && fits(level, body, base, 0.0)) {
			return new Vec3(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
		}
		return null;
	}

	/**
	 * The rule vanilla uses when a following pet teleports, for a whole body: solid ground under its middle
	 * that is not leaves, nothing in the way, no water or lava in it, and nothing that burns or pricks beside
	 * it. Unridden, every mount walks, so none is set down in water or in the air.
	 */
	private static boolean fits(ServerLevel level, EntityDimensions body, BlockPos at, double headroom) {
		BlockPos below = at.below();
		if (!level.isLoaded(at) || !level.getWorldBorder().isWithinBounds(at)) {
			return false;
		}
		BlockState ground = level.getBlockState(below);
		if (ground.getCollisionShape(level, below).isEmpty() || ground.getBlock() instanceof LeavesBlock) {
			return false;
		}
		AABB box = body.makeBoundingBox(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		if (!level.noCollision(box.expandTowards(0.0, headroom, 0.0)) || level.containsAnyLiquid(box)) {
			return false;
		}
		// Out of the portal itself, and clear of anything harmful around it and under it.
		return level.getBlockStates(box).noneMatch(state -> state.is(BlockTags.PORTALS))
				&& level.getBlockStates(box.inflate(1.0)).noneMatch(MountCrossing::harmful);
	}

	private static boolean harmful(BlockState state) {
		return state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE)
				|| state.is(BlockTags.CAMPFIRES) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
				|| state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW)
				|| state.is(Blocks.POINTED_DRIPSTONE);
	}

	private static List<BlockPos> landing() {
		List<BlockPos> offsets = new ArrayList<>();
		int reach = FollowRules.LANDING_REACH;
		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					offsets.add(new BlockPos(dx, dy, dz));
				}
			}
		}
		offsets.sort(Comparator.comparingInt(o -> o.getX() * o.getX() + o.getZ() * o.getZ() + 3 * o.getY() * o.getY()));
		return List.copyOf(offsets);
	}
}
