package tk.darrow.shamanicmounts.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Axis-aligned cubes with buried faces removed and a coat that runs across them.
 *
 * <p>A face is kept only where its outside is air, a later cube wins a shared plane, and nothing is
 * nudged off its plane, so stacked shells neither flicker nor crack. Culling stays inside one posed
 * part so a swinging leg does not lose faces it needs when it leaves the body.
 *
 * <p>Every coat face reads its {@link Mat} cell one texel per model pixel, addressed by the face's
 * own position in the part, so the fur on a shoulder plate continues onto the barrel behind it.
 * Eye faces instead stretch a pixel eye from the cell, sized to the face.
 *
 * <p>The cut plan depends only on the cube layout, not the pose, so a caller can keep the
 * {@link Plan} from one frame and hand it back on the next.
 */
final class SolidDraw {
	private static final float EPS = 0.0004f;
	private static final float PX = 16.0f;
	/** Eye art regions in an eye cell, as texel rectangles: 2×2, 3×2, 4×3, and the closed lid. */
	private static final int[] EYE_2 = { 0, 0, 8, 8 };
	private static final int[] EYE_3 = { 8, 0, 20, 8 };
	private static final int[] EYE_4 = { 0, 8, 16, 20 };
	private static final int[] EYE_SHUT = { 16, 8, 24, 16 };

	private final List<Box> boxes = new ArrayList<>();
	/** The last pose snapshot, shared by every cube drawn under the same transform. */
	private Matrix4f lastModel;
	private Matrix3f lastNormal;
	/** Scratch corners and texture coordinates for one quad, reused across the frame. */
	private final float[] xs = new float[4];
	private final float[] ys = new float[4];
	private final float[] zs = new float[4];
	private final float[] us = new float[4];
	private final float[] vs = new float[4];
	private final Vector3f normalScratch = new Vector3f();

	/** Coordinates are blocks in Minecraft axes: x right, y up, z toward the tail. */
	void add(PoseStack pose, int group, float x, float y, float z, float w, float h, float d, Mat coat, int eyeFace, Mat eye) {
		if (w < EPS || h < EPS || d < EPS) {
			return;
		}
		Matrix4f model = pose.last().pose();
		if (lastModel == null || !lastModel.equals(model)) {
			lastModel = new Matrix4f(model);
			lastNormal = new Matrix3f(pose.last().normal());
		}
		boxes.add(new Box(x, y, z, x + w, y + h, z + d, coat, eyeFace, eye, group, boxes.size(), lastModel, lastNormal));
	}

	/**
	 * Draw every visible face. {@code cached} may be null or stale; the plan actually used is returned.
	 * With a {@code glow} supplier the eye faces are drawn again, full bright, for eyes that shine at night.
	 */
	Plan flush(VertexConsumer consumer, int light, int overlay, Plan cached, boolean blink, Supplier<VertexConsumer> glow) {
		Plan plan = cached != null && cached.matches(boxes) ? cached : plan();
		for (Face face : plan.faces) {
			Box box = boxes.get(face.box);
			for (Rect rect : face.rects) {
				emit(box, face, rect, consumer, light, overlay, blink);
			}
		}
		// The glow buffer is fetched only now: asking the buffer source for it ends the coat buffer.
		if (glow != null && !blink) {
			VertexConsumer shine = null;
			for (Face face : plan.faces) {
				if (!face.eye) {
					continue;
				}
				if (shine == null) {
					shine = glow.get();
				}
				Box box = boxes.get(face.box);
				for (Rect rect : face.rects) {
					emit(box, face, rect, shine, 0xF000F0, overlay, false);
				}
			}
		}
		boxes.clear();
		lastModel = null;
		lastNormal = null;
		return plan;
	}

	private Plan plan() {
		List<Face> faces = new ArrayList<>(boxes.size() * 4);
		for (Box box : boxes) {
			for (int axis = 0; axis < 3; axis++) {
				face(faces, box, axis, 1);
				face(faces, box, axis, -1);
			}
		}
		return new Plan(signature(boxes), faces);
	}

