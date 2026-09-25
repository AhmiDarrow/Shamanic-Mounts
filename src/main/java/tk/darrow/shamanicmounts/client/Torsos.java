package tk.darrow.shamanicmounts.client;

import static tk.darrow.shamanicmounts.client.Anchor.post;

import net.minecraft.util.Mth;

/**
 * Bodies, one per line. Each is a barrel with a fuller chest, a hanging belly, proud shoulder and
 * haunch plates so the light catches the muscle, and a spine line. Feet stand on Z = 0.
 */
final class Torsos {
	private Torsos() {
	}

	/** The shoulder the whole body swings around when it sits or flies. */
	static float[] shoulder(MountMesh.Shell shell) {
		return switch (shell) {
			case STEED -> new float[] { 0f, 15f };
			case HART -> new float[] { 0f, 16f };
			case ELK -> new float[] { 0f, 17f };
			case CRANE -> new float[] { 0f, 24f };
			case ROC -> new float[] { 0f, 17f };
			case NAGUAL -> new float[] { 0f, 13f };
			case BARGHEST -> new float[] { 0f, 17f };
			case SHADE -> new float[] { 0f, 12f };
			case CHIMERA -> new float[] { 0f, 16f };
			case BEAR -> new float[] { 0f, 15f };
			case SERPENT -> new float[] { 0f, 8f };
		};
	}

	static Anchor draw(Pen pen, MountMesh.Shell shell, Skin skin) {
		return switch (shell) {
			case STEED -> steed(pen, skin);
			case HART -> hart(pen, skin);
			case ELK -> elk(pen, skin);
			case CRANE -> crane(pen, skin);
			case ROC -> roc(pen, skin);
			case NAGUAL -> nagual(pen, skin);
			case BARGHEST -> barghest(pen, skin);
			case SHADE -> shade(pen, skin);
			case CHIMERA -> chimera(pen, skin);
			case BEAR -> bear(pen, skin);
			case SERPENT -> serpent(pen, skin);
		};
	}

	private static void breathe(Pen pen, float y, float z, Runnable body) {
		float b = pen.anim.breath;
		pen.scaleAt(0f, y, z, 1.0f + 0.010f * b, 1.0f, 1.0f + 0.014f * b, body);
	}

	private static Anchor steed(Pen pen, Skin skin) {
		breathe(pen, 6f, 16f, () -> {
			pen.box(-5f, 0f, 12f, 10f, 20f, 9f, skin.base());
			pen.box(-5.5f, -2f, 11.5f, 11f, 9f, 9.5f, skin.base());
			pen.box(-4f, 5f, 11f, 8f, 13f, 1.6f, skin.pale());
			pen.box(-3f, -2.4f, 12.5f, 6f, 2f, 6f, skin.pale());
			pen.box(-4.5f, 16f, 12.5f, 9f, 6.5f, 9f, skin.base());
		});
		Anchor a = new Anchor();
		a.neckY = -2f;
		a.neckZ = 14f;
		a.half = 5f;
		a.wingY = 2f;
		a.wingZ = 17f;
		a.tailY = 22f;
		a.tailZ = 19f;
		a.saddleY = 5f;
		a.saddleZ = 21f;
		a.chestY = 6f;
		a.chestZ = 16f;
		a.posts = new Anchor.Post[] {
				post(-5.5f, -1f, 13f, 3f, true), post(2.5f, -1f, 13f, 3f, true),
				post(-5.5f, 5f, 13f, 3f, true), post(2.5f, 5f, 13f, 3f, true),
				post(-5.5f, 11f, 13f, 3f, false), post(2.5f, 11f, 13f, 3f, false),
				post(-5.5f, 17f, 13f, 3f, false), post(2.5f, 17f, 13f, 3f, false)
		};
		return a;
	}

