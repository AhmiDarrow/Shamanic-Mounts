package tk.darrow.shamanicmounts.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ListTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import tk.darrow.shamanicmounts.book.HerdIO;
import tk.darrow.shamanicmounts.entity.MountMenus;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.net.MountPayloads;

public final class MountClient {
	private static boolean useWasDown;
	private static boolean sneakSent;
	private static boolean jumpSent;
	private static boolean keysKnown;

	/** What the client last told the server about its keys, for the harness. */
	public static String keysSent() {
		return (keysKnown ? "known" : "unknown") + " sneak=" + sneakSent + " jump=" + jumpSent;
	}

	private MountClient() {
	}

	public static void install(IEventBus modBus) {
		modBus.addListener(MountClient::renderers);
		modBus.addListener(MountClient::screens);
		NeoForge.EVENT_BUS.addListener(MountClient::tick);
		MountPayloads.openBook = MountClient::openBook;
		MountPayloads.warp = MountClient::warp;
	}

	public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(tk.darrow.shamanicmounts.entity.MountEntities.MOUNT.get(), MountRenderer::new);
	}

	public static void screens(RegisterMenuScreensEvent event) {
		event.register(MountMenus.CHEST.get(), MountChestScreen::new);
	}

	public static void tick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof ShamanicMount mount)) {
			useWasDown = false;
			keysKnown = false;
			return;
		}
		boolean sneak = minecraft.options.keyShift.isDown();
		boolean jump = minecraft.options.keyJump.isDown();
		if (!keysKnown || sneak != sneakSent || jump != jumpSent) {
			// The local copy moves the mount, so it hears the keys first; the server keeps the rules.
			mount.riderKeys(minecraft.player, sneak, jump);
			PacketDistributor.sendToServer(new MountPayloads.MountKeys(sneak, jump));
			sneakSent = sneak;
			jumpSent = jump;
			keysKnown = true;
		}
		boolean down = minecraft.options.keyUse.isDown();
		if (down && !useWasDown) {
			PacketDistributor.sendToServer(new MountPayloads.MountUse(true));
		}
		useWasDown = down;
	}

	private static void warp(double[] at) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null && minecraft.player.getVehicle() instanceof ShamanicMount mount) {
			mount.absMoveTo(at[0], at[1], at[2]);
			mount.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		}
	}

	private static void openBook(ListTag entries) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		ClientBook.open(minecraft.player.getUUID(), HerdIO.read(entries));
	}
}
