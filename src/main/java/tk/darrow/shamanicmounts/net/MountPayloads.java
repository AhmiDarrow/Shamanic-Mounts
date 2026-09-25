package tk.darrow.shamanicmounts.net;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.entity.MountHerdData;
import tk.darrow.shamanicmounts.entity.ShamanicMount;

/** Herd book sync, rename, release, and the ridden use key. */
public final class MountPayloads {
	/** Set by the client. The server leaves this as a no-op so it never loads a client class. */
	public static Consumer<ListTag> openBook = entries -> {
	};
	/** Set by the client: move the ridden mount, whose motion the rider's client owns. */
	public static Consumer<double[]> warp = at -> {
	};

	private MountPayloads() {
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(HerdSync.TYPE, HerdSync.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> openBook.accept(payload.data().getList("E", Tag.TAG_COMPOUND))));
		registrar.playToServer(HerdEdit.TYPE, HerdEdit.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> HerdEdit.handle(payload, (ServerPlayer) context.player())));
		registrar.playToServer(MountUse.TYPE, MountUse.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> MountUse.handle((ServerPlayer) context.player())));
		registrar.playToClient(MountWarp.TYPE, MountWarp.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> warp.accept(new double[] { payload.x(), payload.y(), payload.z() })));
		registrar.playToServer(MountModeChoice.TYPE, MountModeChoice.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> MountModeChoice.handle(payload, (ServerPlayer) context.player())));
		registrar.playToServer(MountKeys.TYPE, MountKeys.STREAM_CODEC,
				(payload, context) -> context.enqueueWork(() -> MountKeys.handle(payload, (ServerPlayer) context.player())));
	}

	public static void sendHerd(ServerPlayer player) {
		CompoundTag tag = new CompoundTag();
		tag.put("E", MountHerdData.get(player.serverLevel()).writePlayer(player.getUUID()));
		PacketDistributor.sendToPlayer(player, new HerdSync(tag));
	}

	public record HerdSync(CompoundTag data) implements CustomPacketPayload {
		public static final Type<HerdSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "herd"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HerdSync> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.COMPOUND_TAG, HerdSync::data, HerdSync::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record HerdEdit(int action, String id, String name) implements CustomPacketPayload {
		public static final Type<HerdEdit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "herd_edit"));
		public static final StreamCodec<RegistryFriendlyByteBuf, HerdEdit> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, HerdEdit::action, ByteBufCodecs.STRING_UTF8, HerdEdit::id, ByteBufCodecs.STRING_UTF8,
				HerdEdit::name, HerdEdit::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void handle(HerdEdit payload, ServerPlayer player) {
			java.util.UUID id;
			try {
				id = java.util.UUID.fromString(payload.id());
			} catch (IllegalArgumentException exception) {
				return;
			}
			MountHerdData herd = MountHerdData.get(player.serverLevel());
			boolean changed = false;
			if (payload.action() == 0) {
				changed = herd.rename(player.getUUID(), id, payload.name());
				if (changed) {
					ShamanicMount mount = ShamanicMount.loaded(player.server, id);
					if (mount != null) {
						mount.setCustomName(net.minecraft.network.chat.Component.literal(herd.book().get(id).name()));
						mount.setCustomNameVisible(true);
					}
				}
			} else if (payload.action() == 1) {
				changed = herd.release(player.getUUID(), id);
				if (changed) {
					ShamanicMount mount = ShamanicMount.loaded(player.server, id);
					if (mount != null) {
						mount.releaseIntoWorld(player);
					}
				}
			}
			// The open book already applied this edit. Sending the server herd back when the
			// tame is not one the server knows would replace that page with an empty book.
			if (changed) {
				sendHerd(player);
			}
		}
	}

	/** Follow, stay, or wander, chosen on the saddle bags screen for one of the player's own mounts. */
	public record MountModeChoice(int entity, int mode) implements CustomPacketPayload {
		public static final Type<MountModeChoice> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "mount_mode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MountModeChoice> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, MountModeChoice::entity, ByteBufCodecs.VAR_INT, MountModeChoice::mode, MountModeChoice::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void handle(MountModeChoice payload, ServerPlayer player) {
			if (player.level().getEntity(payload.entity()) instanceof ShamanicMount mount && mount.isTame()
					&& mount.isOwnedBy(player) && player.distanceToSqr(mount) < 100.0) {
				mount.setMode(tk.darrow.shamanicmounts.entity.MountMode.of(payload.mode()));
			}
		}
	}

	/**
	 * A blink. The rider's client moves the mount and would put it straight back, so the server
	 * tells that client where the mount now is.
	 */
	public record MountWarp(double x, double y, double z) implements CustomPacketPayload {
		public static final Type<MountWarp> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "mount_warp"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MountWarp> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.DOUBLE, MountWarp::x, ByteBufCodecs.DOUBLE, MountWarp::y, ByteBufCodecs.DOUBLE, MountWarp::z, MountWarp::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * The rider's sneak and jump keys, sent whenever either changes. Vanilla clears sneak every tick a
	 * rider holds it and only reports jump when it is released, so the mount reads the keys from here.
	 */
	public record MountKeys(boolean sneak, boolean jump) implements CustomPacketPayload {
		public static final Type<MountKeys> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "mount_keys"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MountKeys> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, MountKeys::sneak, ByteBufCodecs.BOOL, MountKeys::jump, MountKeys::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void handle(MountKeys payload, ServerPlayer player) {
			if (player.getVehicle() instanceof ShamanicMount mount) {
				mount.riderKeys(player, payload.sneak(), payload.jump());
			}
		}
	}

	public record MountUse(boolean pressed) implements CustomPacketPayload {
		public static final Type<MountUse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "mount_use"));
		public static final StreamCodec<RegistryFriendlyByteBuf, MountUse> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, MountUse::pressed, MountUse::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}

		private static void handle(ServerPlayer player) {
			if (player.getVehicle() instanceof ShamanicMount mount) {
				mount.used(player);
			}
		}
	}
}
