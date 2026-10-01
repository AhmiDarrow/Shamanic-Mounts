package tk.darrow.shamanicmounts.client;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Draws blender-space cubes for one mount. X is right, Y runs toward the tail, Z is up, and the feet
 * stand on Z = 0. Every joint opens a new rigid part, so face culling never crosses a hinge.
 *
 * <p>The joints are kept on the draw buffer's pooled transform stack, which does the same arithmetic as a
 * {@link PoseStack} without a new matrix pair and quaternion at every joint of every frame.
 */
final class Pen {
	static final int EYE_LEFT = 1;
	static final int EYE_RIGHT = 2;
	static final int EYE_FRONT = 3;

	final MountPose anim;
	private final SolidDraw draw;
	private int group;
	private int groupSeq;
	/** How many parts deep the pen is. Each level grows its cubes a hair, see {@link #NEST}. */
	private int depth;
	/**
	 * Growth per level of nesting, in blocks (about a fiftieth of a pixel). Culling cannot see across
	 * parts, so a child face lying in its parent's plane would fight it for the same depth and
	 * flicker; a hair of growth makes the child win cleanly without a visible change.
	 */
	private static final float NEST = 0.0012f;

	Pen(PoseStack pose, MountPose anim) {
		this(pose, anim, new SolidDraw());
	}

	/** A pen over a shared draw buffer, starting at {@code pose}'s current transform. The buffer is emptied by every flush. */
	Pen(PoseStack pose, MountPose anim, SolidDraw draw) {
		this.anim = anim;
		this.draw = draw;
		draw.begin(pose.last());
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
		float grow = depth * NEST;
		draw.add(group, x / 16f - grow, z / 16f - grow, y / 16f - grow, dx / 16f + 2 * grow, dz / 16f + 2 * grow,
				dy / 16f + 2 * grow, coat, eyeFace, eye);
	}

	/** Offset in blender pixels from the current joint, staying in the same rigid part. */
	void shift(float bx, float by, float bz, Runnable body) {
		draw.push();
		draw.translate(bx / 16f, bz / 16f, by / 16f);
		body.run();
		draw.pop();
	}

	/** Bend around the current origin. Yaw sweeps back, roll flaps, pitch feathers. */
	void curl(float yawDeg, float rollDeg, float pitchDeg, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		depth++;
		draw.push();
		if (yawDeg != 0f) {
			draw.rotateY(yawDeg);
		}
		if (pitchDeg != 0f) {
			draw.rotateX(pitchDeg);
		}
		if (rollDeg != 0f) {
			draw.rotateZ(rollDeg);
		}
		body.run();
		draw.pop();
		group = saved;
		depth--;
	}

	/**
	 * Yaw, roll, then pitch, in degrees, around a blender-pixel joint. Positive pitch turns the part
	 * so that what hangs below the joint swings toward the nose.
	 */
	void hinge(float bx, float by, float bz, float yawDeg, float rollDeg, float pitchDeg, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		depth++;
		float hx = bx / 16f;
		float hy = bz / 16f;
		float hz = by / 16f;
		draw.push();
		draw.translate(hx, hy, hz);
		if (yawDeg != 0f) {
			draw.rotateY(yawDeg);
		}
		if (pitchDeg != 0f) {
			draw.rotateX(pitchDeg);
		}
		if (rollDeg != 0f) {
			draw.rotateZ(rollDeg);
		}
		draw.translate(-hx, -hy, -hz);
		body.run();
		draw.pop();
		group = saved;
		depth--;
	}

	/** Grow or shrink a part about a blender-pixel point, in its own rigid group. */
	void scaleAt(float bx, float by, float bz, float sx, float sy, float sz, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		depth++;
		float hx = bx / 16f;
		float hy = bz / 16f;
		float hz = by / 16f;
		draw.push();
		draw.translate(hx, hy, hz);
		draw.scale(sx, sz, sy);
		draw.translate(-hx, -hy, -hz);
		body.run();
		draw.pop();
		group = saved;
		depth--;
	}

	/** Move a whole part without rotating it, in its own rigid group. */
	void lift(float bx, float by, float bz, Runnable body) {
		int saved = group;
		group = ++groupSeq;
		depth++;
		draw.push();
		draw.translate(bx / 16f, bz / 16f, by / 16f);
		body.run();
		draw.pop();
		group = saved;
		depth--;
	}

	SolidDraw.Plan flush(VertexConsumer consumer, int light, int overlay, SolidDraw.Plan cached, Supplier<VertexConsumer> glow) {
		return draw.flush(consumer, light, overlay, cached, anim.blink, glow);
	}

	/** Draw with the plans kept in {@code plans[slot]} and {@code plans[spare]}; see {@link SolidDraw#flush}. */
	void flush(VertexConsumer consumer, int light, int overlay, SolidDraw.Plan[] plans, int slot, int spare,
			Supplier<VertexConsumer> glow, boolean cull) {
		draw.flush(consumer, light, overlay, plans, slot, spare, anim.blink, glow, cull);
	}
}
