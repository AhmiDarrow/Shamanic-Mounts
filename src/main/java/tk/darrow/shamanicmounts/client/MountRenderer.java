package tk.darrow.shamanicmounts.client;

import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
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
	/**
	 * The eye shine. It has a buffer of its own in the level's buffer source (see {@link MountClient}), so asking for it
	 * does not end the coat batch: every mount's coat goes to the GPU in one draw and every eye in another, instead of
	 * two draws for each mount with shining eyes.
	 */
	static final RenderType EYES = RenderType.eyes(COAT);
	/** Sit and air ease in over a few ticks so a mount settles instead of snapping. */
	private final Map<ShamanicMount, MountEase> eased = new WeakHashMap<>();
	/** The live harness can hold every mount at one stride position: swing and amount. */
	static float[] forcedGait;
	/** The live harness can hold every mount in the air with one wing pose (1 glide, 2 flap); -1 is off. */
	static int forcedWing = -1;

	public MountRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.8f;
	}

	/** A foal's shadow is as small as the foal. */
	@Override
	protected float getShadowRadius(ShamanicMount mount) {
		return this.shadowRadius * mount.getAgeScale();
	}

	@Override
	public void render(ShamanicMount mount, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		// Faces turned away from the camera are skipped before they are submitted, but only where that changes nothing:
		// a back-face-culled draw in the world, through a perspective camera at the origin of an unrotated pose space.
		boolean cull = mount.isAddedToLevel() && cameraAtOrigin(pose.last().pose());
		pose.pushPose();
		float body = Mth.rotLerp(partialTick, mount.yBodyRotO, mount.yBodyRot);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - body));
		float scale = mount.phenotype().uniformScale * mount.getScale() * mount.getAgeScale();
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
		float sitTarget = mount.isInSittingPose() ? 1.0f : 0.0f;
		boolean flying = forcedWing > 0 || (mount.wingPose() != 0 && !mount.onGround());
		float airTarget = flying || (!mount.onGround() && !mount.isInWater() && mount.getDeltaMovement().y < -0.3) ? 1.0f : 0.0f;
		MountEase ease = eased.computeIfAbsent(mount, key -> new MountEase(sitTarget, airTarget, key.tickCount));
		if (mount.isAddedToLevel()) {
			ease.tick(mount.tickCount, sitTarget, airTarget);
		} else {
			ease.snap(sitTarget, airTarget);
		}
		float sit = ease.sit(partialTick);
		float air = ease.air(partialTick);
		pose.translate(0.0f, Math.abs(Mth.cos(swing * 1.3324f) * 0.04f * amount), 0.0f);
		// The rig's chest front is near its origin; slide it forward so the hitbox centres on the body.
		pose.translate(0.0f, 0.0f, -MountSize.form(mount.phenotype()).centre / 16f);
		float head = Mth.rotLerp(partialTick, mount.yHeadRotO, mount.yHeadRot);
		float age = mount.tickCount + partialTick;
		Phenotype phenotype = mount.phenotype();
		// Eye shine is a two-pixel detail; past thirty blocks it is not worth a second buffer.
		boolean near = !mount.isAddedToLevel()
				|| this.entityRenderDispatcher.camera.getPosition().distanceToSqr(mount.position()) < 30.0 * 30.0;
		boolean glow = near && (phenotype.chimera || phenotype.sense == Phenotype.SenseShow.SCENT
				|| phenotype.phase != Phenotype.PhaseShow.SOLID);
		MountPose anim = new MountPose(swing, amount, age, Mth.wrapDegrees(head - body),
				Mth.lerp(partialTick, mount.xRotO, mount.getXRot()), forcedWing > 0 ? 1.0f : mount.wingOpen(partialTick),
				forcedWing > 0 ? forcedWing : mount.wingPose(), sit, air,
				(float) mount.getDeltaMovement().y, mount.isInWater(), mount.isBaby(), mount.getId(), glow);
		// Every face is wound outward, so a mount in the world skips its back faces on the GPU. A book
		// portrait is drawn with the depth axis flipped, which reverses the winding, so it keeps both sides.
		var consumer = buffers.getBuffer(mount.isAddedToLevel() ? RenderType.entityCutout(COAT) : RenderType.entityCutoutNoCull(COAT));
		java.util.function.Supplier<com.mojang.blaze3d.vertex.VertexConsumer> eyes = glow ? () -> buffers.getBuffer(EYES) : null;
		MountMesh.draw(phenotype, mount.saddled(), mount.hasBags(), mount.armorTier(), mount.pelt(), pose, consumer, eyes, light,
				OverlayTexture.NO_OVERLAY, anim, cull);
		pose.popPose();
		super.render(mount, yaw, partialTick, pose, buffers, light);
	}

	/**
	 * Whether the pose space is the level's camera space: no rotation or scale yet, only the offset to the entity, and a
	 * perspective projection that sorts by distance to the origin. The level's entity pass draws like that; a picture
	 * in a screen, or a pass drawn from another viewpoint, does not.
	 */
	private static boolean cameraAtOrigin(Matrix4f m) {
		return m.m00() == 1f && m.m11() == 1f && m.m22() == 1f && m.m01() == 0f && m.m02() == 0f && m.m10() == 0f
				&& m.m12() == 0f && m.m20() == 0f && m.m21() == 0f && m.m03() == 0f && m.m13() == 0f && m.m23() == 0f
				&& m.m33() == 1f && RenderSystem.getVertexSorting() == VertexSorting.DISTANCE_TO_ORIGIN;
	}

	@Override
	public ResourceLocation getTextureLocation(ShamanicMount mount) {
		return COAT;
	}
}
