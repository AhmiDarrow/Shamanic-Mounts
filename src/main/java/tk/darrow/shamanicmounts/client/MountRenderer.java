package tk.darrow.shamanicmounts.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** Block cubes, scaled to the same size as the hitbox, with a walk, a sit, and a wing pose. */
public class MountRenderer extends EntityRenderer<ShamanicMount> {
	private static final ResourceLocation COAT = ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "textures/entity/mount.png");
	/** Sit and air ease in over a few frames so a mount settles instead of snapping. */
	private final Map<ShamanicMount, float[]> eased = new WeakHashMap<>();
	/** The live harness can hold every mount at one stride position: swing and amount. */
	static float[] forcedGait;
	/** The live harness can hold every mount in the air with one wing pose (1 glide, 2 flap); -1 is off. */
	static int forcedWing = -1;

	public MountRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.8f;
	}

	@Override
	public void render(ShamanicMount mount, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		float body = Mth.rotLerp(partialTick, mount.yBodyRotO, mount.yBodyRot);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - body));
		float scale = mount.phenotype().uniformScale * mount.getScale();
		pose.scale(scale, scale, scale);
		float swing = 0.0f;
		float amount = 0.0f;
		if (mount.isAlive()) {
			swing = mount.walkAnimation.position(partialTick);
			amount = Math.min(1.0f, mount.walkAnimation.speed(partialTick));
			if (mount.isBaby()) {
				swing *= 3.0f;
			}
		}
		if (forcedGait != null) {
			swing = forcedGait[0];
			amount = forcedGait[1];
		}
		float[] ease = eased.computeIfAbsent(mount, key -> new float[2]);
		float sitTarget = mount.isInSittingPose() ? 1.0f : 0.0f;
		boolean flying = forcedWing > 0 || (mount.wingPose() != 0 && !mount.onGround());
		float airTarget = flying || (!mount.onGround() && !mount.isInWater() && mount.getDeltaMovement().y < -0.3) ? 1.0f : 0.0f;
		ease[0] += (sitTarget - ease[0]) * 0.12f;
		ease[1] += (airTarget - ease[1]) * 0.15f;
		float sit = ease[0];
		float air = ease[1];
		pose.translate(0.0f, Math.abs(Mth.cos(swing * 1.3324f) * 0.04f * amount), 0.0f);
		// The rig's chest front is near its origin; slide it forward so the hitbox centres on the body.
		pose.translate(0.0f, 0.0f, -MountSize.form(mount.phenotype()).centre / 16f);
		float head = Mth.rotLerp(partialTick, mount.yHeadRotO, mount.yHeadRot);
		float age = mount.tickCount + partialTick;
		Phenotype phenotype = mount.phenotype();
		// Eye shine is a two-pixel detail; past thirty blocks it is not worth a second buffer.
		boolean near = this.entityRenderDispatcher.camera.getPosition().distanceToSqr(mount.position()) < 30.0 * 30.0;
		boolean glow = near && (phenotype.chimera || phenotype.sense == Phenotype.SenseShow.SCENT
				|| phenotype.phase != Phenotype.PhaseShow.SOLID);
		MountPose anim = new MountPose(swing, amount, age, Mth.wrapDegrees(head - body),
				Mth.lerp(partialTick, mount.xRotO, mount.getXRot()), forcedWing > 0 ? 1.0f : mount.wingOpen(partialTick),
				forcedWing > 0 ? forcedWing : mount.wingPose(), sit, air,
				(float) mount.getDeltaMovement().y, mount.isInWater(), mount.isBaby(), mount.getId(), glow);
		var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(COAT));
		java.util.function.Supplier<com.mojang.blaze3d.vertex.VertexConsumer> eyes = glow ? () -> buffers.getBuffer(RenderType.eyes(COAT)) : null;
		MountMesh.draw(phenotype, mount.saddled(), mount.hasBags(), mount.armorTier(), mount.pelt(), pose, consumer, eyes, light,
				OverlayTexture.NO_OVERLAY, anim);
		pose.popPose();
		super.render(mount, yaw, partialTick, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(ShamanicMount mount) {
		return COAT;
	}
}