	private static Anchor hart(Pen pen, Skin skin) {
		breathe(pen, 6f, 18f, () -> {
			pen.box(-4f, 0f, 14f, 8f, 18f, 8f, skin.base());
			pen.box(-4.5f, -2f, 13.5f, 9f, 7f, 8.5f, skin.base());
			pen.box(-3f, 3f, 13f, 6f, 12f, 1.5f, skin.pale());
			pen.box(-3f, -2.4f, 13f, 6f, 3f, 5f, skin.pale());
			pen.box(-4f, 14f, 14.5f, 8f, 5f, 7.5f, skin.base());
		});
		Anchor a = new Anchor();
		a.neckY = -2f;
		a.neckZ = 16f;
		a.half = 4f;
		a.wingY = 1f;
		a.wingZ = 18f;
		a.tailY = 19f;
		a.tailZ = 20f;
		a.saddleY = 4f;
		a.saddleZ = 22f;
		a.chestY = 6f;
		a.chestZ = 18f;
		a.posts = new Anchor.Post[] {
				post(-4f, -1f, 15f, 2.5f, true), post(1.5f, -1f, 15f, 2.5f, true),
				post(-4f, 13f, 15f, 2.5f, false), post(1.5f, 13f, 15f, 2.5f, false)
		};
		return a;
	}

	private static Anchor elk(Pen pen, Skin skin) {
		breathe(pen, 8f, 18f, () -> {
			pen.box(-6f, 0f, 13f, 12f, 22f, 10f, skin.base());
			pen.box(-6.5f, -3f, 12.5f, 13f, 10f, 10.5f, skin.base());
			pen.box(-4.5f, -1f, 22f, 9f, 7f, 3f, skin.dark());
			pen.box(-5f, 4f, 12f, 10f, 14f, 1.5f, skin.pale());
			pen.box(-6f, 16f, 13.5f, 12f, 6.5f, 9.5f, skin.base());
			pen.box(-2f, -3.5f, 10f, 4f, 6f, 4f, skin.hair());
		});
		Anchor a = new Anchor();
		a.neckY = -3f;
		a.neckZ = 16f;
		a.half = 6f;
		a.wingY = 2f;
		a.wingZ = 19f;
		a.tailY = 22f;
		a.tailZ = 21f;
		a.saddleY = 6f;
		a.saddleZ = 24f;
		a.chestY = 8f;
		a.chestZ = 18f;
		a.posts = new Anchor.Post[] {
				post(-6f, -1.5f, 14f, 3.5f, true), post(2.5f, -1.5f, 14f, 3.5f, true),
				post(-6f, 16f, 14f, 3.5f, false), post(2.5f, 16f, 14f, 3.5f, false)
		};
		return a;
	}

	private static Anchor crane(Pen pen, Skin skin) {
		breathe(pen, 6f, 24f, () -> {
			pen.box(-4f, 0f, 20f, 8f, 12f, 8f, skin.base());
			pen.box(-4.5f, 1f, 21f, 9f, 8f, 6f, skin.base());
		});
		Anchor a = new Anchor();
		a.neckY = -1f;
		a.neckZ = 23f;
		a.half = 4f;
		a.wingY = 1f;
		a.wingZ = 24.5f;
		a.tailY = 12f;
		a.tailZ = 24f;
		a.saddleY = 3f;
		a.saddleZ = 28f;
		a.chestY = 6f;
		a.chestZ = 24f;
		a.posts = new Anchor.Post[] { post(-3.5f, 4f, 21f, 2f, true), post(1.5f, 4f, 21f, 2f, true) };
		return a;
	}

	private static Anchor roc(Pen pen, Skin skin) {
		breathe(pen, 8f, 17f, () -> {
			pen.box(-6f, 0f, 12f, 12f, 18f, 11f, skin.base());
			pen.box(-6.5f, -2f, 12.5f, 13f, 9f, 10f, skin.base());
			pen.box(-4f, 1f, 11f, 8f, 12f, 1.5f, skin.base());
			pen.box(-5f, 12f, 13f, 10f, 7f, 9f, skin.base());
		});
		Anchor a = new Anchor();
		a.neckY = -2f;
		a.neckZ = 15f;
		a.half = 6f;
		a.wingY = 3f;
		a.wingZ = 18f;
		a.tailY = 19f;
		a.tailZ = 15f;
		a.saddleY = 4f;
		a.saddleZ = 23f;
		a.chestY = 8f;
		a.chestZ = 17f;
		a.posts = new Anchor.Post[] { post(-6f, 6f, 13f, 4f, true), post(2f, 6f, 13f, 4f, true) };
		return a;
	}

