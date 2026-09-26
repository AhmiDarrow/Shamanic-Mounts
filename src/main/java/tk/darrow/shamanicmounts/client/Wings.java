package tk.darrow.shamanicmounts.client;

/**
 * Shoulder, elbow, and wrist. A feathered wing is a slim leading edge with feathers hanging back from it
 * ({@link #bird}); a membrane wing is long fingers with skin between them. The tip lags the shoulder and a
 * downstroke spreads the feathers. A perched wing folds back along the flank.
 */
final class Wings {
	/** How far each feather or web sits above its neighbour, in pixels. */
	private static final float LAYER = 0.15f;
	/** The folded wing: swung this far back, turned this far onto its edge, drooped this far at the tip. */
	private static final float FOLD_BACK = 101.0f, FOLD_EDGE = 82.0f, FOLD_DROOP = -10.0f, FOLD_ELBOW = 168.0f, FOLD_WRIST = 181.0f;

	private Wings() {
	}

	static void feathered(Pen pen, Anchor anchor, float span, float chord, Mat mat, Mat dark, Mat primary, int feathers) {
		float reach = Math.max(8f, anchor.tailY - anchor.wingY);
		bird(pen, true, -anchor.half, anchor.wingY, anchor.wingZ, span, chord, reach, mat, dark, primary, feathers);
		bird(pen, false, anchor.half, anchor.wingY, anchor.wingZ, span, chord, reach, mat, dark, primary, feathers);
	}

