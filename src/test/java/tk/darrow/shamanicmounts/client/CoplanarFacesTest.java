package tk.darrow.shamanicmounts.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.PoseStack;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Phenotype;

/**
 * No two faces that look the same way ever lie in one plane over the same patch while a mount stands, walks, or lies
 * down and breathes. Two such faces fight for the same depth and flicker; breathing sweeps the torso's faces through
 * the planes of the legs, necks, and tails set against it, so the check follows each pair across a whole breath.
 *
 * <p>The serpent is left out: its segments slide past one another by design. So is a perched feathered wing on a
 * saddled mount (it rises and falls a third of a pixel with the breath, through the tack).
 */
class CoplanarFacesTest {
	/** Planes this close count as one: 0.002 px. */
	private static final float TOL = 0.002f / 16f;
	private static final float[] BREATHS = { -1f, -0.5f, 0f, 0.5f, 1f };

	private static Map<String, Genome> mounts() {
		Map<String, Genome> m = new LinkedHashMap<>();
		m.put("eightfold", Founders.eightfold());
		m.put("drum hart", Founders.drumHart());
		m.put("elk", Founders.elk());
		m.put("crane", Founders.crane());
		m.put("nagual", Founders.nagual());
		m.put("barghest", Founders.barghest());
		m.put("roc", Founders.roc());
		m.put("shade", Founders.shade());
		m.put("bear", Founders.bear());
		m.put("chimera", Founders.eightfold().asChimera());
		m.put("hart x eightfold", new Genome(Founders.drumHart().maternal, Founders.eightfold().paternal, true, true, true));
		m.put("bear x steed", new Genome(Founders.bear().maternal, Founders.eightfold().paternal, true, true, true));
		m.put("elk x nagual", new Genome(Founders.elk().maternal, Founders.nagual().paternal, true, false, true));
		m.put("hart x bird", new Genome(Founders.drumHart().maternal, Founders.crane().paternal, false, false, false));
		return m;
	}

