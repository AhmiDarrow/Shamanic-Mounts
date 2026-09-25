package tk.darrow.shamanicmounts.client;

import tk.darrow.shamanicmounts.genome.Phenotype;

/**
 * Necks and heads. A neck starts at the torso's neck anchor and returns the socket its head hangs
 * from: the front y of its last segment and the z of that segment's underside. Heads are drawn
 * about that socket, muzzle forward along -y, and carry their own ears, eyes, brows, and racks.
 */
final class Heads {
	private Heads() {
	}

	static float[] neck(Pen pen, MountMesh.Shell shell, Anchor a, Skin skin) {
		return switch (shell) {
			case STEED -> steedNeck(pen, a, skin);
			case HART -> hartNeck(pen, a, skin);
			case ELK -> elkNeck(pen, a, skin);
			case CRANE -> craneNeck(pen, a, skin);
			case ROC -> rocNeck(pen, a, skin);
			case NAGUAL -> nagualNeck(pen, a, skin);
			case BARGHEST -> barghestNeck(pen, a, skin);
			case SHADE -> shadeNeck(pen, a, skin);
			case CHIMERA -> dragonNeck(pen, a, skin);
			case BEAR -> bearNeck(pen, a, skin);
			case SERPENT -> serpentNeck(pen, a, skin);
		};
	}

