package tk.darrow.shamanicmounts.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
 * {@link Plan} from one frame and hand it back on the next. Culling never looks past a part, so each part
 * is cut on its own and the cut is kept by the part's cubes alone: a layout that changes in one part (a
 * wing folding, a tail fanning) re-cuts nothing, and mounts built alike share their cuts. A plan is baked: every surviving piece
 * already holds its corners and texture coordinates, so a frame only transforms and submits them.
 * Pieces of one face that line up are merged first, which draws the same surface with fewer quads.
 * Cubes, pose snapshots, and the pen's transform stack are pooled, so a frame allocates nothing per cube
 * or per joint.
 *
 * <p>In the world a mount is drawn with back faces culled, so a face turned away from the camera is not
 * submitted at all: the GPU would throw it away anyway. Each part's camera position is worked out once per
 * frame, and a face is skipped only when the camera is clearly behind its plane.
 */
final class SolidDraw {
	private static final float EPS = 0.0004f;
	private static final float PX = 16.0f;
	/** Eye art regions in an eye cell, as texel rectangles: 2×2, 3×2, 4×3, and the closed lid. */
	private static final int[] EYE_2 = { 0, 0, 8, 8 };
	private static final int[] EYE_3 = { 8, 0, 20, 8 };
	private static final int[] EYE_4 = { 0, 8, 16, 20 };
	private static final int[] EYE_SHUT = { 16, 8, 24, 16 };

	/** Render statistics for the live harness: parts cut, plans put together from cut parts, and quads emitted. */
	static long replans;
	static long assembles;
	static long quads;
	/** Set by the live harness; a player's game never counts or times. */
	static boolean stats;
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
	/**
	 * How far behind a face's plane the camera must be, in blocks, before the face is skipped. View bobbing
	 * moves the true eye up to about a tenth of a block from the camera, so this keeps every face the GPU
	 * could still show.
	 */
	private static final float BEHIND = 0.25f;

	/** The pen's transform stack, pooled. Each level is a model matrix and its normal matrix. */
	private Matrix4f[] stackModel = new Matrix4f[32];
	private Matrix3f[] stackNormal = new Matrix3f[32];
	private int top = -1;
	private final Quaternionf turn = new Quaternionf();

	/** Per pose snapshot, filled at flush: unit normals of its three axes, and the camera in its own space. */
	private float[] axisNormals = new float[64 * 9];
	private float[] eyes = new float[64 * 3];
	/** Per pose and axis, how many local units make one block across that axis' planes. */
	private float[] unitsPerBlock = new float[64 * 3];
	private boolean[] shown = new boolean[256];
	private final Matrix4f inverse = new Matrix4f();

	/** Start a pen's stack at {@code base}. */
	void begin(PoseStack.Pose base) {
		top = 0;
		ensureStack(0);
		stackModel[0].set(base.pose());
		stackNormal[0].set(base.normal());
	}

	private void ensureStack(int level) {
		if (level >= stackModel.length) {
			stackModel = java.util.Arrays.copyOf(stackModel, level * 2);
			stackNormal = java.util.Arrays.copyOf(stackNormal, level * 2);
		}
		if (stackModel[level] == null) {
			stackModel[level] = new Matrix4f();
			stackNormal[level] = new Matrix3f();
		}
	}

	/** The same as {@link PoseStack#pushPose()}, into a pooled level. */
	void push() {
		ensureStack(top + 1);
		stackModel[top + 1].set(stackModel[top]);
		stackNormal[top + 1].set(stackNormal[top]);
		top++;
	}

	void pop() {
		top--;
	}

	/** The same arithmetic as {@link PoseStack#translate(float, float, float)}. */
	void translate(float x, float y, float z) {
		stackModel[top].translate(x, y, z);
	}

	/** The same arithmetic as {@code mulPose(Axis.XP.rotationDegrees(degrees))}, without a new quaternion. */
	void rotateX(float degrees) {
		rotate(turn.rotationX(degrees * (float) (Math.PI / 180.0)));
	}

	void rotateY(float degrees) {
		rotate(turn.rotationY(degrees * (float) (Math.PI / 180.0)));
	}

	void rotateZ(float degrees) {
		rotate(turn.rotationZ(degrees * (float) (Math.PI / 180.0)));
	}

	private void rotate(Quaternionf quaternion) {
		stackModel[top].rotate(quaternion);
		stackNormal[top].rotate(quaternion);
	}