	private static MountPose pose(int kind, float breath) {
		float age = (float) (Math.asin(breath) / 0.06 + 2 * Math.PI / 0.06 * 3);
		return switch (kind) {
			case 0 -> new MountPose(0f, 0f, age, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
			case 1 -> new MountPose(1.3f, 1f, age, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
			case 2 -> new MountPose(3.7f, 1f, age, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, false, 0, false);
			default -> new MountPose(0f, 0f, age, 0f, 0f, 0f, 0, 1f, 0f, 0f, false, false, 0, false);
		};
	}

	@Test
	void noFacesShareAPlaneThroughABreath() {
		String[] poses = { "standing", "mid-stride", "late stride", "lying down" };
		List<String> fights = new ArrayList<>();
		for (Map.Entry<String, Genome> mount : mounts().entrySet()) {
			Phenotype phenotype = Expression.express(mount.getValue());
			boolean feathered = phenotype.wings != Phenotype.WingShow.NONE
					&& MountMesh.shell(phenotype) != MountMesh.Shell.CHIMERA;
			for (int tack = 0; tack < (feathered ? 1 : 3); tack++) {
				SolidDraw.Plan[] plans = new SolidDraw.Plan[60];
				for (int kind = 0; kind < poses.length; kind++) {
					List<List<SolidDrawTest.Quad>> draws = new ArrayList<>();
					for (float breath : BREATHS) {
						SolidDrawTest.Capture capture = new SolidDrawTest.Capture();
						MountMesh.drawWithPlans(phenotype, tack >= 1, tack >= 1, tack == 2 ? 2 : 0, 0, new PoseStack(), capture, null, 0, 0,
								pose(kind, breath), plans);
						draws.add(capture.quads);
					}
					fights.addAll(fights(mount.getKey() + (tack == 1 ? " saddled" : tack == 2 ? " armored" : "") + ", " + poses[kind], draws));
				}
			}
		}
		assertTrue(fights.isEmpty(), fights.size() + " face pairs share a plane: " + String.join(" | ", fights.subList(0, Math.min(8, fights.size()))));
	}

	private static List<String> fights(String what, List<List<SolidDrawTest.Quad>> draws) {
		List<String> out = new ArrayList<>();
		List<SolidDrawTest.Quad> mid = draws.get(BREATHS.length / 2);
		for (int i = 0; i < mid.size(); i++) {
			for (int j = i + 1; j < mid.size(); j++) {
				float first = gap(mid.get(i), mid.get(j));
				if (Float.isNaN(first) || Math.abs(first) > 0.2f / 16f) {
					continue;
				}
				float lo = Float.MAX_VALUE;
				float hi = -Float.MAX_VALUE;
				float near = Float.MAX_VALUE;
				for (List<SolidDrawTest.Quad> draw : draws) {
					float g = gap(draw.get(i), draw.get(j));
					if (!Float.isNaN(g)) {
						lo = Math.min(lo, g);
						hi = Math.max(hi, g);
						near = Math.min(near, Math.abs(g));
					}
				}
				if (near < TOL || (lo < 0f && hi > 0f)) {
					out.add(String.format("%s: quads %d and %d, %.3f to %.3f px apart", what, i, j, lo * 16f, hi * 16f));
				}
			}
		}
		return out;
	}

	/** How far b's plane lies past a's along a's normal, if both face the same way and overlap; else NaN. */
	private static float gap(SolidDrawTest.Quad a, SolidDrawTest.Quad b) {
		float[] na = normal(a);
		float[] nb = normal(b);
		if (na == null || nb == null || dot(na, nb) < 0.9999) {
			return Float.NaN;
		}
		// A cross's girth scales x after a turn, so faces can be parallelograms: measure in a's own edge basis.
		float[] e1 = { a.xs()[1] - a.xs()[0], a.ys()[1] - a.ys()[0], a.zs()[1] - a.zs()[0] };
		float[] e2 = { a.xs()[3] - a.xs()[0], a.ys()[3] - a.ys()[0], a.zs()[3] - a.zs()[0] };
		double g11 = dot(e1, e1), g12 = dot(e1, e2), g22 = dot(e2, e2), det = g11 * g22 - g12 * g12;
		double s0 = Double.MAX_VALUE, s1 = -Double.MAX_VALUE, t0 = Double.MAX_VALUE, t1 = -Double.MAX_VALUE;
		for (int k = 0; k < 4; k++) {
			float[] p = { b.xs()[k] - a.xs()[0], b.ys()[k] - a.ys()[0], b.zs()[k] - a.zs()[0] };
			double r1 = dot(p, e1), r2 = dot(p, e2);
			double s = (r1 * g22 - r2 * g12) / det, t = (r2 * g11 - r1 * g12) / det;
			s0 = Math.min(s0, s);
			s1 = Math.max(s1, s);
			t0 = Math.min(t0, t);
			t1 = Math.max(t1, t);
		}
		double across = (Math.min(1, s1) - Math.max(0, s0)) * Math.sqrt(g11) * 16;
		double up = (Math.min(1, t1) - Math.max(0, t0)) * Math.sqrt(g22) * 16;
		if (across < 0.1 || up < 0.1) {
			return Float.NaN;
		}
		double da = na[0] * a.xs()[0] + na[1] * a.ys()[0] + na[2] * a.zs()[0];
		double db = na[0] * b.xs()[0] + na[1] * b.ys()[0] + na[2] * b.zs()[0];
		return (float) (db - da);
	}

	private static double dot(float[] a, float[] b) {
		return (double) a[0] * b[0] + (double) a[1] * b[1] + (double) a[2] * b[2];
	}

	private static float[] normal(SolidDrawTest.Quad q) {
		float ux = q.xs()[1] - q.xs()[0], uy = q.ys()[1] - q.ys()[0], uz = q.zs()[1] - q.zs()[0];
		float vx = q.xs()[2] - q.xs()[0], vy = q.ys()[2] - q.ys()[0], vz = q.zs()[2] - q.zs()[0];
		float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		return length < 1e-9f ? null : new float[] { nx / length, ny / length, nz / length };
	}
}
