package tk.darrow.shamanicmounts.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.book.HerdIO;
import tk.darrow.shamanicmounts.entity.MountMenus;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.net.MountPayloads;
import tk.darrow.shamanicmounts.tame.ReinTrial;

public final class MountClient {
	private static boolean useWasDown;
	private static boolean sneakSent;
	private static boolean jumpSent;
	private static boolean sprintSent;
	private static boolean keysKnown;
	private static int reinSent;
	private static boolean reinKnown;

	/** What the client last told the server about its keys, for the harness. */
	public static String keysSent() {
		return (keysKnown ? "known" : "unknown") + " sneak=" + sneakSent + " jump=" + jumpSent + " sprint=" + sprintSent;
	}

	private MountClient() {
	}

	public static void install(IEventBus modBus) {
		modBus.addListener(MountClient::renderers);
		modBus.addListener(MountClient::screens);
		modBus.addListener(MountClient::buffers);
		NeoForge.EVENT_BUS.addListener(MountClient::tick);
		NeoForge.EVENT_BUS.addListener(MountClient::afterEntities);
		NeoForge.EVENT_BUS.addListener(ReinGuide::render);
		NeoForge.EVENT_BUS.addListener(ItemHints::tooltip);
		MountPayloads.openBook = MountClient::openBook;
		MountPayloads.warp = MountClient::warp;
	}

	public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(tk.darrow.shamanicmounts.entity.MountEntities.MOUNT.get(), MountRenderer::new);
	}

	public static void screens(RegisterMenuScreensEvent event) {
		event.register(MountMenus.CHEST.get(), MountChestScreen::new);
	}

	/** The eye shine gets its own buffer, so it is gathered across every mount and drawn once. */
	public static void buffers(RegisterRenderBuffersEvent event) {
		event.registerRenderBuffer(MountRenderer.EYES);
	}

	/**
	 * The gathered eye shine is drawn as soon as the entities are, where each mount's eyes used to be drawn: before
	 * block entities, water, and particles, so water in front of a mount still hides its eyes.
	 */
	public static void afterEntities(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			Minecraft.getInstance().renderBuffers().bufferSource().endBatch(MountRenderer.EYES);
		}
	}

	public static void tick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof ShamanicMount mount)) {
			useWasDown = false;
			keysKnown = false;
			reinKnown = false;
			sprintSent = false;
			return;
		}
		boolean sneak = minecraft.options.keyShift.isDown();
		boolean jump = minecraft.options.keyJump.isDown();
		// Gallop is the sprint key.
		boolean sprint = minecraft.options.keySprint.isDown();
		if (!keysKnown || sneak != sneakSent || jump != jumpSent || sprint != sprintSent) {
			// The local copy moves the mount, so it hears the keys first; the server keeps the rules.
			mount.riderKeys(minecraft.player, sneak, jump, sprint);
			PacketDistributor.sendToServer(new MountPayloads.MountKeys(sneak, jump, sprint));
			sneakSent = sneak;
			jumpSent = jump;
			sprintSent = sprint;
			keysKnown = true;
		}
		int rein = 0;
		if (minecraft.options.keyLeft.isDown()) {
			rein |= ReinTrial.PRESS_LEFT;
		}
		if (minecraft.options.keyRight.isDown()) {
			rein |= ReinTrial.PRESS_RIGHT;
		}
		if (minecraft.options.keyUp.isDown()) {
			rein |= ReinTrial.PRESS_FORWARD;
		}
		if (minecraft.options.keyDown.isDown()) {
			rein |= ReinTrial.PRESS_BACK;
		}
		if (!reinKnown || rein != reinSent) {
			PacketDistributor.sendToServer(new MountPayloads.MountRein(rein));
			reinSent = rein;
			reinKnown = true;
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

	private static void openBook(net.minecraft.nbt.CompoundTag data) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}
		HerdBook book = HerdIO.read(data.getList("E", net.minecraft.nbt.Tag.TAG_COMPOUND));
		if (data.getBoolean("C")) {
			book.bredChimera(minecraft.player.getUUID());
		}
		ClientBook.open(minecraft.player.getUUID(), book);
	}
}
