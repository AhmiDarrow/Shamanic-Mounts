package tk.darrow.shamanicmounts.client;

/**
 * Shoulder, elbow, and wrist, then coverts and primaries. The tip lags the shoulder and a downstroke
 * spreads the feathers. A perched wing folds back along the flank with the primaries crossed over
 * the tail. A membrane wing swaps feathers for long fingers with skin between them.
 */
final class Wings {
	private Wings() {
	}

	static void feathered(Pen pen, Anchor anchor, float span, float chord, Mat mat, Mat dark, Mat primary, int feathers) {
		one(pen, true, -anchor.half, anchor.wingY, anchor.wingZ, span, chord, mat, dark, primary, feathers, false);
		one(pen, false, anchor.half, anchor.wingY, anchor.wingZ, span, chord, mat, dark, primary, feathers, false);
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
					pen.shift(dir * (forearm - 3f), fromMid * covertSlot, 0.0f, () -> pen.curl(fan, lag, 0.0f, () ->
							pen.box(dir < 0 ? -length : -2f, -1f, 0f, length + 2f, 2f, 1f, dark)));
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
						pen.shift(dir * (hand - 3f), fromMid * primarySlot, 0.0f, () -> pen.curl(fan, lag, 0.0f, () -> {
							pen.box(dir < 0 ? -length : -3f, -wide * 0.5f, -thin * 0.5f, length + 3f, wide, thin, vane);
							if (membrane) {
								// Each finger carries its own web behind it, the length of the finger, so the
								// wing fans open and folds without a sheet poking through.
								float web = Math.max(2f, primarySlot + 1.6f);
								pen.box(dir < 0 ? -length : -3f, wide * 0.5f - 0.2f, -0.25f, length + 3f, web, 0.5f, primary);
								pen.box(dir < 0 ? -length : -3f, -0.3f, -1.1f, length + 3f, 0.6f, 0.4f, Mat.HORN);
							}
						}));
					}
				}));
			}));
		}));
	}
}
