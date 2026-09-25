package tk.darrow.shamanicmounts.client;

/** Where a torso hands off to its neck, wings, tail, saddle, and legs. Blender pixels, feet on Z = 0. */
final class Anchor {
	/** Base of the neck: the y of the chest front and the z of the neck's underside there. */
	float neckY;
	float neckZ;
	/** Half the body width at the wing root. */
	float half;
	float wingY;
	float wingZ;
	float tailY;
	float tailZ;
	float saddleY;
	float saddleZ;
	/** The shoulder, which the body pitches around when it sits or flies. */
	float shoulderY;
	float shoulderZ;
	/** Where breathing swells from. */
	float chestY;
	float chestZ;
	Post[] posts = new Post[0];

	/**
	 * One leg root: its left-front corner, the z of the hip joint, how thick the leg is, and the z
	 * where the thigh disappears into the body.
	 */
	static final class Post {
		final float x;
		final float y;
		final float hip;
		final float thick;
		final boolean front;
		final float top;

		Post(float x, float y, float hip, float thick, boolean front, float top) {
			this.x = x;
			this.y = y;
			this.hip = hip;
			this.thick = thick;
			this.front = front;
			this.top = top;
		}

		/** A shorter or moved copy that still reaches the same body. */
		Post at(float newY, float newHip) {
			return new Post(x, newY, newHip, thick, front, top);
		}

		Post front(boolean isFront) {
			return new Post(x, y, hip, thick, isFront, top);
		}
	}

	static Post post(float x, float y, float hip, float thick, boolean front) {
		return new Post(x, y, hip, thick, front, hip + 2f);
	}
}