	/** The same arithmetic as {@link PoseStack#scale(float, float, float)}. */
	void scale(float x, float y, float z) {
		stackModel[top].scale(x, y, z);
		if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
			if (x < 0.0F || y < 0.0F || z < 0.0F) {
				stackNormal[top].scale(Math.signum(x), Math.signum(y), Math.signum(z));
			}
		} else {
			stackNormal[top].scale(1.0F / x, 1.0F / y, 1.0F / z);
		}
	}

	/** A cube under the pen's current transform. */
	void add(int group, float x, float y, float z, float w, float h, float d, Mat coat, int eyeFace, Mat eye) {
		add(stackModel[top], stackNormal[top], group, x, y, z, w, h, d, coat, eyeFace, eye);
	}

	/** Coordinates are blocks in Minecraft axes: x right, y up, z toward the tail. */
	void add(PoseStack pose, int group, float x, float y, float z, float w, float h, float d, Mat coat, int eyeFace, Mat eye) {
		add(pose.last().pose(), pose.last().normal(), group, x, y, z, w, h, d, coat, eyeFace, eye);
	}

	private void add(Matrix4f model, Matrix3f normal, int group, float x, float y, float z, float w, float h, float d, Mat coat,
			int eyeFace, Mat eye) {
		if (w < EPS || h < EPS || d < EPS) {
			return;
		}
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
			normals[poses].set(normal);
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
		if (stats) {
			boxesAdded++;
		}
		count++;
	}

	/**
	 * Draw every visible face. {@code cached} may be null or stale; the plan actually used is returned.
	 * With a {@code glow} supplier the eye faces are drawn again, full bright, for eyes that shine at night.
	 */
	Plan flush(VertexConsumer consumer, int light, int overlay, Plan cached, boolean blink, Supplier<VertexConsumer> glow) {
		Plan[] plans = { cached };
		flush(consumer, light, overlay, plans, 0, -1, blink, glow, false);
		return plans[0];
	}

	/**
	 * Draw every visible face with the plan kept in {@code plans[slot]}. A layout that alternates with another, like
	 * a wing that folds and opens, keeps its other plan in {@code plans[spare]} (-1 for none), and the two swap rather
	 * than being cut again. With {@code cull}, faces turned away from the camera are left out: only for a draw whose
	 * back faces are culled anyway and whose pose space has the camera at its origin.
	 */
	void flush(VertexConsumer consumer, int light, int overlay, Plan[] plans, int slot, int spare, boolean blink,
			Supplier<VertexConsumer> glow, boolean cull) {
		long start = stats ? System.nanoTime() : 0L;
		long sig = signature();
		if (stats) {
			signNanos += System.nanoTime() - start;
		}
		Plan plan = plans[slot];
		if (plan == null || plan.signature != sig) {
			Plan other = spare >= 0 ? plans[spare] : null;
			if (other != null && other.signature == sig) {
				plans[spare] = plan;
				plan = other;
			} else {
				if (spare >= 0) {
					plans[spare] = plan;
				}
				plan = plan(sig);
			}
			plans[slot] = plan;
		}
		prepare(cull);
		Face[] faces = plan.faces;
		if (shown.length < faces.length) {
			shown = new boolean[faces.length * 2];
		}
		for (int i = 0; i < faces.length; i++) {
			Face face = faces[i];
			int pose = boxes[face.box].pose;
			boolean show = !cull || !behind(face, pose);
			shown[i] = show;
			if (show) {
				emit(face, pose, face.eye && blink ? face.shut : face.uv, consumer, light, overlay);
			}
		}
		// The glow buffer is fetched only now: in a buffer source without its own glow buffer, asking for it ends
		// the coat buffer.
		if (glow != null && !blink && plan.eyeFaces.length > 0) {
			VertexConsumer shine = null;
			for (int i : plan.eyeFaces) {
				if (shown[i]) {
					if (shine == null) {
						shine = glow.get();
					}
					Face face = faces[i];
					emit(face, boxes[face.box].pose, face.uv, shine, 0xF000F0, overlay);
				}
			}
		}
		count = 0;
		poses = 0;
		top = -1;
		if (stats) {
			flushNanos += System.nanoTime() - start;
		}
	}

	/** Each pose's unit axis normals, and with culling its camera position and scale, once per frame. */
	private void prepare(boolean cull) {
		if (axisNormals.length < poses * 9) {
			axisNormals = new float[poses * 18];
			eyes = new float[poses * 6];
			unitsPerBlock = new float[poses * 6];
		}
		Vector3f n = normalScratch;
		for (int p = 0; p < poses; p++) {
			Matrix3f normal = normals[p];
			for (int axis = 0; axis < 3; axis++) {
				// Exactly what transforming the face's unit normal and normalizing it gives; the opposite face negates it.
				normal.transform(n.set(axis == 0 ? 1f : 0f, axis == 1 ? 1f : 0f, axis == 2 ? 1f : 0f));
				float length = n.length();
				if (length > 1.0e-6f) {
					n.div(length);
				}
				axisNormals[p * 9 + axis * 3] = n.x;
				axisNormals[p * 9 + axis * 3 + 1] = n.y;
				axisNormals[p * 9 + axis * 3 + 2] = n.z;
			}
			if (cull) {
				// The camera sits at the origin of pose space; in the part's own space it is the inverse's translation.
				// A row of the inverse is how fast that local coordinate changes per block of distance.
				Matrix4f inv = models[p].invertAffine(inverse);
				eyes[p * 3] = inv.m30();
				eyes[p * 3 + 1] = inv.m31();
				eyes[p * 3 + 2] = inv.m32();
				unitsPerBlock[p * 3] = (float) Math.sqrt(inv.m00() * inv.m00() + inv.m10() * inv.m10() + inv.m20() * inv.m20());
				unitsPerBlock[p * 3 + 1] = (float) Math.sqrt(inv.m01() * inv.m01() + inv.m11() * inv.m11() + inv.m21() * inv.m21());
				unitsPerBlock[p * 3 + 2] = (float) Math.sqrt(inv.m02() * inv.m02() + inv.m12() * inv.m12() + inv.m22() * inv.m22());
			}
		}
	}

	/** Whether the camera is clearly behind this face's plane, so the face looks away from it. */
	private boolean behind(Face face, int pose) {
		int k = pose * 3 + face.axis;
		float out = (eyes[k] - face.plane) * face.sign;
		return out < -BEHIND * unitsPerBlock[k];
	}

	/** Submit a face's baked corners under its pose, with the pose's normal for that face. */
	private void emit(Face face, int pose, float[] uv, VertexConsumer consumer, int light, int overlay) {
		Matrix4f model = models[pose];
		int k = pose * 9 + face.axis * 3;
		float nx = axisNormals[k] * face.sign;
		float ny = axisNormals[k + 1] * face.sign;
		float nz = axisNormals[k + 2] * face.sign;
		float[] pos = face.pos;
		int corners = pos.length / 3;
		Vector3f at = positionScratch;
		for (int i = 0; i < corners; i++) {
			model.transformPosition(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2], at);
			// One call per vertex: the buffer's fast path for the entity format writes it all at once.
			consumer.addVertex(at.x, at.y, at.z, WHITE, uv[i * 2], uv[i * 2 + 1], overlay, light, nx, ny, nz);
		}
		if (stats) {
			quads += corners / 4;
		}
	}

	/** How many cut parts are kept for reuse. A part is a few hundred bytes to a few kilobytes. */
	private static final int PART_LIMIT = 4096;
	/** Cut parts by their cubes, least recently used first. Mounts are drawn on the render thread only. */
	private static final java.util.LinkedHashMap<PartKey, Part> PARTS = new java.util.LinkedHashMap<>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<PartKey, Part> eldest) {
			return size() > PART_LIMIT;
		}
	};
	private final PartKey probe = new PartKey();

	/**
	 * This frame's layout as a plan, put together from each part's cut. A part already cut, for this mount or any
	 * other, is reused; only a part never seen is cut. Faces come out in cube order, as a whole cut would give them.
	 */
	private Plan plan(long signature) {
		assembles++;
		int groups = 0;
		for (int i = 0; i < count; i++) {
			groups = Math.max(groups, boxes[i].group + 1);
		}
		int[] start = new int[groups + 1];
		for (int i = 0; i < count; i++) {
			start[boxes[i].group + 1]++;
		}
		for (int g = 0; g < groups; g++) {
			start[g + 1] += start[g];
		}
		int[] fill = start.clone();
		int[] members = new int[count];
		int[] local = new int[count];
		for (int i = 0; i < count; i++) {
			int g = boxes[i].group;
			local[i] = fill[g] - start[g];
			members[fill[g]++] = i;
		}
		Part[] parts = new Part[groups];
		for (int g = 0; g < groups; g++) {
			if (start[g + 1] > start[g]) {
				parts[g] = part(members, start[g], start[g + 1]);
			}
		}
		List<Face> faces = new ArrayList<>(count * 4);
		for (int i = 0; i < count; i++) {
			Part part = parts[boxes[i].group];
			for (int f = part.first[local[i]]; f < part.first[local[i] + 1]; f++) {
				faces.add(part.faces[f].at(i));
			}
		}
		int eyeCount = 0;
		for (Face face : faces) {
			eyeCount += face.eye ? 1 : 0;
		}
		int[] eyeFaces = new int[eyeCount];
		for (int i = 0, e = 0; i < faces.size(); i++) {
			if (faces.get(i).eye) {
				eyeFaces[e++] = i;
			}
		}
		return new Plan(signature, faces.toArray(new Face[0]), eyeFaces);
	}

	/** The cut of the part whose cubes are {@code boxes[members[from..to)]}, from the shared store or cut now. */
	private Part part(int[] members, int from, int to) {
		probe.set(boxes, members, from, to);
		Part part = PARTS.get(probe);
		if (part == null) {
			part = cut(members, from, to);
			PARTS.put(probe.copy(), part);
		}
		return part;
	}

	/** Cut one part. Its faces name their cube by its place in the part. */
	private Part cut(int[] members, int from, int to) {
		replans++;
		int n = to - from;
		List<Face> faces = new ArrayList<>(n * 4);
		int[] first = new int[n + 1];
		for (int l = 0; l < n; l++) {
			first[l] = faces.size();
			Box box = boxes[members[from + l]];
			for (int axis = 0; axis < 3; axis++) {
				face(faces, box, l, members, from, to, axis, 1);
				face(faces, box, l, members, from, to, axis, -1);
			}
		}
		first[n] = faces.size();
		return new Part(faces.toArray(new Face[0]), first);
	}

	private void face(List<Face> faces, Box box, int local, int[] members, int from, int to, int axis, int sign) {
		float plane = sign > 0 ? box.max(axis) : box.min(axis);
		int a = axis == 0 ? 1 : 0;
		int b = axis == 2 ? 1 : 2;
		Rect full = new Rect(box.min(a), box.min(b), box.max(a), box.max(b));
		List<Rect> cuts = new ArrayList<>();
		float outside = plane + sign * EPS;
		for (int i = from; i < to; i++) {
			Box other = boxes[members[i]];
			if (other == box) {
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
		faces.add(new Face(local, axis, sign, plane, eye, pos, uv, shut));
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
		/** Which faces are eyes, for the glow pass. */
		private final int[] eyeFaces;

		Plan(long signature, Face[] faces, int[] eyeFaces) {
			this.signature = signature;
			this.faces = faces;
			this.eyeFaces = eyeFaces;
		}
	}

	/** One part's cut: its faces in cube order, and where each cube's faces start ({@code first[n]} is the end). */
	private static final class Part {
		final Face[] faces;
		final int[] first;

		Part(Face[] faces, int[] first) {
			this.faces = faces;
			this.first = first;
		}
	}

	/** A part's cubes in order: corners, coat, eye face, and eye. Everything its cut depends on. */
	private static final class PartKey {
		private static final int PER_BOX = 9;
		private int[] data = new int[64 * PER_BOX];
		private int length;
		private int hash;

		void set(Box[] boxes, int[] members, int from, int to) {
			length = (to - from) * PER_BOX;
			if (data.length < length) {
				data = new int[length * 2];
			}
			int h = 1;
			int k = 0;
			for (int i = from; i < to; i++) {
				Box box = boxes[members[i]];
				data[k++] = Float.floatToIntBits(box.x0);
				data[k++] = Float.floatToIntBits(box.y0);
				data[k++] = Float.floatToIntBits(box.z0);
				data[k++] = Float.floatToIntBits(box.x1);
				data[k++] = Float.floatToIntBits(box.y1);
				data[k++] = Float.floatToIntBits(box.z1);
				data[k++] = box.coat.ordinal();
				data[k++] = box.eyeFace;
				data[k++] = box.eye == null ? -1 : box.eye.ordinal();
			}
			for (int i = 0; i < length; i++) {
				h = 31 * h + data[i];
			}
			hash = h;
		}

		PartKey copy() {
			PartKey key = new PartKey();
			key.data = java.util.Arrays.copyOf(data, length);
			key.length = length;
			key.hash = hash;
			return key;
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof PartKey key && key.length == length && key.hash == hash
					&& java.util.Arrays.equals(data, 0, length, key.data, 0, length);
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
		/** The face's axis (0 x, 1 y, 2 z), which way it looks along it (1 or -1), and where its plane lies. */
		final int axis;
		final int sign;
		final float plane;
		final boolean eye;
		final float[] pos;
		final float[] uv;
		final float[] shut;

		/** The same face on cube {@code box} of a frame; the baked corners and coordinates are shared. */
		Face at(int box) {
			return new Face(box, axis, sign, plane, eye, pos, uv, shut);
		}

		Face(int box, int axis, int sign, float plane, boolean eye, float[] pos, float[] uv, float[] shut) {
			this.box = box;
			this.axis = axis;
			this.sign = sign;
			this.plane = plane;
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