	private static Anchor nagual(Pen pen, Skin skin) {
		breathe(pen, 8f, 13f, () -> {
			pen.box(-6f, 0f, 9f, 12f, 22f, 9f, skin.base());
			pen.box(-6.5f, -3f, 8.5f, 13f, 9f, 9.5f, skin.base());
			pen.box(-5f, 4f, 8f, 10f, 14f, 1.5f, skin.pale());
			pen.box(-4f, -3.4f, 8.5f, 8f, 3f, 7f, skin.pale());
			pen.box(-6f, 16f, 9.5f, 12f, 6.5f, 8.5f, skin.base());
		});
		Anchor a = new Anchor();
		a.neckY = -3f;
		a.neckZ = 12f;
		a.half = 6f;
		a.wingY = 2f;
		a.wingZ = 14f;
		a.tailY = 22.5f;
		a.tailZ = 15f;
		a.saddleY = 5f;
		a.saddleZ = 18f;
		a.chestY = 8f;
		a.chestZ = 13f;
		a.posts = new Anchor.Post[] {
				post(-6f, -1.5f, 10f, 4f, true), post(2f, -1.5f, 10f, 4f, true),
				post(-6f, 16f, 10f, 4f, false), post(2f, 16f, 10f, 4f, false)
		};
		return a;
	}

	private static Anchor barghest(Pen pen, Skin skin) {
		breathe(pen, 6f, 17f, () -> {
			pen.box(-5f, -2f, 12f, 10f, 10f, 10f, skin.base());
			pen.box(-3.5f, 6f, 14f, 7f, 10f, 7.5f, skin.base());
			pen.box(-4f, 13f, 13f, 8f, 7f, 8.5f, skin.base());
			pen.box(-4f, -2.4f, 13f, 8f, 3f, 6f, skin.pale());
		});
		Anchor a = new Anchor();
		a.neckY = -2f;
		a.neckZ = 16f;
		a.half = 5f;
		a.wingY = 0f;
		a.wingZ = 17f;
		a.tailY = 20f;
		a.tailZ = 19.5f;
		a.saddleY = 4f;
		a.saddleZ = 21f;
		a.chestY = 6f;
		a.chestZ = 17f;
		a.posts = new Anchor.Post[] {
				post(-4.5f, -1f, 14f, 3f, true), post(1.5f, -1f, 14f, 3f, true),
				post(-4.5f, 15f, 14f, 3f, false), post(1.5f, 15f, 14f, 3f, false)
		};
		return a;
	}

	private static Anchor shade(Pen pen, Skin skin) {
		breathe(pen, 6f, 13f, () -> {
			pen.box(-3.5f, 0f, 10f, 7f, 18f, 6f, skin.base());
			pen.box(-4f, -2f, 9.5f, 8f, 6f, 6.5f, skin.base());
			pen.box(-2.5f, -2.4f, 9.5f, 5f, 3f, 4.5f, skin.pale());
			pen.box(-3.5f, 14f, 10.5f, 7f, 5f, 5.5f, skin.base());
		});
		Anchor a = new Anchor();
		a.neckY = -2f;
		a.neckZ = 12f;
		a.half = 3.5f;
		a.wingY = 1f;
		a.wingZ = 13.5f;
		a.tailY = 19f;
		a.tailZ = 13f;
		a.saddleY = 4f;
		a.saddleZ = 16f;
		a.chestY = 6f;
		a.chestZ = 13f;
		a.posts = new Anchor.Post[] {
				post(-3.5f, -1f, 11f, 2.5f, true), post(1f, -1f, 11f, 2.5f, true),
				post(-3.5f, 14f, 11f, 2.5f, false), post(1f, 14f, 11f, 2.5f, false)
		};
		return a;
	}

