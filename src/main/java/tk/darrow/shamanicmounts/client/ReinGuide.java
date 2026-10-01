package tk.darrow.shamanicmounts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.tame.ReinTrial;

/** The direction the rider is asked to press, drawn large in the middle of the screen. */
public final class ReinGuide {
	private static final int GOLD = 0xFFE7C36A;
	private static final int INK = 0xFF1A1208;
	private static byte lastCode;
	private static int startedTick;

	private ReinGuide() {
	}

	public static void render(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null
				|| !(minecraft.player.getVehicle() instanceof ShamanicMount mount) || mount.isTame()) {
			lastCode = 0;
			return;
		}
		ReinTrial.Dir cue = mount.shownCue();
		if (cue == null) {
			lastCode = 0;
			return;
		}
		byte code = cue.code();
		if (code != lastCode) {
			lastCode = code;
			startedTick = mount.tickCount;
		}
		int elapsed = Math.max(0, mount.tickCount - startedTick);
		float left = 1.0f - Math.min(elapsed, ReinTrial.BEAT_TICKS) / (float) ReinTrial.BEAT_TICKS;

		GuiGraphics graphics = event.getGuiGraphics();
		int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
		int centerY = minecraft.getWindow().getGuiScaledHeight() / 2 - 8;
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(centerX, centerY, 0.0f);
		// A new prompt punches in and shakes once, then holds still so the direction stays readable.
		float pop = elapsed < 5 ? 1.28f - elapsed * 0.05f : 1.0f;
		float shake = elapsed < 7 ? Mth.sin(elapsed * 2.6f) * (7 - elapsed) : 0.0f;
		pose.mulPose(Axis.ZP.rotationDegrees(turn(cue) + shake));
		pose.scale(pop, pop, 1.0f);
		arrow(graphics);
		pose.popPose();

		graphics.drawCenteredString(minecraft.font, Component.translatable(cue.key()), centerX, centerY - 40, GOLD);
		int width = 80;
		int filled = Math.round(width * left);
		int barX = centerX - width / 2;
		int barY = centerY + 32;
		graphics.fill(barX - 1, barY - 1, barX + width + 1, barY + 5, INK);
		if (filled > 0) {
			graphics.fill(barX, barY, barX + filled, barY + 4, GOLD);
		}
	}

	/**
	 * The arrow is drawn pointing up. Screen Y grows downward, so positive Z rotation turns that
	 * up-arrow clockwise: right, down, left.
	 */
	private static float turn(ReinTrial.Dir cue) {
		return switch (cue) {
			case FORWARD -> 0.0f;
			case RIGHT -> 90.0f;
			case BACK -> 180.0f;
			case LEFT -> -90.0f;
		};
	}

	/** Point at the top. The wide end of the head is the lower row. */
	private static void arrow(GuiGraphics graphics) {
		graphics.fill(-8, -4, 8, 16, INK);
		for (int row = 0; row < 24; row++) {
			int half = 3 + row;
			graphics.fill(-half, -26 + row, half, -25 + row, INK);
		}
		graphics.fill(-4, -2, 4, 13, GOLD);
		for (int row = 0; row < 20; row++) {
			int half = 1 + row;
			graphics.fill(-half, -22 + row, half, -21 + row, GOLD);
		}
	}
}
