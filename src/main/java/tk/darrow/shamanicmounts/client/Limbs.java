package tk.darrow.shamanicmounts.client;

/**
 * Legs with a hip, a knee, and a foot. The thigh swings at the hip, the cannon folds at the knee
 * while the leg is lifted, and the foot is one of hoof, paw, bird toes, or the roc's grip.
 */
final class Limbs {
	enum Foot { HOOF, PAW, TALON, GRIP }

	private Limbs() {
	}

	/**
	 * One leg. {@code phase} is where in the stride this leg is. {@code sit} folds every leg under
	 * the lying body; {@code air} tucks every leg back for flight.
	 */
	static void leg(Pen pen, Anchor.Post post, Foot foot, Skin skin, float phase, float sit, float air) {
		MountPose pose = pen.anim;
		boolean bird = foot == Foot.TALON || foot == Foot.GRIP;
		float reach = bird ? 24f : 32f;
		float hipSwing = pose.stride(phase, reach);
		float lift = pose.lift(phase);
		float bendSign = post.front && !bird ? -1f : 1f;
		float bend = bendSign * lift * 48f;
		// Lying down, every leg folds: the upper leg reaches forward and the lower leg tucks back
		// along the ground under the body. Birds fold the same way onto their hocks.
		float fold = bird ? -1f : 1f;
		hipSwing = hipSwing * (1f - sit) + fold * sit * (post.front && !bird ? 68f : 76f);
		bend = bend * (1f - sit) - fold * sit * (post.front && !bird ? 146f : 150f);
		hipSwing -= air * 48f;
		bend += bendSign * air * 40f;
		float thick = post.thick;
		float cx = post.x + thick * 0.5f;
		float cy = post.y + thick * 0.5f;
		float hip = post.hip;
		float knee = bird ? hip * 0.5f : hip * 0.44f;
		float thighThick = bird ? thick + 1f : thick;
		float cannonThick = bird ? Math.max(1.5f, thick - 0.5f) : Math.max(2f, thick - 0.5f);
		float hipAngle = hipSwing;
		float kneeAngle = bend;
		pen.hinge(cx, cy, hip, 0f, 0f, hipAngle, () -> {
			float tx = cx - thighThick * 0.5f;
			float ty = cy - thighThick * 0.5f;
			// The thigh runs up into the body, however short the leg, so a spare leg never floats.
			pen.box(tx, ty, knee - 1f, thighThick, thighThick, post.top - knee + 1f, skin.base());
			pen.hinge(cx, cy, knee, 0f, 0f, kneeAngle, () -> {
				float lx = cx - cannonThick * 0.5f;
				float ly = cy - cannonThick * 0.5f;
				float footHigh = foot == Foot.HOOF ? 2f : foot == Foot.PAW ? 1.5f : 1f;
				pen.box(lx, ly, footHigh - 0.5f, cannonThick, cannonThick, knee - footHigh + 1.5f, skin.dark());
				switch (foot) {
					case HOOF -> pen.box(cx - thick * 0.5f - 0.5f, cy - thick * 0.5f - 0.5f, 0f, thick + 1f, thick + 1f, 2f, Mat.HOOF);
					case PAW -> paw(pen, cx, cy, thick, skin);
					case TALON -> talons(pen, cx, cy, thick, Mat.TALON, 1f, 3f);
					case GRIP -> talons(pen, cx, cy, thick, Mat.BEAK, 1.5f, 4.5f);
				}
			});
		});
	}

	private static void paw(Pen pen, float cx, float cy, float thick, Skin skin) {
		float w = thick + 1f;
		float x = cx - w * 0.5f;
		float y = cy - thick * 0.5f - 1f;
		pen.box(x, y, 0f, w, thick + 1.5f, 1.6f, skin.dark());
		int toes = 3;
		float toeW = w / toes;
		for (int i = 0; i < toes; i++) {
			pen.box(x + i * toeW + 0.1f, y - 0.8f, 0f, toeW - 0.2f, 1.2f, 1.2f, skin.dark());
		}
	}

	/** Three toes forward and one back, each with a darker claw. */
	private static void talons(Pen pen, float cx, float cy, float thick, Mat mat, float width, float length) {
		float[] spread = { -1.6f, 0f, 1.6f };
		for (float s : spread) {
			float x = cx + s * (thick * 0.45f + width * 0.3f) - width * 0.5f;
			pen.box(x, cy - length - thick * 0.3f, 0f, width, length, 1f, mat);
			pen.box(x + width * 0.2f, cy - length - thick * 0.3f - 1f, 0f, width * 0.6f, 1f, 0.8f, Mat.TALON);
		}
		pen.box(cx - width * 0.5f, cy + thick * 0.3f, 0f, width, length * 0.6f, 1f, mat);
		pen.box(cx - thick * 0.5f, cy - thick * 0.5f, 0f, thick, thick, 1.2f, mat);
	}
}