	private void face(List<Face> faces, Box box, int axis, int sign) {
		float plane = sign > 0 ? box.max(axis) : box.min(axis);
		int a = axis == 0 ? 1 : 0;
		int b = axis == 2 ? 1 : 2;
		Rect full = new Rect(box.min(a), box.min(b), box.max(a), box.max(b));
		List<Rect> rects = new ArrayList<>(1);
		rects.add(full);
		float outside = plane + sign * EPS;
		for (Box other : boxes) {
			if (other == box || other.group != box.group) {
				continue;
			}
			boolean buried = outside > other.min(axis) && outside < other.max(axis);
			boolean shared = !buried && other.order > box.order
					&& Math.abs((sign > 0 ? other.max(axis) : other.min(axis)) - plane) < EPS;
			if (!buried && !shared) {
				continue;
			}
			Rect cut = overlap(full, other.min(a), other.min(b), other.max(a), other.max(b));
			if (cut == null) {
				continue;
			}
			rects = subtractAll(rects, cut);
			if (rects.isEmpty()) {
				return;
			}
		}
		int eyeId = axis == 0 ? (sign < 0 ? 1 : 2) : axis == 2 && sign < 0 ? 3 : 0;
		boolean eye = eyeId != 0 && eyeId == box.eyeFace && box.eye != null;
		// Planar coat address. Across runs along the face's horizontal edge; up runs along y, or z on a lid.
		float acrossMin = axis == 0 ? box.z0 : box.x0;
		float acrossMax = axis == 0 ? box.z1 : box.x1;
		float upMin = axis == 1 ? box.z0 : box.y0;
		float upMax = axis == 1 ? box.z1 : box.y1;
		float acrossOff = anchorLow(acrossMin * PX, acrossMax * PX);
		float upOff = axis == 1 ? anchorLow(upMin * PX, upMax * PX) : anchorHigh(upMin * PX, upMax * PX);
		faces.add(new Face(box.order, axis, sign, plane, full, eye, rects, acrossOff, upOff));
	}

	/** The cell-aligned start at or below {@code lo}, unless the span would then leave the cell. */
	private static float anchorLow(float lo, float hi) {
		float off = (float) Math.floor(lo / Mat.CELL) * Mat.CELL;
		return hi - off > Mat.CELL ? lo : off;
	}

	/** The cell-aligned top at or above {@code hi}, unless the span would then leave the cell. */
	private static float anchorHigh(float lo, float hi) {
		float top = (float) Math.ceil(hi / Mat.CELL) * Mat.CELL;
		return top - lo > Mat.CELL ? hi : top;
	}

	private static Rect overlap(Rect face, float ou0, float ov0, float ou1, float ov1) {
		float a0 = Math.max(face.u0, ou0);
		float b0 = Math.max(face.v0, ov0);
		float a1 = Math.min(face.u1, ou1);
		float b1 = Math.min(face.v1, ov1);
		if (a1 - a0 < EPS || b1 - b0 < EPS) {
			return null;
		}
		return new Rect(a0, b0, a1, b1);
	}

	private static List<Rect> subtractAll(List<Rect> rects, Rect cut) {
		List<Rect> next = new ArrayList<>(rects.size() + 2);
		for (Rect rect : rects) {
			subtract(rect, cut, next);
		}
		return next;
	}

	private static void subtract(Rect rect, Rect cut, List<Rect> out) {
		float u0 = Math.max(rect.u0, cut.u0);
		float v0 = Math.max(rect.v0, cut.v0);
		float u1 = Math.min(rect.u1, cut.u1);
		float v1 = Math.min(rect.v1, cut.v1);
		if (u0 >= u1 - EPS || v0 >= v1 - EPS) {
			out.add(rect);
			return;
		}
		if (rect.u0 < u0 - EPS) {
			out.add(new Rect(rect.u0, rect.v0, u0, rect.v1));
		}
		if (u1 < rect.u1 - EPS) {
			out.add(new Rect(u1, rect.v0, rect.u1, rect.v1));
		}
		if (rect.v0 < v0 - EPS) {
			out.add(new Rect(u0, rect.v0, u1, v0));
		}
		if (v1 < rect.v1 - EPS) {
			out.add(new Rect(u0, v1, u1, rect.v1));
		}
	}

