package tk.darrow.shamanicmounts.client;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** One axis-aligned cube, in blocks, y up. */
final class CubeDraw {
	private CubeDraw() {
	}

	static void box(PoseStack pose, VertexConsumer consumer, int light, int overlay, float x, float y, float z, float w,
			float h, float d, int color) {
		float x1 = x + w;
		float y1 = y + h;
		float z1 = z + d;
		int a = (color >>> 24) & 255;
		int r = (color >>> 16) & 255;
		int g = (color >>> 8) & 255;
		int b = color & 255;
		if (a == 0) {
			a = 255;
		}
		PoseStack.Pose entry = pose.last();
		// Entity shaders ignore the normal, so each face carries the block shade itself.
		face(entry, consumer, light, overlay, r, g, b, a, 0.8f, x, y, z1, x1, y, z1, x1, y1, z1, x, y1, z1, 0, 0, 1);
		face(entry, consumer, light, overlay, r, g, b, a, 0.8f, x1, y, z, x, y, z, x, y1, z, x1, y1, z, 0, 0, -1);
		face(entry, consumer, light, overlay, r, g, b, a, 1.0f, x, y1, z1, x1, y1, z1, x1, y1, z, x, y1, z, 0, 1, 0);
		face(entry, consumer, light, overlay, r, g, b, a, 0.5f, x, y, z, x1, y, z, x1, y, z1, x, y, z1, 0, -1, 0);
		face(entry, consumer, light, overlay, r, g, b, a, 0.6f, x1, y, z1, x1, y, z, x1, y1, z, x1, y1, z1, 1, 0, 0);
		face(entry, consumer, light, overlay, r, g, b, a, 0.6f, x, y, z, x, y, z1, x, y1, z1, x, y1, z, -1, 0, 0);
	}

	private static void face(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int r, int g, int b,
			int a, float shade, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float nx, float ny, float nz) {
		int rs = Math.min(255, Math.round(r * shade));
		int gs = Math.min(255, Math.round(g * shade));
		int bs = Math.min(255, Math.round(b * shade));
		vertex(pose, consumer, light, overlay, rs, gs, bs, a, x0, y0, z0, 0, 0, nx, ny, nz);
		vertex(pose, consumer, light, overlay, rs, gs, bs, a, x1, y1, z1, 1, 0, nx, ny, nz);
		vertex(pose, consumer, light, overlay, rs, gs, bs, a, x2, y2, z2, 1, 1, nx, ny, nz);
		vertex(pose, consumer, light, overlay, rs, gs, bs, a, x3, y3, z3, 0, 1, nx, ny, nz);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int r, int g, int b,
			int a, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		Matrix4f matrix = pose.pose();
		Vector3f normal = pose.transformNormal(new Vector3f(nx, ny, nz), new Vector3f());
		consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(overlay).setLight(light)
				.setNormal(normal.x, normal.y, normal.z);
	}

	/** One quad. Vertex color is the face shade; the atlas supplies the albedo. */
	static void quad(Matrix4f model, Matrix3f normal, VertexConsumer consumer, int light, int overlay, float shade,
			float[] xs, float[] ys, float[] zs, float[] us, float[] vs, float nx, float ny, float nz, Vector3f scratch) {
		int tone = Math.min(255, Math.round(shade * 255f));
		Vector3f transformed = normal.transform(scratch.set(nx, ny, nz));
		for (int i = 0; i < 4; i++) {
			consumer.addVertex(model, xs[i], ys[i], zs[i]).setColor(tone, tone, tone, 255).setUv(us[i], vs[i])
					.setOverlay(overlay).setLight(light).setNormal(transformed.x, transformed.y, transformed.z);
		}
	}
}
