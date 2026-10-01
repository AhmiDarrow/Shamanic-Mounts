package tk.darrow.shamanicmounts.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** The cheaper draw must draw the same thing: the pen's own stack, culled back faces, and parts cut once. */
class DrawCostTest {
	/** Every vertex as it reaches the buffer: position, texture, normal. */
	static final class Vertices implements VertexConsumer {
		final List<float[]> all = new ArrayList<>();
		private float[] at;

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			at = new float[] { x, y, z, 0, 0, 0, 0, 0 };
			all.add(at);
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			at[3] = u;
			at[4] = v;
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
			at[5] = x;
			at[6] = y;
			at[7] = z;
			return this;
		}

		/** One string per quad, exact to the bit, for set comparisons. */
		Set<String> quads() {
			Set<String> out = new HashSet<>();
			for (int q = 0; q + 3 < all.size(); q += 4) {
				StringBuilder key = new StringBuilder();
				for (int i = 0; i < 4; i++) {
					for (float f : all.get(q + i)) {
						key.append(Float.floatToIntBits(f)).append(',');
					}
				}
				out.add(key.toString());
			}
			return out;
		}
	}

	private static MountPose pose(float open, int wing, float air) {
		return new MountPose(1.3f, 0.6f, 44f, 12f, -5f, open, wing, 0f, air, 0.1f, false, false, 5, false);
	}

	@Test
	void thePensStackDoesThePoseStacksArithmetic() {
		PoseStack reference = new PoseStack();
		reference.translate(0.3f, -1.2f, 4.5f);
		reference.mulPose(Axis.YP.rotationDegrees(37f));
		reference.pushPose();
		reference.translate(0.1f, 0.2f, -0.3f);
		reference.mulPose(Axis.XP.rotationDegrees(-12.5f));
		reference.mulPose(Axis.ZP.rotationDegrees(80f));
		reference.scale(1.2f, 0.7f, 1f);
		reference.scale(1.5f, 1.5f, 1.5f);
		SolidDraw expected = new SolidDraw();
		expected.add(reference, 0, 0.1f, 0.2f, 0.3f, 0.5f, 0.25f, 0.75f, Mat.HART, 0, null);
		Vertices want = new Vertices();
		expected.flush(want, 0, 0, null, false, null);

		PoseStack start = new PoseStack();
		start.translate(0.3f, -1.2f, 4.5f);
		start.mulPose(Axis.YP.rotationDegrees(37f));
		SolidDraw draw = new SolidDraw();
		draw.begin(start.last());
		draw.push();
		draw.translate(0.1f, 0.2f, -0.3f);
		draw.rotateX(-12.5f);
		draw.rotateZ(80f);
		draw.scale(1.2f, 0.7f, 1f);
		draw.scale(1.5f, 1.5f, 1.5f);
		draw.add(0, 0.1f, 0.2f, 0.3f, 0.5f, 0.25f, 0.75f, Mat.HART, 0, null);
		Vertices got = new Vertices();
		draw.flush(got, 0, 0, null, false, null);

		assertEquals(want.all.size(), got.all.size());
		for (int i = 0; i < want.all.size(); i++) {
			assertArrayEquals(want.all.get(i), got.all.get(i), 0f, "vertex " + i);
		}
	}

	/** A crane five blocks in front of the camera, turned a little, as the level's entity pass would place it. */
	private static Vertices crane(boolean cull, float yaw) {
		Phenotype phenotype = Expression.express(Founders.crane());
		PoseStack pose = new PoseStack();
		pose.translate(0.4f, -1.1f, -5f);
		pose.mulPose(Axis.YP.rotationDegrees(yaw));
		Vertices out = new Vertices();
		MountMesh.drawWithPlans(phenotype, true, true, 2, 1, pose, out, null, 0, 0, pose(1f, 2, 1f), MountMesh.newPlans(), cull);
		return out;
	}

	@Test
	void cullingLeavesOutOnlyFacesTurnedAway() {
		for (float yaw : new float[] { 0f, 50f, 140f, 260f }) {
			Vertices whole = crane(false, yaw);
			Set<String> all = whole.quads();
			Set<String> kept = crane(true, yaw).quads();
			assertTrue(all.containsAll(kept), "culling changed a face it kept");
			assertTrue(kept.size() < all.size() * 0.7, "culling left out too little: " + kept.size() + " of " + all.size());
			// Every face left out looks away from the camera at the origin.
			for (int q = 0; q < whole.all.size(); q += 4) {
				List<float[]> v = whole.all.subList(q, q + 4);
				StringBuilder key = new StringBuilder();
				for (float[] corner : v) {
					for (float f : corner) {
						key.append(Float.floatToIntBits(f)).append(',');
					}
				}
				if (kept.contains(key.toString())) {
					continue;
				}
				float[] c = v.get(0);
				float facing = c[0] * c[5] + c[1] * c[6] + c[2] * c[7];
				assertTrue(facing > 0f, "a face toward the camera was left out at yaw " + yaw);
			}
		}
	}

	@Test
	void aHoppingFlierCutsNoPartTwice() {
		for (Genome genome : new Genome[] { Founders.crane(), Founders.roc(), Founders.drumHart() }) {
			Phenotype phenotype = Expression.express(genome);
			SolidDraw.Plan[] plans = MountMesh.newPlans();
			float[][] hop = { { 0f, 0, 0f }, { 0f, 0, 0.2f }, { 1f, 2, 0.4f }, { 1f, 2, 0.7f }, { 1f, 1, 1f }, { 0.3f, 1, 0.8f },
					{ 0f, 0, 0.5f }, { 0f, 0, 0.1f }, { 0f, 0, 0f } };
			for (int round = 0; round < 2; round++) {
				long before = SolidDraw.replans;
				for (float[] frame : hop) {
					MountMesh.drawWithPlans(phenotype, false, false, 0, 0, new PoseStack(), new Vertices(), null, 0, 0,
							pose(frame[0], (int) frame[1], frame[2]), plans, false);
				}
				if (round == 1) {
					assertEquals(0, SolidDraw.replans - before, "a second hop cut parts again");
				}
			}
		}
	}

	@Test
	void twoMountsBuiltAlikeShareTheirCuts() {
		SolidDraw.Plan[] first = MountMesh.newPlans();
		MountMesh.drawWithPlans(Expression.express(Founders.elk()), false, false, 0, 2, new PoseStack(), new Vertices(), null, 0, 0,
				pose(0f, 0, 0f), first, false);
		long before = SolidDraw.replans;
		// The same genes expressed again, as a second wild elk would be: a separate phenotype, the same body.
		MountMesh.drawWithPlans(Expression.express(Founders.elk()), false, false, 0, 2, new PoseStack(), new Vertices(), null, 0, 0,
				pose(0f, 0, 0f), MountMesh.newPlans(), false);
		assertEquals(0, SolidDraw.replans - before, "a second mount built alike cut its parts again");
	}
}