	/**
	 * A soaring bird's wing, after the condor and the eagle: broad from body to tip, with a deep, nearly straight
	 * trailing edge of secondaries, three rows of coverts over their roots, and a wide flare of long primaries at the
	 * hand. Each primary narrows toward its tip (the emargination that slots a soaring wing), so spread in flight the
	 * tips stand apart as fingers, and they curl upward as a soaring bird's do. Folded, every feather swings in line
	 * with the arm and the wing lies along the flank.
	 *
	 * <p>Box axes are blender pixels: x along the span, y front (-) to back (+), z up. Feathers lie in the wing's
	 * plane, a hair above one another so no two share a face.
	 */
	private static void bird(Pen pen, boolean left, float hx, float hy, float hz, float span, float chord, float reach, Mat mat,
			Mat dark, Mat primary, int feathers) {
		MountPose anim = pen.anim;
		int dir = left ? -1 : 1;
		float sign = left ? 1.0f : -1.0f;
		float perch = 1.0f - anim.open;
		if (perch >= 0.5f) {
			// At rest the wing is not the flight skeleton bent back: it is drawn as it lies (see folded).
			folded(pen, dir, sign, hx, hy, hz, chord, reach, mat, dark, primary, anim.breath);
			return;
		}
		float fly = anim.open;
		int pose = anim.wingPose;
		float freq = pose == 2 ? 0.42f : 0.28f;
		float power = pose == 2 ? 1.0f : pose == 1 ? 0.5f : 0.0f;
		float t = anim.age * freq;
		float down = Math.max(0.0f, -(float) Math.cos(t)) * power;
		float shoulderFlap = (float) Math.sin(t) * 34.0f * power;
		float elbowFlap = (float) Math.sin(t - 0.9f) * 42.0f * power;
		float wristFlap = (float) Math.sin(t - 1.7f) * 48.0f * power;
		float crescent = fly * (pose == 1 ? 10.0f : 4.0f);
		float settle = perch * anim.breath * 1.2f;
		// Standing, a condor's wing is folded in a Z against its side: the upper arm runs back from the shoulder to the
		// elbow, the forearm folds forward along the top of the flank to the wrist, and the hand runs back again with
		// the primaries past the tail. That fold is what turns the wing's upper side, coverts and all, outward, with the
		// flight feathers hanging down and back over the flank. The upper arm turns on edge (the same way for both
		// wings: it is a turn about the body's long axis, which a mirror does not flip) and the elbow and wrist each
		// fold the next bone back on itself.
		float shoulderYaw = sign * (perch * FOLD_BACK + crescent * 0.35f);
		float shoulderRoll = sign * (perch * FOLD_DROOP - fly * (pose == 1 ? 4.0f : 0.0f) + shoulderFlap + settle);
		float shoulderPitch = -perch * FOLD_EDGE;
		float elbowYaw = sign * (perch * FOLD_ELBOW + crescent);
		float elbowRoll = sign * elbowFlap;
		float wristYaw = sign * (-perch * FOLD_WRIST + crescent * 0.3f);
		float wristRoll = sign * wristFlap;
		// Folded, the arm's three bones lie over one another: the wing is about as long as the body, not the span.
		float foldSpan = 1.0f - 0.15f * perch, foldDeep = 1.0f - 0.3f * perch;
		// A condor's arm is most of its span; the hand is short and the primaries make the tip.
		float humerus = Math.max(4f, span * 0.30f);
		float forearm = Math.max(4f, span * 0.36f);
		float hand = Math.max(3f, span - humerus - forearm);
		// The depth of the wing: long secondaries, so the inner wing is broad and its trailing edge straight.
		float deep = chord * 1.7f;
		int primaries = Math.max(6, feathers + 1);
		// Folded, the forearm's feathers hang down and lean back along the body (the forearm points forward, so
		// back is against its span); the upper arm's point up in the fold, so they turn right over to hang down too.
		float tuck = sign * perch * 74.0f;
		float armTuck = sign * perch * 145.0f;
		boolean folded = perch > 0.5f;
		pen.shift(hx + dir * 1.4f * perch, hy + 1.0f * perch, hz + 3.0f * perch, () -> pen.curl(shoulderYaw, shoulderRoll, shoulderPitch,
				() -> pen.scaleAt(0f, 0f, 0f, foldSpan, foldDeep, 1.0f, () -> {
			bone(pen, dir, humerus, 2.4f, mat);
			if (!folded) {
				coverts(pen, dir, humerus, 2.4f, deep * 0.42f, 1.0f, mat, dark, 0.3f);
				coverts(pen, dir, humerus, 2.0f, deep * 0.22f, 1.3f, mat, dark, 0.3f);
			}
			// Tertials: the long feathers nearest the body, a little shorter than the secondaries, filling the join.
			int tertials = Math.max(3, Math.round(humerus / 2.2f));
			for (int i = 0; i < tertials; i++) {
				float along = (i + 0.5f) / tertials;
				float length = deep * (0.72f + 0.26f * along);
				back(pen, dir, humerus * along, length, 3.8f, 0.2f + i * LAYER, armTuck, mat, dark);
			}
			pen.shift(dir * (humerus - 1f), 0.0f, 0.0f, () -> pen.curl(elbowYaw, elbowRoll, 0.0f, () -> {
				bone(pen, dir, forearm, 2.2f, mat);
				// Secondaries: a broad, even trailing edge, one broad feather every two pixels.
				int secondaries = Math.max(4, Math.round(forearm / 2.0f));
				for (int i = 0; i < secondaries; i++) {
					float along = (i + 0.5f) / secondaries;
					float length = deep * (0.98f + 0.04f * along);
					float fan = sign * fly * (along - 0.5f) * (3.0f + down * 5.0f);
					back(pen, dir, forearm * along, length, 3.8f, i * LAYER, tuck + fan, mat, dark);
				}
				// Lesser, median and greater coverts, each row longer and lying lower over the flight feathers.
				float top = 1.1f + secondaries * LAYER;
				coverts(pen, dir, forearm, 2.4f, deep * 0.52f, top, mat, dark, 0.3f);
				coverts(pen, dir, forearm, 2.0f, deep * 0.34f, top + 0.3f, mat, dark, 0.3f);
				coverts(pen, dir, forearm, 1.6f, deep * 0.18f, top + 0.6f, mat, dark, 0.3f);
				pen.shift(dir * (forearm - 1f), 0.0f, 0.0f, () -> pen.curl(wristYaw, wristRoll, 0.0f, () -> {
					bone(pen, dir, hand, 1.8f, mat);
					// Primary coverts: a broad base over the roots of the flare (tucked under the forearm when folded).
					if (!folded) coverts(pen, dir, hand, 2.4f, deep * 0.5f, 1.3f + primaries * LAYER, dark, dark, 0.3f);
					// The flare. The inner primaries carry the trailing edge on, the outer ones reach out past the hand,
					// longest just inside the tip, so the tip is broad and rounded. Spread, the tips stand apart as
					// fingers; folded, they close together into a point.
					for (int i = 0; i < primaries; i++) {
						int feather = i;
						float along = (float) i / (primaries - 1);
						float spread = 0.2f + 0.8f * fly;
						// Folded, the hand points back with its upper side in, so the primaries lean a little the other
						// way (down, over the flank) and close together.
						float sweep = sign * ((1f - along) * 72f * spread * fly + 4f * fly - perch * (3f + (1f - along) * 6f)
								+ down * (1f - along) * 8f);
						float lag = sign * (float) Math.sin(t - 2.1f - feather * 0.28f) * 18.0f * power;
						float length = deep * (1.0f + 0.62f * (float) Math.sin(Math.PI * 0.5 * Math.min(1f, along / 0.8f))
								- (along > 0.86f ? 0.18f : 0f));
						float width = 3.6f - along * 0.6f;
						// Soaring, the outer fingers bend upward, most at the very tip.
						float lift = sign * fly * (6f + 16f * along) * (pose == 2 ? 0.5f : 1f);
						float layer = feather * LAYER;
						Mat vane = dark;
						pen.shift(dir * hand * (0.15f + 0.85f * along), 0.4f, 0.0f, () -> pen.curl(sweep, lag, 0.0f,
								() -> finger(pen, dir, length, width, layer, lift, vane, primary)));
					}
				}));
			}));
		})));
	}

