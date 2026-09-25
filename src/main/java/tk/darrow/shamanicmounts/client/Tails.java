package tk.darrow.shamanicmounts.client;

import net.minecraft.util.Mth;

/** Tails hang from the tail root as a chain of hinged segments, each lagging the one before it. */
final class Tails {
	private Tails() {
	}

	/** Side to side wag for segment {@code index}, with a lift when the mount is walking. */
	private static float wag(MountPose anim, int index) {
		float idle = Mth.sin(anim.age * 0.11f - index * 0.55f) * 7.0f;
		float walk = Mth.cos(anim.swing * 0.6662f - index * 0.5f) * 14.0f * anim.amount;
		return idle + walk;
	}

	private static float lift(MountPose anim, int index) {
		return anim.air * 12.0f + Mth.sin(anim.age * 0.07f - index * 0.4f) * 2.0f * (1.0f - anim.amount);
	}

	/** Dock, then the long fall of hair. */
	static void plume(Pen pen, float y, float z, Skin skin) {
		MountPose anim = pen.anim;
		pen.hinge(0f, y, z + 1f, wag(anim, 0), 0f, -lift(anim, 0) * 2f, () -> {
			pen.box(-1.5f, y - 1f, z - 1f, 3f, 4f, 3.5f, skin.dark());
			pen.box(-1.5f, y + 1f, z - 6f, 3f, 3f, 6f, Mat.MANE);
			pen.hinge(0f, y + 2.5f, z - 6f, wag(anim, 1) * 0.6f, 0f, -lift(anim, 1), () -> {
				pen.box(-1.5f, y + 1.5f, z - 13f, 3f, 3.5f, 7.5f, Mat.MANE);
				pen.hinge(0f, y + 3f, z - 13f, wag(anim, 2) * 0.5f, 0f, -lift(anim, 2), () ->
						pen.box(-1f, y + 2f, z - 18f, 2.5f, 3f, 5.5f, Mat.MANE));
			});
		});
	}

	/** The short white flag a deer lifts when it runs. */
	static void flag(Pen pen, float y, float z, Skin skin) {
		MountPose anim = pen.anim;
		float raise = anim.amount * 40f;
		pen.hinge(0f, y, z, wag(anim, 0) * 0.4f, 0f, -raise, () -> {
			pen.box(-1.5f, y - 1f, z - 1f, 3f, 4f, 2.5f, skin.base());
			pen.box(-1.5f, y + 1f, z - 4f, 3f, 3f, 4f, Mat.FLAG);
			pen.box(-1f, y + 1.5f, z - 5f, 2f, 2f, 1.5f, Mat.FLAG);
		});
	}

	/** A cat's or a hound's tail: a tapered chain that curls up at the end. */
	static void lash(Pen pen, float y, float z, Skin skin, int segments, float thick, boolean curl) {
		MountPose anim = pen.anim;
		chain(pen, y, z, skin, segments, thick, curl, 0);
	}

	private static void chain(Pen pen, float y, float z, Skin skin, int segments, float thick, boolean curl, int index) {
		if (index >= segments) {
			return;
		}
		MountPose anim = pen.anim;
		float t = index / (float) Math.max(1, segments - 1);
		float size = thick * (1.0f - 0.45f * t);
		float length = 3.5f;
		float pitch = curl ? -6f - 14f * t : 4f - 10f * t;
		pitch -= lift(anim, index);
		// Lying down, the tail drops from the root and lies back along the ground.
		pitch = pitch * (1f - anim.sit) + anim.sit * (index == 0 ? 25f : -3f);
		Mat mat = t > 0.8f ? skin.dark() : index % 2 == 0 ? skin.base() : skin.dark();
		pen.hinge(0f, y, z, wag(anim, index) * (0.5f + 0.5f * t), 0f, pitch, () -> {
			pen.box(-size * 0.5f, y - 0.5f, z - size * 0.5f, size, length + 0.5f, size, mat);
			chain(pen, y + length, z, skin, segments, thick, curl, index + 1);
		});
	}

	/** A short fan of tail feathers with dark tips. */
	static void fan(Pen pen, float y, float z, Skin skin, float length, Mat tip) {
		MountPose anim = pen.anim;
		float spread = anim.air * 1.0f;
		pen.hinge(0f, y, z, wag(anim, 0) * 0.3f, 0f, 10f + anim.air * 20f, () -> {
			pen.box(-2.5f, y - 1f, z - 1f, 5f, 3f, 2.5f, skin.base());
			float[] xs = { -2.6f, -1.3f, 0f, 1.3f, 2.6f };
			for (int i = 0; i < xs.length; i++) {
				float x = xs[i] * (1f + spread);
				float len = length - Math.abs(xs[i]) * 0.7f;
				pen.box(x - 0.7f, y + 1.5f, z - 0.3f, 1.4f, len, 0.8f, i % 2 == 0 ? skin.base() : skin.dark());
				pen.box(x - 0.7f, y + 1.5f + len - 1.5f, z - 0.2f, 1.4f, 1.5f, 0.9f, tip);
			}
		});
	}

	/** The chimera's tail: scaled chain with a fur tuft and a spade fin. */
	static void spade(Pen pen, float y, float z, Skin skin) {
		MountPose anim = pen.anim;
		spadeChain(pen, y, z, 0, 7, anim, skin);
	}

	private static void spadeChain(Pen pen, float y, float z, int index, int segments, MountPose anim, Skin skin) {
		float t = index / (float) (segments - 1);
		float size = 4.5f * (1.0f - 0.5f * t);
		float length = 4f;
		float pitch = -4f - 10f * t - lift(anim, index) * 0.5f;
		Mat mat = index % 2 == 0 ? skin.rosette() : skin.base();
		pen.hinge(0f, y, z, wag(anim, index) * (0.4f + 0.6f * t), 0f, pitch, () -> {
			pen.box(-size * 0.5f, y - 0.5f, z - size * 0.5f, size, length + 0.5f, size, mat);
			// A ridge of plates runs down the top of the tail.
			pen.box(-0.6f, y, z + size * 0.5f - 0.2f, 1.2f, length * 0.6f, 1.2f, index % 2 == 0 ? Mat.PEARL : Mat.WISH);
			if (index + 1 < segments) {
				spadeChain(pen, y + length, z, index + 1, segments, anim, skin);
			} else {
				pen.box(-3f, y + length - 1f, z - 0.4f, 6f, 5f, 0.8f, skin.star());
				pen.box(-1.5f, y + length + 3.5f, z - 0.4f, 3f, 2.5f, 0.8f, skin.star());
				pen.box(-1f, y + length - 1f, z - 1.2f, 2f, 5f, 2.4f, skin.dark());
			}
		});
	}
}