	/**
	 * A long thick body on the ground, in a lazy S that ripples with the stride, thickest under the
	 * saddle and tapering to the tail root. The belly scales are pale. No legs at all.
	 */
	private static Anchor serpent(Pen pen, Skin skin) {
		MountPose anim = pen.anim;
		float travel = anim.swing * 0.6662f;
		float[] widths = { 8f, 9f, 10f, 10f, 9f, 8f, 7f };
		for (int i = 0; i < widths.length; i++) {
			float w = widths[i];
			float y = -4f + i * 5f;
			float sway = Mth.sin(travel - i * 0.9f) * (1.2f + 2.4f * anim.amount) + Mth.sin(anim.age * 0.05f + i) * 0.6f;
			int seg = i;
			pen.lift(sway, 0f, 0f, () -> {
				pen.box(-w * 0.5f, y, 0f, w, 5.2f, w * 0.9f, skin.base());
				pen.box(-w * 0.35f, y - 0.1f, -0.2f, w * 0.7f, 5.4f, 1.4f, skin.pale());
				pen.box(-w * 0.2f, y, w * 0.9f - 0.2f, w * 0.4f, 5.2f, 1f, skin.dark());
				if (seg % 2 == 0) {
					pen.pair(-w * 0.5f - 0.3f, y + 1f, 2f, 0.5f, 3f, w * 0.5f, skin.dark());
				}
			});
		}
		Anchor a = new Anchor();
		a.neckY = -4f;
		a.neckZ = 7f;
		a.half = 5f;
		a.wingY = 4f;
		a.wingZ = 9f;
		a.tailY = 31f;
		a.tailZ = 3f;
		a.saddleY = 6f;
		a.saddleZ = 9.2f;
		a.chestY = 8f;
		a.chestZ = 8f;
		a.posts = new Anchor.Post[] { post(-5f, 0f, 6f, 3f, true), post(2f, 0f, 6f, 3f, true), post(-5f, 20f, 6f, 3f, false), post(2f, 20f, 6f, 3f, false) };
		return a;
	}

	/** A low, heavy barrel with a shoulder hump, a wide chest, and a stub of a tail. */
	private static Anchor bear(Pen pen, Skin skin) {
		breathe(pen, 8f, 14f, () -> {
			pen.box(-6.5f, -1f, 9f, 13f, 22f, 12f, skin.base());
			pen.box(-7f, -3f, 8.5f, 14f, 10f, 12.5f, skin.base());
			pen.box(-5.5f, 1f, 20.5f, 11f, 9f, 2f, skin.dark());
			pen.box(-5f, 2f, 8.2f, 10f, 16f, 1.5f, skin.pale());
			pen.box(-6.5f, 17f, 9.5f, 13f, 6.5f, 11f, skin.base());
			pen.box(-1.5f, 23f, 15f, 3f, 2f, 2.5f, skin.dark());
		});
		Anchor a = new Anchor();
		a.neckY = -3f;
		a.neckZ = 14f;
		a.half = 6.5f;
		a.wingY = 3f;
		a.wingZ = 18f;
		a.tailY = 23f;
		a.tailZ = 16f;
		a.saddleY = 6f;
		a.saddleZ = 21f;
		a.chestY = 8f;
		a.chestZ = 14f;
		a.posts = new Anchor.Post[] {
				post(-7f, -2f, 11f, 4.5f, true), post(2.5f, -2f, 11f, 4.5f, true),
				post(-7f, 17f, 11f, 4.5f, false), post(2.5f, 17f, 11f, 4.5f, false)
		};
		return a;
	}

	private static Anchor chimera(Pen pen, Skin skin) {
		breathe(pen, 8f, 16f, () -> {
			pen.box(-6f, 0f, 11f, 12f, 24f, 11f, skin.base());
			pen.box(-6.5f, -3f, 10.5f, 13f, 10f, 11.5f, skin.base());
			pen.box(-6f, 18f, 11.5f, 12f, 6.5f, 10.5f, skin.base());
			pen.box(-2f, -4.5f, 21.5f, 4f, 5f, 2.8f, skin.dark());
			float[] plates = { 0f, 3f, 15f, 18f, 21f };
			for (int i = 0; i < plates.length; i++) {
				pen.box(-1.5f, plates[i], 21.6f, 3f, 2.4f, 3.2f, i % 2 == 0 ? Mat.PEARL : Mat.WISH);
			}
		});
		Anchor a = new Anchor();
		a.neckY = -3f;
		a.neckZ = 15f;
		a.half = 6f;
		a.wingY = 4f;
		a.wingZ = 18f;
		a.tailY = 24.5f;
		a.tailZ = 17f;
		a.saddleY = 7f;
		a.saddleZ = 22f;
		a.chestY = 8f;
		a.chestZ = 16f;
		a.posts = new Anchor.Post[] {
				post(-6.5f, -1.5f, 12f, 4.5f, true), post(2f, -1.5f, 12f, 4.5f, true),
				post(-6.5f, 18.5f, 12f, 4.5f, false), post(2f, 18.5f, 12f, 4.5f, false)
		};
		return a;
	}
}