	/**
	 * A wing folded at rest, as a standing vulture or eagle carries it: one smooth, leaf-shaped shield lying flat on
	 * the flank from the shoulder to a point past the tail. Shingled like the real thing, each layer a hair further
	 * out than the one it covers: the primaries underneath, reaching back past the tail to a point; the secondaries
	 * over them, a broad dark panel over the lower flank whose tips step down and back; a paler band of greater
	 * coverts; scalloped lesser coverts at the shoulder; and the rounded leading edge along the top. Everything sits
	 * against the body, so from behind the two wings hug the flanks and the tips close over the tail.
	 *
	 * <p>Measured from the shoulder: {@code reach} back to the tail, {@code chord} for the depth of the flank it covers.
	 */
	private static void folded(Pen pen, int dir, float sign, float hx, float hy, float hz, float chord, float reach, Mat mat,
			Mat dark, Mat primary, float breath) {
		float length = reach + 12f;
		float deep = Math.min(11f, chord * 0.95f);
		float top = deep * 0.45f, bottom = -deep * 0.55f;
		float lift = breath * 0.3f;
		pen.shift(hx, hy - 2f, hz + lift, () -> {
			// Primaries: underneath everything, long and narrow, closing to a point past the tail and leaning in.
			for (int k = 0; k < 4; k++) {
				float z = bottom * 0.35f + k * 1.2f;
				float start = length * (0.32f + k * 0.03f);
				plate(pen, dir, 0.2f + k * 0.08f, start, z, length - start - k * 1.2f, 2.0f, 2f - k * 1.5f, sign * (3f + k * 1.5f), dark, primary);
			}
			// Secondaries: a broad dark panel over the lower flank, each lower one starting a little later and
			// reaching a little further, so the trailing edge steps down and back.
			int rows = 6;
			for (int k = 0; k < rows; k++) {
				float z = top * 0.1f - k * ((top * 0.1f - bottom) / (rows - 0.5f));
				float start = length * (0.10f + k * 0.025f);
				float run = length * (0.50f + k * 0.035f);
				plate(pen, dir, 0.6f + (rows - k) * 0.06f, start, z, run, 2.4f, 9f, sign * 1.5f, dark, k % 2 == 0 ? primary : dark);
			}
			// Greater coverts: a paler band across the middle of the wing.
			int band = Math.max(4, Math.round(length * 0.42f / 2.4f));
			for (int k = 0; k < band; k++) {
				float y = length * 0.06f + k * 2.4f;
				plate(pen, dir, 0.9f + k * 0.02f, y, top * 0.2f, length * 0.16f, 2.8f, 22f, 0f, mat, mat);
			}
			// Lesser coverts: small scallops near the shoulder, two rows.
			for (int row = 0; row < 2; row++) {
				int count = 5 - row;
				for (int k = 0; k < count; k++) {
					float y = row * 1.2f + k * 2.2f;
					plate(pen, dir, 1.1f + row * 0.15f + k * 0.02f, y, top * (0.55f + row * 0.3f), 3.0f, 2.2f, 28f, 0f,
							mat, dark);
				}
			}
			// The leading edge: the folded wrist and forearm along the top, rounded at the front.
			float edge = length * 0.34f;
			pen.box(dir < 0 ? -2.0f : 0.0f, 0.5f, top, 2.0f, edge, 1.6f, mat);
			pen.box(dir < 0 ? -1.8f : 0.0f, -0.6f, top - 1.2f, 1.8f, 1.6f, 2.4f, mat);
		});
	}

