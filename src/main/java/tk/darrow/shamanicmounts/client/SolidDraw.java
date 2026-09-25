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
 * {@link Plan} from one frame and hand it back on the next. A plan is baked: every surviving piece
 * already holds its corners and texture coordinates, so a frame only transforms and submits them.
 * Pieces of one face that line up are merged first, which draws the same surface with fewer quads.
 * Cubes and pose snapshots are pooled, so a frame allocates nothing per cube.
 */
final class SolidDraw {
	private static final float EPS = 0.0004f;
	private static final float PX = 16.0f;
	/** Eye art regions in an eye cell, as texel rectangles: 2×2, 3×2, 4×3, and the closed lid. */
	private static final int[] EYE_2 = { 0, 0, 8, 8 };
	private static final int[] EYE_3 = { 8, 0, 20, 8 };
	private static final int[] EYE_4 = { 0, 8, 16, 20 };
	private static final int[] EYE_SHUT = { 16, 8, 24, 16 };

	/** Render statistics for the live harness: plan rebuilds and quads emitted since the last read. */
	static long replans;
	static long quads;
	static long flushNanos;
	static long signNanos;
	static long boxesAdded;

	/** This frame's cubes, pooled across frames. Only the first {@code count} are live. */
	private Box[] boxes = new Box[256];
	private int count;
	/** Pose snapshots, pooled; cubes under one transform share one. */
	private Matrix4f[] models = new Matrix4f[64];
	private Matrix3f[] normals = new Matrix3f[64];
	private int poses;
	private final Vector3f normalScratch = new Vector3f();
	private final Vector3f positionScratch = new Vector3f();
	/** Opaque white: the atlas carries the colour. */
	private static final int WHITE = 0xFFFFFFFF;

	/** Coordinates are blocks in Minecraft axes: x right, y up, z toward the tail. */
	void add(PoseStack pose, int group, float x, float y, float z, float w, float h, float d, Mat coat, int eyeFace, Mat eye) {
		if (w < EPS || h < EPS || d < EPS) {
			return;
		}
		Matrix4f model = pose.last().pose();
		if (poses == 0 || !models[poses - 1].equals(model)) {
			if (poses == models.length) {
				models = java.util.Arrays.copyOf(models, poses * 2);
				normals = java.util.Arrays.copyOf(normals, poses * 2);
			}
			if (models[poses] == null) {
				models[poses] = new Matrix4f();
				normals[poses] = new Matrix3f();
			}
			models[poses].set(model);
			normals[poses].set(pose.last().normal());
			poses++;
		}
		if (count == boxes.length) {
			boxes = java.util.Arrays.copyOf(boxes, count * 2);
		}
		Box box = boxes[count];
		if (box == null) {
			box = new Box();
			boxes[count] = box;
		}
		box.set(x, y, z, x + w, y + h, z + d, coat, eyeFace, eye, group, count, poses - 1);
		boxesAdded++;
		count++;
	}

	/**
	 * Draw every visible face. {@code cached} may be null or stale; the plan actually used is returned.
	 * With a {@code glow} supplier the eye faces are drawn again, full bright, for eyes that shine at night.
	 */
	Plan flush(VertexConsumer consumer, int light, int overlay, Plan cached, boolean blink, Supplier<VertexConsumer> glow) {
		long start = System.nanoTime();
		long sig = signature();
		signNanos += System.nanoTime() - start;
		Plan plan = cached != null && cached.signature == sig ? cached : plan();
		for (Face face : plan.faces) {
			emit(face, face.eye && blink ? face.shut : face.uv, consumer, light, overlay);
		}
		// The glow buffer is fetched only now: asking the buffer source for it ends the coat buffer.
		if (glow != null && !blink && plan.hasEyes) {
			VertexConsumer shine = glow.get();
			for (Face face : plan.faces) {
				if (face.eye) {
					emit(face, face.uv, shine, 0xF000F0, overlay);
				}
			}
		}
		count = 0;
		poses = 0;
		flushNanos += System.nanoTime() - start;
		return plan;
	}

