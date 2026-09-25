package tk.darrow.shamanicmounts.client;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/**
 * Draws blender-space cubes for one mount. X is right, Y runs toward the tail, Z is up, and the feet
 * stand on Z = 0. Every joint opens a new rigid part, so face culling never crosses a hinge.
 */
final class Pen {
	static final int EYE_LEFT = 1;
	static final int EYE_RIGHT = 2;
	static final int EYE_FRONT = 3;

	final PoseStack pose;
	final MountPose anim;
	private final SolidDraw draw = new SolidDraw();
	private int group;
	private int groupSeq;

	Pen(PoseStack pose, MountPose anim) {
		this.pose = pose;
		this.anim = anim;
	}

	void box(float x, float y, float z, float dx, float dy, float dz, Mat mat) {
		add(x, y, z, dx, dy, dz, mat, 0, null);
	}

	/** The cube and its mirror across the centre line. */
	void pair(float x, float y, float z, float dx, float dy, float dz, Mat mat) {
		box(x, y, z, dx, dy, dz, mat);
		box(-x - dx, y, z, dx, dy, dz, mat);
	}

	void gaze(float x, float y, float z, float dx, float dy, float dz, Mat rim, Mat eye, int face) {
		add(x, y, z, dx, dy, dz, rim, face, eye);
	}

	private void add(float x, float y, float z, float dx, float dy, float dz, Mat coat, int eyeFace, Mat eye) {
		draw.add(pose, group, x / 16f, z / 16f, y / 16f, dx / 16f, dz / 16f, dy / 16f, coat, eyeFace, eye);
	}

	/** Offset in blender pixels from the current joint, staying in the same rigid part. */
	void shift(float bx, float by, float bz, Runnable body) {
		pose.pushPose();
		pose.translate(bx / 16f, bz / 16f, by / 16f);
		body.run();
		pose.popPose();
	}

	/** Bend around the current origin. Yaw sweeps back, roll flaps, pitch feathers. */
	void curl(float yawDeg, float rollDeg, float pitchDeg, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		pose.pushPose();
		if (yawDeg != 0f) {
			pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
		}
		if (pitchDeg != 0f) {
			pose.mulPose(Axis.XP.rotationDegrees(pitchDeg));
		}
		if (rollDeg != 0f) {
			pose.mulPose(Axis.ZP.rotationDegrees(rollDeg));
		}
		body.run();
		pose.popPose();
		group = saved;
	}

	/**
	 * Yaw, roll, then pitch, in degrees, around a blender-pixel joint. Positive pitch turns the part
	 * so that what hangs below the joint swings toward the nose.
	 */
	void hinge(float bx, float by, float bz, float yawDeg, float rollDeg, float pitchDeg, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		float hx = bx / 16f;
		float hy = bz / 16f;
		float hz = by / 16f;
		pose.pushPose();
		pose.translate(hx, hy, hz);
		if (yawDeg != 0f) {
			pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
		}
		if (pitchDeg != 0f) {
			pose.mulPose(Axis.XP.rotationDegrees(pitchDeg));
		}
		if (rollDeg != 0f) {
			pose.mulPose(Axis.ZP.rotationDegrees(rollDeg));
		}
		pose.translate(-hx, -hy, -hz);
		body.run();
		pose.popPose();
		group = saved;
	}

	/** Grow or shrink a part about a blender-pixel point, in its own rigid group. */
	void scaleAt(float bx, float by, float bz, float sx, float sy, float sz, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		float hx = bx / 16f;
		float hy = bz / 16f;
		float hz = by / 16f;
		pose.pushPose();
		pose.translate(hx, hy, hz);
		pose.scale(sx, sz, sy);
		pose.translate(-hx, -hy, -hz);
		body.run();
		pose.popPose();
		group = saved;
	}

	/** Move a whole part without rotating it, in its own rigid group. */
	void lift(float bx, float by, float bz, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		pose.pushPose();
		pose.translate(bx / 16f, bz / 16f, by / 16f);
		body.run();
		pose.popPose();
		group = saved;
	}

	SolidDraw.Plan flush(VertexConsumer consumer, int light, int overlay, SolidDraw.Plan cached, Supplier<VertexConsumer> glow) {
		return draw.flush(consumer, light, overlay, cached, anim.blink, glow);
	}
}