	/**
	 * One feather or covert lying flat on the flank: a vane {@code run} long and {@code tall} deep, pointing back from
	 * {@code y}, tipped down by {@code tip} degrees and turned toward the tail's centre by {@code turn}, with a narrower
	 * tip. {@code out} is how far it stands off the body, so later layers lie over earlier ones.
	 */
	private static void plate(Pen pen, int dir, float out, float y, float z, float run, float tall, float tip, float turn,
			Mat vane, Mat end) {
		pen.shift(dir * out, y, z, () -> pen.curl(turn, 0.0f, tip, () -> {
			float x = dir < 0 ? -0.4f : 0.0f;
			pen.box(x, 0f, -tall * 0.5f, 0.4f, run * 0.8f, tall, vane);
			pen.box(x, run * 0.8f, -tall * 0.32f, 0.4f, run * 0.2f, tall * 0.64f, end);
		}));
	}

	/**
	 * A primary pointing out along the span: a broad base, then the emarginated outer half, narrow as a finger, bent
	 * up by {@code lift} degrees at the notch, and a pale shaft.
	 */
	private static void finger(Pen pen, int dir, float length, float width, float z, float lift, Mat vane, Mat shaft) {
		float base = length * 0.5f, outer = length * 0.5f;
		pen.box(dir < 0 ? -base : 0f, -width * 0.5f, z, base, width, 0.8f, vane);
		pen.box(dir < 0 ? -base : 0f, -0.25f, z + 0.8f, base, 0.5f, 0.2f, shaft);
		pen.shift(dir * base, 0.0f, z, () -> pen.curl(0.0f, lift, 0.0f, () -> {
			float mid = outer * 0.7f, tip = outer * 0.3f;
			pen.box(dir < 0 ? -mid : 0f, -width * 0.3f, 0f, mid, width * 0.6f, 0.8f, vane);
			pen.box(dir < 0 ? -mid - tip : mid, -width * 0.18f, 0f, tip, width * 0.36f, 0.8f, shaft);
			pen.box(dir < 0 ? -outer * 0.95f : 0f, -0.2f, 0.8f, outer * 0.95f, 0.4f, 0.2f, shaft);
		}));
	}

	/** The arm bone: the wing's leading edge, slim and a little rounded by a narrower cap on top. */
	private static void bone(Pen pen, int dir, float length, float thick, Mat mat) {
		float x = dir < 0 ? -length - 1f : -1f;
		pen.box(x, -1.4f, -thick * 0.5f, length + 2f, 2.2f, thick, mat);
		pen.box(x + 0.4f, -1.0f, thick * 0.5f, length + 1.2f, 1.4f, 0.4f, mat);
	}