	private void emit(Box box, Face face, Rect rect, VertexConsumer consumer, int light, int overlay, boolean blink) {
		float u0 = rect.u0;
		float v0 = rect.v0;
		float u1 = rect.u1;
		float v1 = rect.v1;
		float p = face.plane;
		switch (face.axis) {
			case 0 -> {
				if (face.sign > 0) {
					corners(xs, ys, zs, p, u0, v1, p, u0, v0, p, u1, v0, p, u1, v1);
				} else {
					corners(xs, ys, zs, p, u0, v0, p, u0, v1, p, u1, v1, p, u1, v0);
				}
			}
			case 1 -> {
				if (face.sign > 0) {
					corners(xs, ys, zs, u0, p, v1, u1, p, v1, u1, p, v0, u0, p, v0);
				} else {
					corners(xs, ys, zs, u0, p, v0, u1, p, v0, u1, p, v1, u0, p, v1);
				}
			}
			default -> {
				if (face.sign > 0) {
					corners(xs, ys, zs, u0, v0, p, u1, v0, p, u1, v1, p, u0, v1, p);
				} else {
					corners(xs, ys, zs, u1, v0, p, u0, v0, p, u0, v1, p, u1, v1, p);
				}
			}
		}
		for (int i = 0; i < 4; i++) {
			if (face.eye) {
				eyeUv(box, face, xs[i], ys[i], zs[i], us, vs, i, blink);
			} else {
				coatUv(box, face, xs[i], ys[i], zs[i], us, vs, i);
			}
		}
		float nx = face.axis == 0 ? face.sign : 0;
		float ny = face.axis == 1 ? face.sign : 0;
		float nz = face.axis == 2 ? face.sign : 0;
		CubeDraw.quad(box.model, box.normal, consumer, light, overlay, 1.0f, xs, ys, zs, us, vs, nx, ny, nz, normalScratch);
	}

	/** X faces store (y, z) in (u, v). Y faces store (x, z). Z faces store (x, y). */
	private static void corners(float[] xs, float[] ys, float[] zs, float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		xs[0] = ax;
		ys[0] = ay;
		zs[0] = az;
		xs[1] = bx;
		ys[1] = by;
		zs[1] = bz;
		xs[2] = cx;
		ys[2] = cy;
		zs[2] = cz;
		xs[3] = dx;
		ys[3] = dy;
		zs[3] = dz;
	}

	/** One texel per model pixel, anchored so neighbouring cubes continue the same cloth. */
	private static void coatUv(Box box, Face face, float x, float y, float z, float[] us, float[] vs, int index) {
		Mat mat = box.coat;
		float across = (face.axis == 0 ? z : x) * PX - face.acrossOff;
		float up;
		if (face.axis == 1) {
			up = z * PX - face.upOff;
		} else {
			up = face.upOff - y * PX;
		}
		us[index] = (mat.left() + across) / Mat.ATLAS;
		vs[index] = (mat.top() + up) / Mat.ATLAS;
	}