	private static float[] steedNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		for (int i = 0; i < 4; i++) {
			float sy = y - 2.2f * i;
			float sz = z + 3.2f * i;
			pen.box(-3f, sy, sz, 6f, 7f, 7f, skin.base());
			if (i < 3) {
				pen.box(-1f, sy - 0.5f, sz + 7f, 2f, 7.5f, 1.8f, Mat.MANE);
				pen.box(3f, sy + 0.5f, sz + 2f, 1.2f, 6.5f, 6f, Mat.MANE);
			}
		}
		return new float[] { y - 6.6f, z + 9.6f };
	}

	private static float[] hartNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		for (int i = 0; i < 4; i++) {
			float sy = y - 1.6f * i;
			float sz = z + 3.6f * i;
			pen.box(-2.5f, sy, sz, 5f, 6f, 6f, skin.base());
		}
		return new float[] { y - 4.8f, z + 10.8f };
	}

	private static float[] elkNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 2f;
		for (int i = 0; i < 3; i++) {
			float sy = y - 2f * i;
			float sz = z + 3f * i;
			pen.box(-4f, sy, sz, 8f, 8f, 9f, skin.base());
			pen.box(-2f, sy + 1f, sz - 3f, 4f, 7f, 3.2f, Mat.ELK_DARK);
			pen.box(-1.5f, sy - 0.5f, sz + 9f, 3f, 8f, 1.5f, Mat.ELK_DARK);
		}
		return new float[] { y - 4f, z + 6f };
	}

	private static float[] craneNeck(Pen pen, Anchor a, Skin skin) {
		float[][] spine = {
				{ -2f, 23f }, { -3.2f, 25.2f }, { -4.4f, 27.4f }, { -5.6f, 29.6f },
				{ -6.8f, 31.6f }, { -9.4f, 33.2f }, { -12.2f, 34f }, { -15f, 34.4f }
		};
		for (int i = 0; i < spine.length; i++) {
			float sy = spine[i][0] + (a.neckY + 1f);
			float sz = spine[i][1] + (a.neckZ - 23f);
			pen.box(-2f, sy, sz, 4f, 4.5f, 4.2f, skin.base());
			if (i >= 4) {
				pen.box(-1.5f, sy, sz + 4.1f, 3f, 4f, 0.7f, skin.dark());
			}
		}
		return new float[] { spine[7][0] + (a.neckY + 1f), spine[7][1] + (a.neckZ - 23f) };
	}

	private static float[] rocNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 2f;
		float z = a.neckZ - 1f;
		pen.box(-4.5f, y, z, 9f, 8f, 9f, skin.base());
		pen.box(-4.5f, y - 2.5f, z + 3f, 9f, 8f, 9f, skin.base());
		pen.pair(-5.5f, y - 1f, z + 3f, 1f, 7f, 8f, skin.dark());
		return new float[] { y - 2.5f, z + 3.5f };
	}

	private static float[] nagualNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		pen.box(-4.5f, y, z, 9f, 7f, 8f, skin.base());
		pen.box(-4.5f, y - 2f, z + 2f, 9f, 7f, 8f, skin.base());
		return new float[] { y - 2f, z + 2f };
	}

	private static float[] barghestNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		for (int i = 0; i < 3; i++) {
			float sy = y - 2f * i;
			float sz = z + 2.5f * i;
			pen.box(-3f, sy, sz, 6f, 7f, 7f, skin.base());
			pen.box(-1.2f, sy, sz + 7f, 2.4f, 7f, 1f, skin.dark());
		}
		return new float[] { y - 4f, z + 5f };
	}

	private static float[] shadeNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		pen.box(-2.5f, y, z, 5f, 5f, 5f, skin.base());
		pen.box(-2.5f, y - 2f, z + 2.5f, 5f, 5f, 5f, skin.base());
		return new float[] { y - 2f, z + 2.5f };
	}

	/** The body rises in three segments to hold the head up off the ground. */
	private static float[] serpentNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY;
		float z = a.neckZ - 7f;
		// Each segment overlaps the last by a third, so the neck reads as one curve and never opens.
		float[][] rise = { { 0f, 0f }, { -3f, 3.5f }, { -6f, 7f }, { -8.5f, 10.5f }, { -10.5f, 13.5f } };
		for (int i = 0; i < rise.length; i++) {
			float sy = y + rise[i][0];
			float sz = z + rise[i][1];
			float w = 7.5f - i * 0.5f;
			pen.box(-w * 0.5f, sy - 3.5f, sz, w, 7.5f, w * 0.9f, skin.base());
			pen.box(-w * 0.3f, sy - 3.7f, sz - 0.2f, w * 0.6f, 7.9f, 1.2f, skin.pale());
		}
		return new float[] { y - 13.5f, z + 14f };
	}

	private static float[] bearNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		pen.box(-4.5f, y, z, 9f, 8f, 9f, skin.base());
		pen.box(-4.5f, y - 2.5f, z + 2f, 9f, 7f, 8f, skin.base());
		return new float[] { y - 2.5f, z + 2f };
	}

	private static float[] dragonNeck(Pen pen, Anchor a, Skin skin) {
		float y = a.neckY - 1f;
		float z = a.neckZ - 1f;
		for (int i = 0; i < 3; i++) {
			float sy = y - 2.5f * i;
			float sz = z + 3f * i;
			pen.box(-4f, sy, sz, 8f, 7f, 8f, skin.base());
			pen.box(-1.2f, sy + 1f, sz + 8f, 2.4f, 3f, 2.5f, i % 2 == 0 ? Mat.WISH : Mat.PEARL);
			pen.pair(-5f, sy + 0.5f, sz + 2f, 1f, 6f, 6f, skin.dark());
		}
		return new float[] { y - 5f, z + 6f };
	}

	/** Head builders. {@code y} and {@code z} are the socket: the skull's back sits at y, its floor near z. */
	static void head(Pen pen, Phenotype phenotype, MountMesh.Shell shell, Skin skin, float y, float z) {
		if (shell == MountMesh.Shell.CHIMERA) {
			dragon(pen, y, z, skin);
			return;
		}
		switch (phenotype.head) {
			case STEED -> steed(pen, y, z, skin);
			case HART -> {
				if (phenotype.crownHeavy || shell == MountMesh.Shell.ELK) {
					elk(pen, y, z, skin);
				} else {
					hart(pen, y, z, skin, phenotype.rack);
				}
			}
			case CAT -> {
				if (phenotype.bond == Phenotype.BondShow.NAGUAL || shell == MountMesh.Shell.NAGUAL) {
					nagual(pen, y, z, skin);
				} else {
					cat(pen, y, z, skin);
				}
			}
			case BIRD -> {
				if (shell == MountMesh.Shell.ROC || phenotype.wings == Phenotype.WingShow.ASTRAL
						|| phenotype.wings == Phenotype.WingShow.ASTRAL_FULL) {
					roc(pen, y, z, skin);
				} else {
					crane(pen, y, z, skin);
				}
			}
			case HOUND -> hound(pen, y, z, skin);
			case BEAR -> bear(pen, y, z, skin);
			case SERPENT -> serpent(pen, y, z, skin);
		}
	}

	/**
	 * An eye set flush into the head's left side, whose plane is {@code side}. The cube lies inside the
	 * skull with its iris on the skull's plane, so the skull is cut away under it and no edge shows.
	 */
	private static void eyeLeft(Pen pen, float side, float y, float z, float w, float h, Skin skin) {
		// Half a pixel back from the brow, so the eye's own front never shares the skull's front plane.
		pen.gaze(side, y + 0.5f, z, 1f, w, h, skin.dark(), skin.eye(), Pen.EYE_LEFT);
	}

	private static void eyeRight(Pen pen, float side, float y, float z, float w, float h, Skin skin) {
		pen.gaze(side - 1f, y + 0.5f, z, 1f, w, h, skin.dark(), skin.eye(), Pen.EYE_RIGHT);
	}

	private static void eyes(Pen pen, float side, float y, float z, float w, float h, Skin skin) {
		eyeLeft(pen, -side, y, z, w, h, skin);
		eyeRight(pen, side, y, z, w, h, skin);
	}

	/** A forward-facing eye set flush into the face plane {@code front}. */
	private static void eyeFront(Pen pen, float x, float front, float z, float w, float h, Skin skin) {
		pen.gaze(x, front, z, w, 1f, h, skin.dark(), skin.eye(), Pen.EYE_FRONT);
	}

	/** Two upright ears; the left one twitches. {@code x} is the outer edge of the left ear. */
	private static void ears(Pen pen, float x, float y, float z, float w, float l, float h, Mat outer, Mat inner) {
		float twitch = pen.anim.ear * 28f;
		pen.hinge(x + w * 0.5f, y + l * 0.5f, z, twitch, -twitch * 0.5f, 0f, () -> {
			pen.box(x, y, z, w, l, h, outer);
			pen.box(x + 0.3f, y - 0.4f, z + 0.8f, w - 0.6f, 0.5f, h - 1.6f, inner);
		});
		pen.box(-x - w, y, z, w, l, h, outer);
		pen.box(-x - w + 0.3f, y - 0.4f, z + 0.8f, w - 0.6f, 0.5f, h - 1.6f, inner);
	}

	private static void steed(Pen pen, float y, float z, Skin skin) {
		pen.box(-4f, y - 6f, z + 0.5f, 8f, 7.5f, 7f, skin.base());
		pen.box(-3f, y - 8f, z + 3.5f, 6f, 3f, 4f, skin.base());
		pen.box(-3f, y - 8f, z - 1f, 6f, 6f, 2f, skin.dark());
		pen.box(-2.5f, y - 13f, z - 0.5f, 5f, 7.5f, 5f, skin.base());
		pen.box(-2.5f, y - 13.5f, z - 1.5f, 5f, 5f, 2f, skin.dark());
		pen.pair(-2.2f, y - 13.4f, z + 2.6f, 1.2f, 1f, 1.4f, Mat.NOSE);
		pen.pair(-5f, y - 3f, z + 0.5f, 1f, 4f, 5f, skin.base());
		eyes(pen, 4f, y - 6f, z + 4f, 3f, 2f, skin);
		ears(pen, -3.2f, y - 2.5f, z + 7f, 2.2f, 2.2f, 4f, skin.base(), skin.dark());
		pen.box(-1.2f, y - 6.5f, z + 7.2f, 2.4f, 4.5f, 1.6f, Mat.MANE);
		pen.box(-1f, y - 8.2f, z + 5.5f, 2f, 1.4f, 2.5f, Mat.MANE);
	}

	private static void hart(Pen pen, float y, float z, Skin skin, Phenotype.RackShow rack) {
		pen.box(-3f, y - 5f, z + 0.5f, 6f, 6.5f, 6f, skin.base());
		pen.box(-2.5f, y - 7f, z + 3f, 5f, 3f, 3.5f, skin.base());
		pen.box(-2f, y - 12f, z, 4f, 7.5f, 4.5f, skin.base());
		pen.box(-2f, y - 12.5f, z - 1f, 4f, 5f, 1.6f, skin.dark());
		pen.box(-1.5f, y - 12.4f, z + 2.2f, 3f, 0.8f, 1.6f, Mat.NOSE);
		pen.pair(-1.4f, y - 12.9f, z + 2.6f, 0.8f, 0.6f, 0.8f, Mat.NOSE);
		pen.box(-2.4f, y - 7.5f, z - 1f, 4.8f, 3f, 1.5f, skin.pale());
		eyes(pen, 3f, y - 5f, z + 3f, 3f, 2f, skin);
		ears(pen, -4.6f, y - 2f, z + 5.5f, 3f, 1.6f, 5f, skin.base(), skin.pale());
		if (rack != Phenotype.RackShow.NONE) {
			antlers(pen, y - 3.5f, z + 6.4f, rack);
		}
	}

	private static void elk(Pen pen, float y, float z, Skin skin) {
		pen.box(-4.5f, y - 7f, z + 0.5f, 9f, 8f, 8f, skin.base());
		pen.box(-3.5f, y - 9.5f, z + 4f, 7f, 3f, 5f, skin.base());
		pen.box(-3f, y - 15f, z + 1f, 6f, 8f, 6f, skin.base());
		pen.box(-3f, y - 16f, z + 3.5f, 6f, 2f, 4f, skin.dark());
		pen.box(-2f, y - 15.8f, z + 4f, 4f, 0.8f, 2f, Mat.NOSE);
		pen.pair(-1.9f, y - 16.4f, z + 4.4f, 1f, 0.7f, 1f, Mat.NOSE);
		pen.box(-2.5f, y - 14.5f, z - 0.5f, 5f, 8f, 2f, skin.dark());
		pen.box(-1.5f, y - 9f, z - 3f, 3f, 4f, 3.5f, Mat.ELK_DARK);
		eyes(pen, 4.5f, y - 6.5f, z + 4.5f, 3f, 2f, skin);
		ears(pen, -6.5f, y - 3f, z + 6.5f, 3.5f, 1.8f, 4.5f, skin.base(), skin.pale());
		palms(pen, y - 4f, z + 8f);
	}

	private static void crane(Pen pen, float y, float z, Skin skin) {
		pen.box(-2f, y - 3.5f, z - 0.3f, 4f, 5f, 3.8f, skin.base());
		pen.box(-2.1f, y - 0.2f, z - 0.2f, 4.2f, 2f, 3.8f, skin.dark());
		pen.box(-1.2f, y - 2.8f, z + 3.4f, 2.4f, 2.8f, 0.5f, Mat.CROWN);
		pen.box(-0.9f, y - 7.5f, z + 0.9f, 1.8f, 4.5f, 1.7f, Mat.BEAK);
		pen.box(-0.6f, y - 11f, z + 1.2f, 1.2f, 4f, 1.2f, Mat.BEAK);
		pen.box(-0.35f, y - 13.5f, z + 1.5f, 0.7f, 3f, 0.7f, Mat.BEAK);
		pen.box(-0.7f, y - 7f, z + 0.3f, 1.4f, 4f, 0.8f, Mat.TALON);
		eyes(pen, 2f, y - 2.8f, z + 1.3f, 2f, 2f, skin);
	}

	private static void roc(Pen pen, float y, float z, Skin skin) {
		pen.box(-5f, y - 7f, z + 0.5f, 10f, 8.5f, 8f, skin.base());
		pen.box(-5.4f, y - 7.6f, z + 5.5f, 10.8f, 3.5f, 1.8f, skin.dark());
		pen.box(-3f, y - 1f, z + 8f, 6f, 3f, 2.5f, skin.dark());
		pen.box(-2f, y + 1f, z + 8.5f, 4f, 3.5f, 3f, skin.dark());
		pen.box(-2.2f, y - 13f, z + 2.5f, 4.4f, 6.5f, 4f, Mat.BEAK);
		pen.box(-1.6f, y - 15f, z - 0.5f, 3.2f, 3f, 4f, Mat.BEAK);
		pen.box(-0.9f, y - 15.4f, z - 2.2f, 1.8f, 2f, 2.2f, Mat.TALON);
		pen.box(-1.8f, y - 11.5f, z + 0.5f, 3.6f, 5.5f, 2.2f, Mat.BEAK);
		pen.box(-3f, y - 6f, z - 1f, 6f, 6f, 2f, skin.base());
		eyes(pen, 5f, y - 6f, z + 3f, 3f, 2f, skin);
	}

	private static void nagual(Pen pen, float y, float z, Skin skin) {
		pen.box(-6f, y - 6f, z, 12f, 7.5f, 7f, skin.base());
		pen.box(-5f, y - 7.5f, z + 2f, 10f, 3f, 4.5f, skin.base());
		pen.box(-2.5f, y - 10.5f, z + 0.5f, 5f, 4.5f, 3f, skin.pale());
		pen.box(-2f, y - 10.5f, z + 3f, 4f, 5f, 2f, skin.base());
		pen.box(-1.5f, y - 11f, z + 3f, 3f, 1f, 1.6f, Mat.NOSE);
		pen.box(-2f, y - 10.5f, z - 0.5f, 4f, 4f, 1.2f, skin.pale());
		pen.box(-4.5f, y - 7f, z - 1.5f, 9f, 6f, 2.5f, skin.base());
		pen.pair(-6.5f, y - 4f, z - 0.5f, 1f, 5f, 4f, skin.pale());
		ears(pen, -5f, y - 1.5f, z + 6.5f, 3f, 2f, 3f, skin.base(), skin.pale());
		pen.pair(-5f, y + 0.4f, z + 6.5f, 3f, 0.4f, 3f, skin.dark());
		eyeFront(pen, -4.5f, y - 7.5f, z + 3.5f, 3f, 2f, skin);
		eyeFront(pen, 1.5f, y - 7.5f, z + 3.5f, 3f, 2f, skin);
	}

	private static void cat(Pen pen, float y, float z, Skin skin) {
		pen.box(-3.5f, y - 5f, z, 7f, 6f, 5.5f, skin.base());
		pen.box(-3f, y - 6f, z + 2f, 6f, 2.5f, 3.5f, skin.base());
		pen.box(-2f, y - 8f, z + 0.5f, 4f, 3.5f, 3f, skin.pale());
		pen.box(-1f, y - 8.4f, z + 2.4f, 2f, 0.8f, 1.2f, Mat.NOSE);
		pen.box(-1.5f, y - 8f, z - 0.2f, 3f, 3f, 1f, skin.pale());
		pen.pair(-4f, y - 4f, z - 0.2f, 0.8f, 4f, 3f, skin.pale());
		ears(pen, -3.6f, y - 1.5f, z + 5f, 3f, 1.5f, 4.5f, skin.base(), skin.pale());
		pen.pair(-3.6f, y, z + 5f, 3f, 0.4f, 4.5f, skin.dark());
		eyeFront(pen, -3f, y - 6f, z + 2.6f, 2f, 2f, skin);
		eyeFront(pen, 1f, y - 6f, z + 2.6f, 2f, 2f, skin);
	}

	private static void hound(Pen pen, float y, float z, Skin skin) {
		pen.box(-4f, y - 6f, z, 8f, 7f, 6.5f, skin.base());
		pen.box(-3.5f, y - 7.5f, z + 3.5f, 7f, 2.5f, 3f, skin.base());
		pen.box(-2.5f, y - 14f, z + 0.5f, 5f, 8.5f, 4f, skin.base());
		pen.box(-1.4f, y - 14.3f, z + 2.9f, 2.8f, 0.7f, 1.5f, Mat.NOSE);
		pen.pair(-1.3f, y - 14.7f, z + 3.2f, 0.9f, 0.5f, 0.8f, Mat.NOSE);
		pen.box(-2.2f, y - 13f, z - 0.8f, 4.4f, 6f, 1.6f, skin.dark());
		pen.box(-2.6f, y - 13.5f, z + 0.3f, 5.2f, 6f, 0.6f, skin.dark());
		pen.pair(-1.9f, y - 13.2f, z - 0.9f, 0.7f, 0.7f, 1.2f, Mat.PEARL);
		float flap = pen.anim.ear * 30f;
		pen.hinge(-4f, y - 2f, z + 6f, 0f, -flap, 0f, () -> pen.box(-5.8f, y - 3.5f, z - 1f, 1.8f, 3.2f, 7.5f, skin.dark()));
		pen.box(4f, y - 3.5f, z - 1f, 1.8f, 3.2f, 7.5f, skin.dark());
		eyes(pen, 4f, y - 6f, z + 3.5f, 3f, 2f, skin);
	}

	/**
	 * A flat wedge of a snake's head with slit eyes, a forked tongue, and a frill fanned out behind
	 * the jaw like a frilled lizard: a ring of red membrane between ribs of dark scale.
	 */
	private static void serpent(Pen pen, float y, float z, Skin skin) {
		pen.box(-4f, y - 9f, z, 8f, 10f, 4.5f, skin.base());
		pen.box(-3f, y - 12.5f, z + 0.3f, 6f, 4f, 3.6f, skin.base());
		pen.box(-3.2f, y - 9.2f, z - 0.4f, 6.4f, 10.4f, 0.8f, skin.pale());
		pen.box(-2.4f, y - 12.6f, z - 0.2f, 4.8f, 4f, 0.8f, skin.pale());
		pen.pair(-2.6f, y - 12.8f, z + 2.6f, 0.8f, 0.6f, 0.8f, Mat.NOSE);
		float flick = pen.anim.ear;
		pen.hinge(0f, y - 12.5f, z + 1.2f, 0f, 0f, -flick * 20f, () -> pen.shift(0f, flick * 1.5f, 0f, () -> {
			pen.box(-0.3f, y - 17f, z + 1f, 0.6f, 4.5f, 0.5f, Mat.FRILL);
			pen.pair(-1f, y - 17.5f, z + 1f, 0.8f, 1.2f, 0.5f, Mat.FRILL);
		}));
		eyes(pen, 4f, y - 8.5f, z + 2.4f, 3f, 1.6f, skin);
		// The frill: a ring of ribs around the back of the skull with skin between them.
		// Eleven ribs over two thirds of a circle, from below one jaw over the crown to below the other,
		// each a rib of dark scale with a web of red skin. It opens wider as the serpent moves.
		float open = 0.7f + 0.3f * pen.anim.amount;
		int ribs = 11;
		for (int i = 0; i < ribs; i++) {
			int rib = i;
			float angle = -120f + i * 24f;
			float crest = 1f - Math.abs(rib - 5) / 5f;
			float reach = 8.5f + 2.5f * crest;
			// Neighbouring webs overlap near the skull; each sits a hair further back so none share a plane.
			float layer = rib * 0.06f;
			pen.hinge(0f, y + 0.5f, z + 2.2f, 0f, angle * open, 0f, () -> {
				pen.box(-0.6f, y + 0.5f + layer, z + 2.2f, 1.2f, 1f, reach + 1.2f, skin.dark());
				pen.box(-2.9f, y + 0.8f + layer, z + 3f, 5.8f, 0.4f, reach, Mat.FRILL);
			});
		}
	}

	/** A broad round skull, a short pale muzzle, small eyes, and round ears on top. */
	private static void bear(Pen pen, float y, float z, Skin skin) {
		pen.box(-4.5f, y - 6f, z, 9f, 7.5f, 7.5f, skin.base());
		pen.box(-3f, y - 11f, z + 0.5f, 6f, 6f, 4.5f, skin.pale());
		pen.box(-2.5f, y - 11.5f, z + 0.3f, 5f, 2f, 4f, skin.pale());
		pen.box(-1.4f, y - 11.4f, z + 2.9f, 2.8f, 0.7f, 1.5f, Mat.NOSE);
		pen.pair(-1.3f, y - 11.8f, z + 3.2f, 0.9f, 0.5f, 0.8f, Mat.NOSE);
		pen.box(-2.5f, y - 10.5f, z - 0.8f, 5f, 5f, 1.2f, skin.dark());
		ears(pen, -4.6f, y - 2f, z + 7f, 2.6f, 2.2f, 2.6f, skin.base(), skin.pale());
		eyes(pen, 4.5f, y - 5f, z + 3.5f, 2f, 2f, skin);
	}

	private static void dragon(Pen pen, float y, float z, Skin skin) {
		// Furred skull, a scaled snout that tapers, horns swept back, and fin ears.
		pen.box(-5f, y - 7f, z + 0.5f, 10f, 8f, 8f, skin.base());
		pen.box(-4f, y - 8.5f, z + 3.5f, 8f, 3f, 5f, skin.base());
		pen.pair(-4.6f, y - 9f, z + 6.2f, 3f, 2.5f, 1.6f, skin.star());
		pen.box(-3f, y - 14f, z + 1f, 6f, 7.5f, 5f, skin.rosette());
		pen.box(-2.4f, y - 15.5f, z + 1.4f, 4.8f, 2f, 4f, skin.rosette());
		pen.box(-2.2f, y - 13.5f, z + 6f, 4.4f, 5.5f, 1f, skin.base());
		pen.pair(-1.9f, y - 15.8f, z + 4.2f, 0.8f, 0.6f, 0.8f, Mat.NOSE);
		pen.box(-2.8f, y - 14.5f, z - 1f, 5.6f, 7.5f, 2.2f, skin.star());
		pen.pair(-2.4f, y - 14.2f, z - 0.4f, 0.8f, 0.8f, 1.6f, Mat.PEARL);
		pen.pair(-1.2f, y - 11f, z - 0.4f, 0.6f, 0.6f, 1.2f, Mat.PEARL);
		pen.box(-1.5f, y - 9f, z - 3.2f, 3f, 3f, 2.6f, skin.dark());
		pen.pair(-4.2f, y - 1.5f, z + 8f, 2.2f, 2.2f, 4f, Mat.HORN);
		pen.pair(-4.6f, y + 0.5f, z + 11f, 1.8f, 3.5f, 1.8f, Mat.HORN);
		pen.pair(-4.8f, y + 3.2f, z + 12f, 1.4f, 3.5f, 1.4f, Mat.HORN);
		pen.pair(-6.4f, y - 2.5f, z + 3.5f, 1.4f, 4.5f, 4f, Mat.MEMBRANE);
		pen.box(-1.5f, y - 2f, z + 8.2f, 3f, 4f, 2.2f, skin.dark());
		eyeLeft(pen, -5f, y - 6.5f, z + 3.8f, 3f, 2f, skin);
		eyeRight(pen, 5f, y - 6.5f, z + 3.8f, 3f, 2f, skin);
	}

	private static void antlers(Pen pen, float y, float z, Phenotype.RackShow kind) {
		float beam = switch (kind) {
			case CROWN -> 12f;
			case FULL -> 9f;
			default -> 0f;
		};
		for (int sign = -1; sign <= 1; sign += 2) {
			if (kind == Phenotype.RackShow.BUDS) {
				pen.box(sign < 0 ? -3f : 1f, y + 1f, z, 2f, 2f, 3f, Mat.ANTLER);
				continue;
			}
			float bx = sign < 0 ? -4f : 2f;
			pen.box(bx, y, z, 2f, 2f, beam, Mat.ANTLER);
			pen.box(bx, y - 2.5f, z + 2f, 2f, 3f, 1.6f, Mat.ANTLER);
			pen.box(bx + (sign < 0 ? -0.5f : 0.5f), y - 4f, z + 3f, 1.6f, 2f, 3.5f, Mat.ANTLER);
			if (sign < 0) {
				pen.box(bx - 4f, y, z + 6f, 4f, 2f, 2f, Mat.ANTLER);
				pen.box(bx - 4f, y, z + 7f, 1.8f, 1.8f, 4f, Mat.ANTLER);
			} else {
				pen.box(bx + 2f, y, z + 6f, 4f, 2f, 2f, Mat.ANTLER);
				pen.box(bx + 4.2f, y, z + 7f, 1.8f, 1.8f, 4f, Mat.ANTLER);
			}
			if (kind == Phenotype.RackShow.CROWN) {
				pen.box(bx, y, z + beam - 2f, 2f, 2f, 5f, Mat.ANTLER);
				if (sign < 0) {
					pen.box(bx - 3f, y + 1f, z + beam, 3f, 2f, 2f, Mat.ANTLER);
					pen.box(bx - 3f, y + 1f, z + beam + 1f, 1.8f, 1.8f, 4f, Mat.ANTLER);
				} else {
					pen.box(bx + 2f, y + 1f, z + beam, 3f, 2f, 2f, Mat.ANTLER);
					pen.box(bx + 3.2f, y + 1f, z + beam + 1f, 1.8f, 1.8f, 4f, Mat.ANTLER);
				}
			}
		}
	}

	private static void palms(Pen pen, float y, float z) {
		for (int sign = -1; sign <= 1; sign += 2) {
			float bx = sign < 0 ? -3f : 1f;
			pen.box(bx, y - 1f, z, 2f, 2f, 5f, Mat.ANTLER);
			if (sign < 0) {
				pen.box(bx - 9f, y - 4f, z + 4f, 11f, 7f, 2f, Mat.ANTLER);
				float[][] tips = { { -9f, -4f }, { -7f, -6f }, { -3f, -7f }, { 0f, -4f } };
				for (float[] tip : tips) {
					pen.box(bx + tip[0], y + tip[1], z + 5f, 2f, 2f, 3f, Mat.ANTLER);
				}
			} else {
				pen.box(bx, y - 4f, z + 4f, 11f, 7f, 2f, Mat.ANTLER);
				float[][] tips = { { 7f, -4f }, { 5f, -6f }, { 1f, -7f }, { -2f, -4f } };
				for (float[] tip : tips) {
					pen.box(bx + tip[0], y + tip[1], z + 5f, 2f, 2f, 3f, Mat.ANTLER);
				}
			}
		}
	}
}