	/** A row of short overlapping coverts behind the leading edge, alternating shades. */
	private static void coverts(Pen pen, int dir, float length, float every, float deep, float z, Mat a, Mat b, float thick) {
		int count = Math.max(2, Math.round(length / every));
		float slot = length / count;
		for (int i = 0; i < count; i++) {
			float cx = dir * slot * (i + 0.5f);
			float width = slot + 0.8f;
			pen.box(cx - width * 0.5f, 0.3f, z + i * 0.02f, width, deep, thick, a);
			pen.box(cx - width * 0.3f, 0.3f + deep, z + i * 0.02f, width * 0.6f, 0.8f, thick, b);
		}
	}

	/** A broad flight feather hanging back from the arm: a wide vane, a rounded tip, a dark band across the end. */
	private static void back(Pen pen, int dir, float at, float length, float width, float z, float yaw, Mat vane, Mat tip) {
		pen.shift(dir * at, 0.4f, z, () -> pen.curl(yaw, 0.0f, 0.0f, () -> {
			pen.box(-width * 0.5f, 0f, 0f, width, length * 0.78f, 0.8f, vane);
			pen.box(-width * 0.5f, length * 0.78f, 0f, width, length * 0.12f, 0.8f, tip);
			pen.box(-width * 0.35f, length * 0.9f, 0f, width * 0.7f, length * 0.1f, 0.8f, tip);
		}));
	}

	static void membrane(Pen pen, Anchor anchor, float span, float chord, Mat bone, Mat skin, int fingers) {
		one(pen, true, -anchor.half, anchor.wingY, anchor.wingZ, span, chord, bone, bone, skin, fingers, true);
		one(pen, false, anchor.half, anchor.wingY, anchor.wingZ, span, chord, bone, bone, skin, fingers, true);
	}