	/** Transform the face's normal once, then submit its baked corners. */
	private void emit(Face face, float[] uv, VertexConsumer consumer, int light, int overlay) {
		Box box = boxes[face.box];
		Matrix4f model = models[box.pose];
		Vector3f n = normals[box.pose].transform(normalScratch.set(face.nx, face.ny, face.nz));
		float length = n.length();
		if (length > 1.0e-6f) {
			n.div(length);
		}
		float nx = n.x;
		float ny = n.y;
		float nz = n.z;
		float[] pos = face.pos;
		int corners = pos.length / 3;
		Vector3f at = positionScratch;
		for (int i = 0; i < corners; i++) {
			model.transformPosition(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2], at);
			// One call per vertex: the buffer's fast path for the entity format writes it all at once.
			consumer.addVertex(at.x, at.y, at.z, WHITE, uv[i * 2], uv[i * 2 + 1], overlay, light, nx, ny, nz);
		}
		quads += corners / 4;
	}

	private Plan plan() {
		replans++;
		List<Face> faces = new ArrayList<>(count * 4);
		for (int i = 0; i < count; i++) {
			Box box = boxes[i];
			for (int axis = 0; axis < 3; axis++) {
				face(faces, box, axis, 1);
				face(faces, box, axis, -1);
			}
		}
		boolean eyes = false;
		for (Face face : faces) {
			eyes |= face.eye;
		}
		return new Plan(signature(), faces.toArray(new Face[0]), eyes);
	}

	private void face(List<Face> faces, Box box, int axis, int sign) {
		float plane = sign > 0 ? box.max(axis) : box.min(axis);
		int a = axis == 0 ? 1 : 0;
		int b = axis == 2 ? 1 : 2;
		Rect full = new Rect(box.min(a), box.min(b), box.max(a), box.max(b));
		List<Rect> cuts = new ArrayList<>();
		float outside = plane + sign * EPS;
		for (int i = 0; i < count; i++) {
			Box other = boxes[i];
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
			if (cut != null) {
				cuts.add(cut);
			}
		}
		List<Rect> rects = visible(full, cuts);
		if (rects.isEmpty()) {
			return;
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

		// Bake the corners and texture coordinates of every piece once.
		float[] pos = new float[rects.size() * 12];
		float[] uv = new float[rects.size() * 8];
		float[] shut = eye ? new float[rects.size() * 8] : null;
		float[] xs = new float[4];
		float[] ys = new float[4];
		float[] zs = new float[4];
		for (int r = 0; r < rects.size(); r++) {
			corners(rects.get(r), axis, sign, plane, xs, ys, zs);
			for (int i = 0; i < 4; i++) {
				int k = r * 4 + i;
				pos[k * 3] = xs[i];
				pos[k * 3 + 1] = ys[i];
				pos[k * 3 + 2] = zs[i];
				if (eye) {
					eyeUv(box, axis, full, xs[i], ys[i], zs[i], uv, k, false);
					eyeUv(box, axis, full, xs[i], ys[i], zs[i], shut, k, true);
				} else {
					coatUv(box.coat, axis, acrossOff, upOff, xs[i], ys[i], zs[i], uv, k);
				}
			}
		}
		faces.add(new Face(box.order, axis == 0 ? sign : 0, axis == 1 ? sign : 0, axis == 2 ? sign : 0, eye, pos, uv, shut));
	}

	/**
	 * What is left of a face once every cut is taken out, as few rectangles as a grid allows: the
	 * face is diced along every cut edge, hidden cells dropped, and the rest joined into runs along
	 * each row, then rows with the same run stacked. Same surface as cutting piece by piece, fewer quads.
	 */
	private static List<Rect> visible(Rect full, List<Rect> cuts) {
		if (cuts.isEmpty()) {
			List<Rect> whole = new ArrayList<>(1);
			whole.add(full);
			return whole;
		}
		float[] us = edges(full.u0, full.u1, cuts, true);
		float[] vs = edges(full.v0, full.v1, cuts, false);
		List<Rect> runs = new ArrayList<>();
		for (int j = 0; j + 1 < vs.length; j++) {
			float cv = (vs[j] + vs[j + 1]) * 0.5f;
			int start = -1;
			for (int i = 0; i + 1 < us.length; i++) {
				float cu = (us[i] + us[i + 1]) * 0.5f;
				boolean shown = true;
				for (Rect cut : cuts) {
					if (cu > cut.u0 && cu < cut.u1 && cv > cut.v0 && cv < cut.v1) {
						shown = false;
						break;
					}
				}
				if (shown && start < 0) {
					start = i;
				} else if (!shown && start >= 0) {
					runs.add(new Rect(us[start], vs[j], us[i], vs[j + 1]));
					start = -1;
				}
			}
			if (start >= 0) {
				runs.add(new Rect(us[start], vs[j], us[us.length - 1], vs[j + 1]));
			}
		}
		return merge(runs);
	}

	/** The distinct cut edges inside a face along one axis, face edges included, in order. */
	private static float[] edges(float lo, float hi, List<Rect> cuts, boolean u) {
		float[] raw = new float[2 + cuts.size() * 2];
		int n = 0;
		raw[n++] = lo;
		raw[n++] = hi;
		for (Rect cut : cuts) {
			float a = u ? cut.u0 : cut.v0;
			float b = u ? cut.u1 : cut.v1;
			if (a > lo + EPS && a < hi - EPS) {
				raw[n++] = a;
			}
			if (b > lo + EPS && b < hi - EPS) {
				raw[n++] = b;
			}
		}
		java.util.Arrays.sort(raw, 0, n);
		float[] out = new float[n];
		int m = 0;
		for (int i = 0; i < n; i++) {
			if (m == 0 || raw[i] - out[m - 1] > EPS) {
				out[m++] = raw[i];
			}
		}
		return java.util.Arrays.copyOf(out, m);
	}

	/** Join pieces of one face that share a full edge, so the same surface needs fewer quads. */
	private static List<Rect> merge(List<Rect> rects) {
		if (rects.size() < 2) {
			return rects;
		}
		List<Rect> out = new ArrayList<>(rects);
		boolean joined = true;
		while (joined) {
			joined = false;
			search:
			for (int i = 0; i < out.size(); i++) {
				for (int j = i + 1; j < out.size(); j++) {
					Rect joinedRect = join(out.get(i), out.get(j));
					if (joinedRect != null) {
						out.set(i, joinedRect);
						out.remove(j);
						joined = true;
						break search;
					}
				}
			}
		}
		return out;
	}

	private static Rect join(Rect a, Rect b) {
		boolean sameU = Math.abs(a.u0 - b.u0) < EPS && Math.abs(a.u1 - b.u1) < EPS;
		if (sameU && (Math.abs(a.v1 - b.v0) < EPS || Math.abs(b.v1 - a.v0) < EPS)) {
			return new Rect(a.u0, Math.min(a.v0, b.v0), a.u1, Math.max(a.v1, b.v1));
		}
		boolean sameV = Math.abs(a.v0 - b.v0) < EPS && Math.abs(a.v1 - b.v1) < EPS;
		if (sameV && (Math.abs(a.u1 - b.u0) < EPS || Math.abs(b.u1 - a.u0) < EPS)) {
			return new Rect(Math.min(a.u0, b.u0), a.v0, Math.max(a.u1, b.u1), a.v1);
		}
		return null;
	}

	/** The four corners of a piece, wound the same way for every face of an axis and sign. */
	private static void corners(Rect rect, int axis, int sign, float p, float[] xs, float[] ys, float[] zs) {
		float u0 = rect.u0;
		float v0 = rect.v0;
		float u1 = rect.u1;
		float v1 = rect.v1;
		switch (axis) {
			case 0 -> {
				if (sign > 0) {
					set(xs, ys, zs, p, u0, v1, p, u0, v0, p, u1, v0, p, u1, v1);
				} else {
					set(xs, ys, zs, p, u0, v0, p, u0, v1, p, u1, v1, p, u1, v0);
				}
			}
			case 1 -> {
				if (sign > 0) {
					set(xs, ys, zs, u0, p, v1, u1, p, v1, u1, p, v0, u0, p, v0);
				} else {
					set(xs, ys, zs, u0, p, v0, u1, p, v0, u1, p, v1, u0, p, v1);
				}
			}
			default -> {
				if (sign > 0) {
					set(xs, ys, zs, u0, v0, p, u1, v0, p, u1, v1, p, u0, v1, p);
				} else {
					set(xs, ys, zs, u1, v0, p, u0, v0, p, u0, v1, p, u1, v1, p);
				}
			}
		}
	}

	private static void set(float[] xs, float[] ys, float[] zs, float ax, float ay, float az, float bx, float by, float bz,
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

	/** One texel per model pixel, anchored so neighbouring cubes continue the same cloth. */
	private static void coatUv(Mat mat, int axis, float acrossOff, float upOff, float x, float y, float z, float[] uv, int k) {
		float across = (axis == 0 ? z : x) * PX - acrossOff;
		float up = axis == 1 ? z * PX - upOff : upOff - y * PX;
		uv[k * 2] = (mat.left() + across) / Mat.ATLAS;
		uv[k * 2 + 1] = (mat.top() + up) / Mat.ATLAS;
	}

	/**
	 * The eye art stretched over the face, upright, with the art's left edge toward the front of the
	 * head. A front eye left of centre is mirrored so the pair matches. Blinking swaps in the lid.
	 */
	private static void eyeUv(Box box, int axis, Rect full, float x, float y, float z, float[] uv, int k, boolean blink) {
		float across;
		float span;
		float up;
		float upSpan;
		if (axis == 0) {
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
		boolean mirror = axis == 2 && (box.x0 + box.x1) * 0.5f < 0.0f;
		int wide = Math.round(span * PX);
		int[] region = blink ? EYE_SHUT : wide >= 4 ? EYE_4 : wide == 3 ? EYE_3 : EYE_2;
		float along = span == 0 ? 0.0f : across / span;
		if (mirror) {
			along = 1.0f - along;
		}
		float rise = upSpan == 0 ? 0.0f : up / upSpan;
		Mat mat = box.eye;
		uv[k * 2] = (mat.left() + region[0] + (region[2] - region[0]) * along) / Mat.ATLAS;
		uv[k * 2 + 1] = (mat.top() + region[3] - (region[3] - region[1]) * rise) / Mat.ATLAS;
	}

	/** Everything a plan depends on: every cube's corners, part, coat, and eye. */
	private long signature() {
		long hash = count;
		for (int i = 0; i < count; i++) {
			Box box = boxes[i];
			hash = hash * 31 + Float.floatToIntBits(box.x0);
			hash = hash * 31 + Float.floatToIntBits(box.y0);
			hash = hash * 31 + Float.floatToIntBits(box.z0);
			hash = hash * 31 + Float.floatToIntBits(box.x1);
			hash = hash * 31 + Float.floatToIntBits(box.y1);
			hash = hash * 31 + Float.floatToIntBits(box.z1);
			hash = hash * 31 + box.group;
			hash = hash * 31 + box.coat.ordinal();
			hash = hash * 31 + box.eyeFace;
			hash = hash * 31 + (box.eye == null ? -1 : box.eye.ordinal());
		}
		return hash;
	}

	/** The visible pieces of one cube layout, baked. Valid for any pose of the same layout. */
	static final class Plan {
		private final long signature;
		private final Face[] faces;
		private final boolean hasEyes;

		Plan(long signature, Face[] faces, boolean hasEyes) {
			this.signature = signature;
			this.faces = faces;
			this.hasEyes = hasEyes;
		}
	}

	/** One cube of the current frame. Pooled: its fields are rewritten every frame. */
	private static final class Box {
		float x0;
		float y0;
		float z0;
		float x1;
		float y1;
		float z1;
		Mat coat;
		int eyeFace;
		Mat eye;
		int group;
		int order;
		int pose;

		void set(float x0, float y0, float z0, float x1, float y1, float z1, Mat coat, int eyeFace, Mat eye, int group,
				int order, int pose) {
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
			this.pose = pose;
		}

		float min(int axis) {
			return axis == 0 ? x0 : axis == 1 ? y0 : z0;
		}

		float max(int axis) {
			return axis == 0 ? x1 : axis == 1 ? y1 : z1;
		}
	}

	/** One face's surviving pieces: baked corners, coat or open-eye coordinates, and the closed lid's. */
	private static final class Face {
		final int box;
		final float nx;
		final float ny;
		final float nz;
		final boolean eye;
		final float[] pos;
		final float[] uv;
		final float[] shut;

		Face(int box, float nx, float ny, float nz, boolean eye, float[] pos, float[] uv, float[] shut) {
			this.box = box;
			this.nx = nx;
			this.ny = ny;
			this.nz = nz;
			this.eye = eye;
			this.pos = pos;
			this.uv = uv;
			this.shut = shut;
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
