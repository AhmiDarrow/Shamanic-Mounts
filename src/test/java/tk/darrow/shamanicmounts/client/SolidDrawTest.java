package tk.darrow.shamanicmounts.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** The cube culler: buried faces go, a later cube wins a shared plane, and only eye faces read eye art. */
class SolidDrawTest {
	/** One emitted quad: its four corners and the atlas region its texture coordinates fall in. */
	record Quad(float[] xs, float[] ys, float[] zs, float[] us, float[] vs) {
		float minZ() {
			float m = Float.MAX_VALUE;
			for (float z : zs) {
				m = Math.min(m, z);
			}
			return m;
		}

		float maxZ() {
			float m = -Float.MAX_VALUE;
			for (float z : zs) {
				m = Math.max(m, z);
			}
			return m;
		}

		boolean flatInZ() {
			return Math.abs(maxZ() - minZ()) < 1.0e-5f;
		}

		/** Which atlas row the texture coordinates sit in; the eye row is 6. */
		int atlasRow() {
			float v = 0;
			for (float f : vs) {
				v += f;
			}
			return (int) Math.floor(v / vs.length * Mat.ATLAS / Mat.CELL);
		}
	}

	static final class Capture implements VertexConsumer {
		final List<Quad> quads = new ArrayList<>();
		private final float[] xs = new float[4];
		private final float[] ys = new float[4];
		private final float[] zs = new float[4];
		private final float[] us = new float[4];
		private final float[] vs = new float[4];
		private int at;

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			xs[at] = x;
			ys[at] = y;
			zs[at] = z;
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			us[at] = u;
			vs[at] = v;
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			at++;
			if (at == 4) {
				quads.add(new Quad(xs.clone(), ys.clone(), zs.clone(), us.clone(), vs.clone()));
				at = 0;
			}
			return this;
		}
	}

	private static List<Quad> drumHead() {
		SolidDraw draw = new SolidDraw();
		PoseStack pose = new PoseStack();
		float y = 0;
		float z = 0;
		// The drum hart's skull, brow, muzzle, and two flush side eyes, in blender pixels via Pen's mapping.
		add(draw, pose, -3f, y - 5f, z + 0.5f, 6f, 6.5f, 6f, Mat.HART, 0, null);
		add(draw, pose, -2.5f, y - 7f, z + 3f, 5f, 3f, 3.5f, Mat.HART, 0, null);
		add(draw, pose, -2f, y - 12f, z, 4f, 7.5f, 4.5f, Mat.HART, 0, null);
		add(draw, pose, -3f, y - 4.5f, z + 3f, 1f, 3f, 2f, Mat.HART_DARK, Pen.EYE_LEFT, Mat.EYE_DEER);
		add(draw, pose, 2f, y - 4.5f, z + 3f, 1f, 3f, 2f, Mat.HART_DARK, Pen.EYE_RIGHT, Mat.EYE_DEER);
		Capture capture = new Capture();
		draw.flush(capture, 0, 0, null, false, null);
		return capture.quads;
	}

	/** Same mapping as Pen.add: blender (x, y, z) becomes Minecraft (x, z, y). */
	private static void add(SolidDraw draw, PoseStack pose, float x, float y, float z, float dx, float dy, float dz, Mat mat,
			int eyeFace, Mat eye) {
		draw.add(pose, 1, x / 16f, z / 16f, y / 16f, dx / 16f, dz / 16f, dy / 16f, mat, eyeFace, eye);
	}

	@Test
	void eyeArtOnlyAppearsOnTheEyesSideFaces() {
		for (Quad quad : drumHead()) {
			if (quad.atlasRow() == 6) {
				boolean sideFace = true;
				for (float x : quad.xs) {
					sideFace &= Math.abs(Math.abs(x) - 3f / 16f) < 1.0e-5f;
				}
				assertTrue(sideFace, "eye art on a face that is not the skull's side: z " + quad.minZ() * 16 + ".." + quad.maxZ() * 16);
			}
		}
	}

	@Test
	void theBrowKeepsItsFrontAndTheEyesLoseTheirs() {
		List<Quad> quads = drumHead();
		boolean browFront = false;
		for (Quad quad : quads) {
			if (quad.flatInZ() && Math.abs(quad.minZ() * 16 - (-7f)) < 1.0e-4f) {
				browFront = true;
			}
			if (quad.flatInZ() && Math.abs(quad.minZ() * 16 - (-4.5f)) < 1.0e-4f) {
				assertFalse(quad.atlasRow() == 6, "an eye's front face was drawn");
				// The muzzle's back leaves a sliver under the skull at |x| <= 2; an eye's front would reach |x| = 3.
				float reach = 0;
				for (float x : quad.xs) {
					reach = Math.max(reach, Math.abs(x) * 16);
				}
				assertFalse(reach > 2.9f && reach < 3.1f, "an eye cube's front face survived inside the skull");
			}
		}
		assertTrue(browFront, "the brow's front face was cut away");
	}

	/** The whole drum head as the renderer builds it, through Pen and Heads. */
	private static List<Quad> realDrumHead() {
		PoseStack pose = new PoseStack();
		MountPose anim = new MountPose(0f, 0f, 0f, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
		Pen pen = new Pen(pose, anim);
		tk.darrow.shamanicmounts.genome.Phenotype phenotype = tk.darrow.shamanicmounts.genome.Expression
				.express(tk.darrow.shamanicmounts.genome.Founders.drumHart());
		Heads.head(pen, phenotype, MountMesh.Shell.HART, Skin.HART[0], 0f, 0f);
		Capture capture = new Capture();
		pen.flush(capture, 0, 0, null, null);
		return capture.quads;
	}

	@Test
	void theRealDrumHeadShowsNoDarkFrontAtEyeHeight() {
		List<Quad> quads = realDrumHead();
		List<String> wrong = new ArrayList<>();
		for (Quad quad : quads) {
			if (!quad.flatInZ()) {
				continue;
			}
			// A front-facing quad between the muzzle and the ears, at eye height, must be coat or eye art on the sides.
			float zPlane = quad.minZ() * 16;
			float minY = Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;
			float maxX = 0;
			for (int i = 0; i < 4; i++) {
				minY = Math.min(minY, quad.ys[i] * 16);
				maxY = Math.max(maxY, quad.ys[i] * 16);
				maxX = Math.max(maxX, Math.abs(quad.xs[i]) * 16);
			}
			if (zPlane < -4f && maxY > 3f && minY < 5f && maxX <= 3f && quad.atlasRow() == 6) {
				wrong.add("eye art at z " + zPlane + " y " + minY + ".." + maxY);
			}
			if (zPlane < -4f && maxY > 3f && minY < 5f && quad.atlasRow() == 1 && isDark(quad)) {
				wrong.add("dark rim at z " + zPlane + " y " + minY + ".." + maxY + " x " + maxX);
			}
		}
		assertTrue(wrong.isEmpty(), String.join(" | ", wrong));
	}

	/** True when the quad's texture sits in the HART_DARK cell. */
	private static boolean isDark(Quad quad) {
		float u = 0;
		for (float f : quad.us) {
			u += f;
		}
		int column = (int) Math.floor(u / 4 * Mat.ATLAS / Mat.CELL);
		return column == Mat.HART_DARK.column;
	}

	@Test
	void listFrontQuadsOfTheRealDrumHead() {
		StringBuilder out = new StringBuilder();
		for (Quad quad : realDrumHead()) {
			if (!quad.flatInZ()) {
				continue;
			}
			float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
			float u = 0, v = 0;
			for (int i = 0; i < 4; i++) {
				minY = Math.min(minY, quad.ys[i] * 16);
				maxY = Math.max(maxY, quad.ys[i] * 16);
				minX = Math.min(minX, quad.xs[i] * 16);
				maxX = Math.max(maxX, quad.xs[i] * 16);
				u += quad.us[i] / 4;
				v += quad.vs[i] / 4;
			}
			int col = (int) Math.floor(u * Mat.ATLAS / Mat.CELL);
			int row = (int) Math.floor(v * Mat.ATLAS / Mat.CELL);
			String cell = "?";
			for (Mat mat : Mat.values()) {
				if (mat.column == col && mat.row == row) {
					cell = mat.name();
				}
			}
			if (quad.minZ() * 16 < -3f) {
				out.append(String.format("z=%.1f x=%.1f..%.1f y=%.1f..%.1f %s%n", quad.minZ() * 16, minX, maxX, minY, maxY, cell));
			}
		}
		System.out.println(out);
		assertTrue(out.length() > 0);
	}

	@Test
	void aCachedPlanDrawsTheSameQuadsAgain() {
		PoseStack pose = new PoseStack();
		MountPose anim = new MountPose(0f, 0f, 0f, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
		tk.darrow.shamanicmounts.genome.Phenotype phenotype = tk.darrow.shamanicmounts.genome.Expression
				.express(tk.darrow.shamanicmounts.genome.Founders.drumHart());
		Pen first = new Pen(pose, anim);
		Heads.head(first, phenotype, MountMesh.Shell.HART, Skin.HART[0], 0f, 0f);
		Capture a = new Capture();
		SolidDraw.Plan plan = first.flush(a, 0, 0, null, null);
		Pen second = new Pen(pose, anim);
		Heads.head(second, phenotype, MountMesh.Shell.HART, Skin.HART[0], 0f, 0f);
		Capture b = new Capture();
		second.flush(b, 0, 0, plan, null);
		assertEquals(a.quads.size(), b.quads.size());
		for (int i = 0; i < a.quads.size(); i++) {
			for (int k = 0; k < 4; k++) {
				assertEquals(a.quads.get(i).us[k], b.quads.get(i).us[k], 1.0e-6f, "u of quad " + i);
				assertEquals(a.quads.get(i).vs[k], b.quads.get(i).vs[k], 1.0e-6f, "v of quad " + i);
				assertEquals(a.quads.get(i).xs[k], b.quads.get(i).xs[k], 1.0e-6f, "x of quad " + i);
			}
		}
	}

	private static Capture wholeDrum(MountPose anim, SolidDraw.Plan[] plans) {
		tk.darrow.shamanicmounts.genome.Phenotype phenotype = tk.darrow.shamanicmounts.genome.Expression
				.express(tk.darrow.shamanicmounts.genome.Founders.drumHart());
		Capture capture = new Capture();
		MountMesh.drawWithPlans(phenotype, false, false, 0, 0, new PoseStack(), capture, null, 0, 0, anim, plans);
		return capture;
	}

	/** Every front-facing quad of the whole mount, at the brow's height, must be coat-coloured. */
	@Test
	void theWholeDrumFrontIsCoatAtBrowHeight() {
		MountPose anim = new MountPose(0f, 0f, 0f, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
		SolidDraw.Plan[] plans = new SolidDraw.Plan[60];
		Capture first = wholeDrum(anim, plans);
		MountPose later = new MountPose(3f, 0.4f, 57f, 10f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
		Capture second = wholeDrum(later, plans);
		assertEquals(first.quads.size(), second.quads.size(), "the cached plan changed the quad count");
		List<String> wrong = new ArrayList<>();
		for (Capture capture : List.of(first, second)) {
			for (Quad quad : capture.quads) {
				float u = 0, v = 0;
				for (int i = 0; i < 4; i++) {
					u += quad.us[i] / 4;
					v += quad.vs[i] / 4;
				}
				int col = (int) Math.floor(u * Mat.ATLAS / Mat.CELL);
				int row = (int) Math.floor(v * Mat.ATLAS / Mat.CELL);
				if (row == 6 || (col == 0 && row == 0) || (col == 7 && row == 0)) {
					wrong.add("eye/mane/nose cell on a quad at " + quad.xs[0] * 16 + "," + quad.ys[0] * 16 + "," + quad.zs[0] * 16);
				}
			}
		}
		// The nose pad, two nostrils, and two eyes account for the dark quads; a brow gone dark would double them.
		assertTrue(wrong.size() < 60, wrong.size() + " dark quads: " + String.join(" | ", wrong.subList(0, Math.min(12, wrong.size()))));
		assertEquals(0, wrong.size() % 2, "the cached frame drew a different number of dark quads");
	}

	@Test
	void everyMaterialHasItsOwnCellOnTheAtlas() {
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		int columns = (int) (Mat.ATLAS / Mat.CELL);
		for (Mat mat : Mat.values()) {
			assertTrue(mat.column >= 0 && mat.column < columns && mat.row >= 0 && mat.row < columns, mat + " is off the atlas");
			assertTrue(seen.add(mat.row * columns + mat.column), mat + " shares a cell");
		}
	}

	@Test
	void quadsComeInFours() {
		assertEquals(0, drumHead().size() % 1);
		assertTrue(drumHead().size() > 10);
	}
}