	/**
	 * The eye art stretched over the face, upright, with the art's left edge toward the front of the
	 * head. A front eye left of centre is mirrored so the pair matches. Blinking swaps in the lid.
	 */
	private static void eyeUv(Box box, Face face, float x, float y, float z, float[] us, float[] vs, int index, boolean blink) {
		Rect full = face.full;
		float across;
		float span;
		float up;
		float upSpan;
		if (face.axis == 0) {
			across = z - full.v0;
			span = full.v1 - full.v0;
			up = y - full.u0;
			upSpan = full.u1 - full.u0;
		} else {
			across = x - full.u0;
			span = full.u1 - full.u0;
			up = y - full.v0;
			upSpan = full.v1 - full.v0;
		}
		boolean mirror = face.axis == 2 && (box.x0 + box.x1) * 0.5f < 0.0f;
		int wide = Math.round(span * PX);
		int[] region = blink ? EYE_SHUT : wide >= 4 ? EYE_4 : wide == 3 ? EYE_3 : EYE_2;
		float along = span == 0 ? 0.0f : across / span;
		if (mirror) {
			along = 1.0f - along;
		}
		float rise = upSpan == 0 ? 0.0f : up / upSpan;
		Mat mat = box.eye;
		us[index] = (mat.left() + region[0] + (region[2] - region[0]) * along) / Mat.ATLAS;
		vs[index] = (mat.top() + region[3] - (region[3] - region[1]) * rise) / Mat.ATLAS;
	}

	private static long signature(List<Box> boxes) {
		long hash = boxes.size();
		for (Box box : boxes) {
			hash = hash * 31 + Float.floatToIntBits(box.x0);
			hash = hash * 31 + Float.floatToIntBits(box.y0);
			hash = hash * 31 + Float.floatToIntBits(box.z0);
			hash = hash * 31 + Float.floatToIntBits(box.x1);
			hash = hash * 31 + Float.floatToIntBits(box.y1);
			hash = hash * 31 + Float.floatToIntBits(box.z1);
			hash = hash * 31 + box.group;
			hash = hash * 31 + box.coat.ordinal();
			hash = hash * 31 + box.eyeFace;
		}
		return hash;
	}

	/** The visible pieces of one cube layout. Valid for any pose of the same layout. */
	static final class Plan {
		private final long signature;
		private final List<Face> faces;

		Plan(long signature, List<Face> faces) {
			this.signature = signature;
			this.faces = faces;
		}

		boolean matches(List<Box> boxes) {
			return signature == signature(boxes);
		}
	}

	private static final class Box {
		final float x0;
		final float y0;
		final float z0;
		final float x1;
		final float y1;
		final float z1;
		final Mat coat;
		final int eyeFace;
		final Mat eye;
		final int group;
		final int order;
		final Matrix4f model;
		final Matrix3f normal;

		Box(float x0, float y0, float z0, float x1, float y1, float z1, Mat coat, int eyeFace, Mat eye, int group, int order,
				Matrix4f model, Matrix3f normal) {
			this.x0 = x0;
			this.y0 = y0;
			this.z0 = z0;
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.coat = coat;
			this.eyeFace = eyeFace;
			this.eye = eye;
			this.group = group;
			this.order = order;
			this.model = model;
			this.normal = normal;
		}

		float min(int axis) {
			return axis == 0 ? x0 : axis == 1 ? y0 : z0;
		}

		float max(int axis) {
			return axis == 0 ? x1 : axis == 1 ? y1 : z1;
		}
	}

	private static final class Face {
		final int box;
		final int axis;
		final int sign;
		final float plane;
		final Rect full;
		final boolean eye;
		final List<Rect> rects;
		final float acrossOff;
		final float upOff;

		Face(int box, int axis, int sign, float plane, Rect full, boolean eye, List<Rect> rects, float acrossOff, float upOff) {
			this.box = box;
			this.axis = axis;
			this.sign = sign;
			this.plane = plane;
			this.full = full;
			this.eye = eye;
			this.rects = rects;
			this.acrossOff = acrossOff;
			this.upOff = upOff;
		}
	}

	private static final class Rect {
		final float u0;
		final float v0;
		final float u1;
		final float v1;

		Rect(float u0, float v0, float u1, float v1) {
			this.u0 = u0;
			this.v0 = v0;
			this.u1 = u1;
			this.v1 = v1;
		}
	}
}