	private static void one(Pen pen, boolean left, float hx, float hy, float hz, float span, float chord, Mat mat, Mat dark,
			Mat primary, int feathers, boolean membrane) {
		MountPose anim = pen.anim;
		int dir = left ? -1 : 1;
		float sign = left ? 1.0f : -1.0f;
		float perch = 1.0f - anim.open;
		float fly = anim.open;
		int pose = anim.wingPose;
		float freq = pose == 2 ? 0.42f : 0.28f;
		float power = pose == 2 ? 1.0f : pose == 1 ? 0.5f : 0.0f;
		float t = anim.age * freq;
		float down = Math.max(0.0f, -(float) Math.cos(t)) * power;
		float shoulderFlap = (float) Math.sin(t) * 34.0f * power;
		float elbowFlap = (float) Math.sin(t - 0.9f) * 42.0f * power;
		float wristFlap = (float) Math.sin(t - 1.7f) * 48.0f * power;
		float crescent = fly * (pose == 1 ? 14.0f : 6.0f);
		// A perched wing settles with a slow breath.
		float settle = perch * anim.breath * 2.0f;
		// Folded, the wing lies back along the flank: swept almost to the tail and dropped a little.
		float shoulderYaw = sign * (perch * 72.0f + crescent * 0.35f);
		float shoulderRoll = sign * (-perch * 34.0f - fly * (pose == 1 ? 6.0f : 0.0f) + shoulderFlap + settle);
		float elbowYaw = sign * (perch * 28.0f + crescent);
		float elbowRoll = sign * (perch * 10.0f + elbowFlap);
		float wristYaw = sign * (perch * 10.0f + crescent * 0.3f);
		float wristRoll = sign * (perch * 6.0f + wristFlap);
		float humerus = Math.max(4f, span * 0.36f);
		float forearm = Math.max(4f, span * 0.32f);
		float hand = Math.max(3f, span - humerus - forearm);
		float half = Math.max(2f, chord * 0.5f);
		float covertSlot = feathers <= 2 ? 0.0f : (chord - 1f) / (feathers - 2);
		float primarySlot = feathers <= 1 ? 0.0f : (chord - 2f) / (feathers - 1);
		int coverts = membrane ? 0 : Math.max(0, feathers - 1);
		pen.shift(hx, hy, hz, () -> pen.curl(shoulderYaw, shoulderRoll, 0.0f, () -> {
			pen.box(dir < 0 ? -humerus : -4f, -half, -1.5f, humerus + 4f, chord, 3.5f, mat);
			// Shoulder coverts lie over the root of the wing.
			pen.box(dir < 0 ? -humerus * 0.7f : -3f, -half + 0.5f, 1.5f, humerus * 0.7f + 3f, chord - 1f, 1f, dark);
			pen.shift(dir * (humerus - 3f), 0.0f, 1.0f, () -> pen.curl(elbowYaw, elbowRoll, 0.0f, () -> {
				pen.box(dir < 0 ? -forearm : -3f, -half - 1f, -1f, forearm + 3f, chord + 2f, 2.5f, mat);
				pen.box(dir < 0 ? -forearm : -3f, -1f, 1.2f, forearm + 3f, 2.5f, 1.5f, dark);
				for (int i = 0; i < coverts; i++) {
					int covert = i;
					float fromMid = covert - (coverts - 1) * 0.5f;
					float fan = sign * fly * (fromMid * (2.2f + down * 4.0f));
					float lag = sign * (float) Math.sin(t - 1.15f - covert * 0.2f) * 16.0f * power;
					float length = 3f + covert * 1.2f;
					// Each covert lies a hair above the one before, like real feathers, so neighbours
					// never share a plane and flicker.
					float layer = covert * LAYER;
					pen.shift(dir * (forearm - 3f), fromMid * covertSlot, 0.0f, () -> pen.curl(fan, lag, 0.0f, () ->
							pen.box(dir < 0 ? -length : -2f, -1f, layer, length + 2f, 2f, 1f, dark)));
				}
				if (membrane) {
					// The web along the forearm, no deeper than the forearm itself.
					pen.box(dir < 0 ? -forearm : -3f, -half - 0.5f, -0.3f, forearm + 3f, chord + 1f, 0.5f, primary);
				}
				pen.shift(dir * (forearm - 3f), 1.0f, 0.0f, () -> pen.curl(wristYaw, wristRoll, 0.0f, () -> {
					pen.box(dir < 0 ? -hand : -3f, -half, -1f, hand + 3f, chord, 2f, mat);
					for (int i = 0; i < feathers; i++) {
						int feather = i;
						float fromMid = feather - (feathers - 1) * 0.5f;
						float fan = sign * fly * (fromMid * (3.5f + down * 9.0f));
						float lag = sign * (float) Math.sin(t - 2.1f - feather * 0.28f) * 30.0f * power;
						float length = membrane ? 6f + feather * 3f : 4f + feather * 2.2f;
						float wide = membrane ? 1.2f : 3f;
						float thin = membrane ? 1.2f : 1.6f;
						Mat vane = membrane ? mat : feather % 2 == 0 ? primary : dark;
						// Primaries and webs stack a hair apart for the same reason as the coverts.
						float layer = feather * LAYER;
						pen.shift(dir * (hand - 3f), fromMid * primarySlot, 0.0f, () -> pen.curl(fan, lag, 0.0f, () -> {
							pen.box(dir < 0 ? -length : -3f, -wide * 0.5f, -thin * 0.5f + layer, length + 3f, wide, thin, vane);
							if (membrane) {
								// Each finger carries its own web behind it, the length of the finger, so the
								// wing fans open and folds without a sheet poking through.
								float web = Math.max(2f, primarySlot + 1.6f);
								pen.box(dir < 0 ? -length : -3f, wide * 0.5f - 0.2f, -0.25f + layer, length + 3f, web, 0.5f, primary);
								pen.box(dir < 0 ? -length : -3f, -0.3f, -1.1f + layer, length + 3f, 0.6f, 0.4f, Mat.HORN);
							}
						}));
					}
				}));
			}));
		}));
	}
}
