package tk.darrow.shamanicmounts.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;

import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;
import tk.darrow.shamanicmounts.ride.GiftRules;

/**
 * The rig for one phenotype, in blender-style pixels: X is right, Y runs toward the tail, Z is up,
 * and the feet stand on Z = 0. Negative
 * Minecraft Z is the direction the mount faces. The renderer applies {@code uniformScale}; this
 * mesh is the un-enlarged pixel assembly.
 *
 * <p>The body, neck, head, wings, tail, saddle, and hind legs hang from a shoulder hinge so the
 * whole animal can sit back or pitch into a climb. The front legs stand outside it.
 */
final class MountMesh {
	enum Shell { STEED, HART, ELK, CRANE, ROC, NAGUAL, BARGHEST, SHADE, CHIMERA, BEAR, SERPENT }

	/** Cut plans per phenotype, bare and saddled. The layout does not change with the pose. */
	private static final Map<Phenotype, SolidDraw.Plan[]> PLANS = new WeakHashMap<>();
	/** Time spent building mounts' geometry, for the live harness. */
	static long drawNanos;
	static long draws;
	/** Mounts are drawn one at a time on the render thread, so they share one cube buffer. */
	private static final SolidDraw DRAW = new SolidDraw();
	/** How far a lying mount sinks, in pixels: most of its shortest leg, learned from the last draw. */
	private static final Map<Phenotype, Float> LIE_DROP = new WeakHashMap<>();

	private MountMesh() {
	}

	/** {@code armor} is the horse armor tier: 0 none, 1 leather, 2 iron, 3 gold, 4 diamond. */
	/** {@code armor} is the horse armor tier: 0 none, 1 leather, 2 iron, 3 gold, 4 diamond. {@code pelt} is 0 to 2. */
	static void draw(Phenotype phenotype, boolean saddled, boolean bags, int armor, int pelt, PoseStack pose,
			VertexConsumer consumer, Supplier<VertexConsumer> glow, int light, int overlay, MountPose anim) {
		long start = SolidDraw.stats ? System.nanoTime() : 0L;
		drawWithPlans(phenotype, saddled, bags, armor, pelt, pose, consumer, glow, light, overlay, anim,
				PLANS.computeIfAbsent(phenotype, key -> new SolidDraw.Plan[60]));
		if (SolidDraw.stats) {
			drawNanos += System.nanoTime() - start;
			draws++;
		}
	}

	static void drawWithPlans(Phenotype phenotype, boolean saddled, boolean bags, int armor, int pelt, PoseStack pose,
			VertexConsumer consumer, Supplier<VertexConsumer> glow, int light, int overlay, MountPose anim, SolidDraw.Plan[] plans) {
		Pen pen = new Pen(pose, anim, DRAW);
		Shell shell = shell(phenotype);
		Skin skin = Skin.of(shell, pelt, phenotype);
		int slot = (saddled ? 1 : 0) + (bags ? 2 : 0) + Math.max(0, Math.min(4, armor)) * 4 + Math.floorMod(pelt, 3) * 20;
		float[] shoulder = Torsos.shoulder(shell);
		float flyPitch = Mth.clamp(anim.climb * 60f, -14f, 18f) * anim.air;
		Anchor[] holder = new Anchor[1];
		// Staying, the mount lies down: the body stays level and sinks while every leg folds under it.
		float drop = anim.sit > 0.001f ? LIE_DROP.getOrDefault(phenotype, 0f) * anim.sit : 0f;
		float girth = phenotype.shape.girth();
		pen.shift(0f, 0f, -drop, () -> {
		Runnable body = () -> {
		pen.hinge(0f, shoulder[0], shoulder[1], 0f, 0f, flyPitch, () -> {
			Anchor anchor = Torsos.draw(pen, shell, skin);
			holder[0] = anchor;
			neckAndHead(pen, phenotype, shell, skin, anchor);
			wings(pen, phenotype, shell, skin, anchor);
			tail(pen, phenotype, shell, skin, anchor);
			if (armor > 0) {
				barding(pen, anchor, armor);
			}
			if (saddled) {
				saddle(pen, phenotype, anchor, bags);
			}
			legs(pen, phenotype, shell, skin, anchor, false);
		});
		legs(pen, phenotype, shell, skin, holder[0], true);
		};
		// A cross carries its hidden body's build: the girth widens or narrows the whole animal.
		if (girth != 1f) {
			pen.scaleAt(0f, 0f, 0f, girth, 1f, 1f, body);
		} else {
			body.run();
		}
		});
		if (anim.sit > 0.001f && !LIE_DROP.containsKey(phenotype)) {
			LIE_DROP.put(phenotype, lieDrop(drawnPosts(phenotype, holder[0])));
		}
		plans[slot] = pen.flush(consumer, light, overlay, plans[slot], anim.glow ? glow : null);
	}

	/** The same choice the server makes for the seat, so the rider lands on the saddle that is drawn. */
	static Shell shell(Phenotype phenotype) {
		return Shell.valueOf(MountSize.form(phenotype).name());
	}

	private static void neckAndHead(Pen pen, Phenotype phenotype, Shell shell, Skin skin, Anchor anchor) {
		MountPose anim = pen.anim;
		float nod = Mth.sin(anim.swing * 0.6662f * 2f) * 3f * anim.amount;
		// In flight the neck reaches forward; lying down, it is carried up so the head stays high.
		float lean = anim.air * 18f - anim.sit * 18f;
		pen.hinge(0f, anchor.neckY, anchor.neckZ + 2f, 0f, 0f, nod + lean, () -> {
			float[] socket = neck(pen, phenotype, shell, anchor, skin);
			float hy = socket[0];
			float hz = socket[1] + 3.5f;
			float lookYaw = Mth.clamp(Mth.wrapDegrees(anim.yaw) + anim.idleYaw(), -45f, 45f);
			float lookPitch = Mth.clamp(-anim.pitch, -30f, 24f) - lean * 0.6f;
			pen.hinge(0f, hy, hz, lookYaw, 0f, lookPitch, () -> {
				// A foal's head is large for its body; a cross's head leans toward the hidden body's.
				float head = phenotype.shape.head() * (anim.baby ? 1.3f : 1f);
				if (head != 1f) {
					pen.scaleAt(0f, hy, hz - 3.5f, head, head, head, () -> Heads.head(pen, phenotype, shell, skin, hy, socket[1]));
				} else {
					Heads.head(pen, phenotype, shell, skin, hy, socket[1]);
				}
			});
		});
	}

	private static void legs(Pen pen, Phenotype phenotype, Shell shell, Skin skin, Anchor anchor, boolean front) {
		Limbs.Foot foot = foot(shell, phenotype.foot);
		Anchor.Post[] drawn = drawnPosts(phenotype, anchor);
		int pairs = drawn.length / 2;
		for (int index = 0; index < drawn.length; index++) {
			Anchor.Post leg = drawn[index];
			if (leg.front != front) {
				continue;
			}
			int pair = index / 2;
			float phase;
			if (pairs == 1) {
				phase = 0f;
			} else if (pairs == 2) {
				phase = pair == 0 ? 0f : (float) Math.PI;
			} else if (pairs == 3) {
				phase = pair * (float) (Math.PI * 2.0 / 3.0);
			} else {
				phase = pair * (float) (Math.PI * 0.5);
			}
			if ((index & 1) == 1) {
				phase += (float) Math.PI;
			}
			Limbs.leg(pen, leg, foot, skin, phase, pen.anim.sit, pen.anim.air);
		}
	}

	/** Folded legs reach about a quarter of their length below the hip, so the body sinks the rest. */
	private static float lieDrop(Anchor.Post[] posts) {
		if (posts.length == 0) {
			return 0f;
		}
		float lowest = Float.MAX_VALUE;
		for (Anchor.Post post : posts) {
			lowest = Math.min(lowest, post.hip);
		}
		return lowest * 0.7f;
	}

	private static Anchor.Post[] drawnPosts(Phenotype phenotype, Anchor anchor) {
		Anchor.Post[] born = anchor.posts;
		return switch (phenotype.legs) {
			case NONE -> new Anchor.Post[0];
			case EIGHT -> born.length == 8 ? born : eightEqual(born);
			case FOUR -> four(born);
			case TWO -> two(born);
			case HITCH -> hitch(born);
			case SPARE -> spare(born);
		};
	}

	private static Anchor.Post[] eightEqual(Anchor.Post[] born) {
		float left = born[0].x;
		float right = born[1].x;
		float frontY = born[0].y;
		float back = born[born.length - 1].y;
		float hip = born[0].hip;
		for (Anchor.Post post : born) {
			hip = Math.max(hip, post.hip);
		}
		float thick = Math.min(3f, born[0].thick);
		Anchor.Post[] posts = new Anchor.Post[8];
		for (int i = 0; i < 4; i++) {
			float y = frontY + (back - frontY) * i / 3f;
			posts[i * 2] = Anchor.post(left, y, hip, thick, i < 2);
			posts[i * 2 + 1] = Anchor.post(right, y, hip, thick, i < 2);
		}
		return posts;
	}

	private static Anchor.Post[] four(Anchor.Post[] born) {
		if (born.length == 4) {
			return born;
		}
		if (born.length == 8) {
			return new Anchor.Post[] { born[0], born[1], born[6].front(false), born[7].front(false) };
		}
		Anchor.Post rearL = born[0].at(born[0].y + 8f, born[0].hip).front(false);
		Anchor.Post rearR = born[1].at(born[1].y + 8f, born[1].hip).front(false);
		return new Anchor.Post[] { born[0], born[1], rearL, rearR };
	}

	private static Anchor.Post[] two(Anchor.Post[] born) {
		if (born.length == 2) {
			return born;
		}
		float y = (born[0].y + born[born.length - 1].y) * 0.5f;
		float hip = Math.max(born[0].hip, born[born.length - 1].hip);
		return new Anchor.Post[] { born[0].at(y, hip).front(true), born[1].at(y, hip).front(true) };
	}

	private static Anchor.Post[] hitch(Anchor.Post[] born) {
		Anchor.Post[] full = born.length >= 4 ? new Anchor.Post[] { born[0], born[1] } : born;
		float shortHip = Math.max(6f, full[0].hip * 3f / 5f);
		Anchor.Post[] extra = born.length >= 4
				? new Anchor.Post[] {
						born[born.length - 2].at(born[born.length - 2].y, shortHip).front(false),
						born[born.length - 1].at(born[born.length - 1].y, shortHip).front(false)
				}
				: new Anchor.Post[] {
						born[0].at(born[0].y + 6f, shortHip).front(false),
						born[1].at(born[1].y + 6f, shortHip).front(false)
				};
		return new Anchor.Post[] { full[0], full[1], extra[0], extra[1] };
	}

	private static Anchor.Post[] spare(Anchor.Post[] born) {
		Anchor.Post[] full = four(born);
		float mid = (full[0].y + full[2].y) * 0.5f;
		float shortHip = Math.max(6f, full[0].hip * 3f / 5f);
		return new Anchor.Post[] {
				full[0], full[1], full[2], full[3],
				full[0].at(mid, shortHip).front(false), full[1].at(mid, shortHip).front(false)
		};
	}

	private static void wings(Pen pen, Phenotype phenotype, Shell shell, Skin skin, Anchor anchor) {
		if (shell == Shell.CHIMERA) {
			Wings.membrane(pen, anchor, 22f * phenotype.wingScale, 7f * (0.6f + 0.4f * phenotype.wingScale), skin.rosette(), Mat.MEMBRANE,
					4 + (phenotype.wingScale >= 2.0f ? 1 : 0));
			return;
		}
		Mat mat = shell == Shell.CRANE ? Mat.CRANE_WING : shell == Shell.ROC ? Mat.ROC : Mat.CRANE_WING;
		Mat dark = shell == Shell.CRANE ? Mat.CRANE_WING_DARK : shell == Shell.ROC ? Mat.ROC_DARK : Mat.CRANE_WING_DARK;
		float span = phenotype.wingScale;
		float chord = 0.6f + 0.4f * span;
		int extra = span >= 2.0f ? 2 : span >= 1.4f ? 1 : span < 0.8f ? -1 : 0;
		switch (phenotype.wings) {
			case NONE -> {
			}
			case PINION -> Wings.feathered(pen, anchor, 11f * span, 5f * chord, mat, dark, Mat.PRIMARY, 3 + extra);
			case FULL -> Wings.feathered(pen, anchor, 20f * span, 7f * chord, mat, dark, Mat.PRIMARY, 5 + extra);
			case ASTRAL -> Wings.feathered(pen, anchor, 26f * span, 8f * chord, Mat.ROC, Mat.ROC_DARK, Mat.ASTRAL, 6 + extra);
			case ASTRAL_FULL -> {
				Wings.feathered(pen, anchor, 20f * span, 7f * chord, mat, dark, Mat.PRIMARY, 5 + extra);
				Wings.feathered(pen, anchor, 26f * span, 8f * chord, Mat.ROC, Mat.ROC_DARK, Mat.ASTRAL, 6 + extra);
			}
		}
	}

	/** The neck, lengthened or shortened by a cross's hidden body. Returns where the head sits. */
	private static float[] neck(Pen pen, Phenotype phenotype, Shell shell, Anchor anchor, Skin skin) {
		float length = phenotype.shape.neck();
		if (length == 1f || shell == Shell.CHIMERA) {
			return Heads.neck(pen, shell, anchor, skin);
		}
		float baseY = anchor.neckY;
		float baseZ = anchor.neckZ + 2f;
		float[][] socket = new float[1][];
		pen.scaleAt(0f, baseY, baseZ, 1f, length, length, () -> socket[0] = Heads.neck(pen, shell, anchor, skin));
		return new float[] { baseY + (socket[0][0] - baseY) * length, baseZ + (socket[0][1] - baseZ) * length };
	}

	private static void tail(Pen pen, Phenotype phenotype, Shell shell, Skin skin, Anchor anchor) {
		float length = phenotype.shape.tail();
		if (length != 1f && shell != Shell.CHIMERA && phenotype.tail != Phenotype.TailShow.NONE) {
			pen.scaleAt(0f, anchor.tailY, anchor.tailZ, 1f, length, length, () -> drawTail(pen, phenotype, shell, skin, anchor));
		} else {
			drawTail(pen, phenotype, shell, skin, anchor);
		}
	}

	private static void drawTail(Pen pen, Phenotype phenotype, Shell shell, Skin skin, Anchor anchor) {
		if (shell == Shell.CHIMERA) {
			Tails.spade(pen, anchor.tailY, anchor.tailZ, skin);
			return;
		}
		switch (phenotype.tail) {
			case NONE -> {
			}
			case STUB -> pen.box(-1f, anchor.tailY, anchor.tailZ - 1f, 2f, 2.5f, 2f, skin.dark());
			case PLUME -> Tails.plume(pen, anchor.tailY, anchor.tailZ, skin);
			case FLAG -> Tails.flag(pen, anchor.tailY, anchor.tailZ, skin);
			case LASH -> {
				if (shell == Shell.SERPENT) {
					Tails.lash(pen, anchor.tailY, anchor.tailZ, skin, 9, 6f, false);
					break;
				}
				boolean cat = shell == Shell.NAGUAL || shell == Shell.SHADE || phenotype.head == Marks.Head.CAT;
				float thick = shell == Shell.NAGUAL ? 3.5f : shell == Shell.BARGHEST ? 2.6f : 2.4f;
				Tails.lash(pen, anchor.tailY, anchor.tailZ, skin, cat ? 7 : 5, thick, cat);
			}
			case FAN -> {
				if (shell == Shell.ROC) {
					Tails.fan(pen, anchor.tailY, anchor.tailZ, skin, 10f, Mat.ASTRAL_DARK);
				} else {
					Tails.fan(pen, anchor.tailY, anchor.tailZ, skin, 6f, Mat.PRIMARY);
				}
			}
		}
	}

	/**
	 * The tack. A seat sits proud of the back line with a pommel in front and a cantle behind, a girth
	 * runs under the barrel, stirrups hang on leathers down both flanks, and when bags are strapped
	 * on they ride behind the seat. A mount with the elk's bearing carries the big double bags. The
	 * drum hangs on the flank.
	 */
	private static void saddle(Pen pen, Phenotype phenotype, Anchor a, boolean bags) {
		float y = a.saddleY;
		float z = a.saddleZ;
		float half = a.half;
		float seatHalf = Math.min(half, 4f);
		pen.box(-seatHalf, y, z + 0.2f, seatHalf * 2f, 7f, 2.2f, Mat.SADDLE);
		pen.box(-seatHalf + 0.6f, y - 0.2f, z + 2.4f, seatHalf * 2f - 1.2f, 1.8f, 2.6f, Mat.SADDLE);
		pen.box(-seatHalf + 0.8f, y + 5.4f, z + 2.4f, seatHalf * 2f - 1.6f, 1.8f, 3f, Mat.SADDLE);
		pen.box(-1.5f, y + 1.8f, z + 2.4f, 3f, 3.4f, 0.6f, Mat.GOLD);
		pen.pair(-half - 1.2f, y + 0.4f, z - 4.5f, 1.4f, 6.2f, 5f, Mat.SADDLE);
		pen.pair(-half - 0.8f, y + 2.5f, z - 9f, 1f, 2f, 4.6f, Mat.SADDLE_DARK);
		pen.box(-half - 0.6f, y + 2.5f, z - 9.6f, half * 2f + 1.2f, 2f, 1.2f, Mat.SADDLE_DARK);
		pen.pair(-half - 1.1f, y + 3.5f, z - 13f, 0.8f, 1.5f, 8.6f, Mat.SADDLE_DARK);
		pen.pair(-half - 2.2f, y + 3f, z - 15f, 1.8f, 2.6f, 2.4f, Mat.GOLD);
		pen.pair(-half - 1.7f, y + 3.5f, z - 14.4f, 0.8f, 1.6f, 1.2f, Mat.SADDLE_DARK);
		if (bags) {
			boolean big = GiftRules.chest(phenotype);
			float bagLength = big ? 6f : 4.5f;
			float bagDrop = big ? 8f : 6.5f;
			bag(pen, half, y + 7.6f, z, bagLength, bagDrop);
			if (big) {
				bag(pen, half, y + 7.6f + bagLength + 0.8f, z, bagLength, bagDrop);
			}
		}
		if (phenotype.bond == Phenotype.BondShow.DRUM) {
			pen.box(-half - 3.4f, y + 1.5f, z - 5.5f, 2.4f, 4f, 4f, Mat.SADDLE);
			pen.box(-half - 3.6f, y + 1.3f, z - 5.7f, 2.8f, 0.6f, 4.4f, Mat.GOLD);
			pen.box(-half - 3.6f, y + 5.1f, z - 5.7f, 2.8f, 0.6f, 4.4f, Mat.GOLD);
		}
	}

	/**
	 * Horse armor, worn the way vanilla wears it: a caparison over the whole barrel under the saddle,
	 * skirts down both flanks, a chest plate, and a crupper over the rump, in the armor's own colour
	 * with a pale rivet line along the edges.
	 */
	private static void barding(Pen pen, Anchor a, int tier) {
		Mat plate = switch (tier) {
			case 1 -> Mat.ARMOR_LEATHER;
			case 3 -> Mat.ARMOR_GOLD;
			case 4 -> Mat.ARMOR_DIAMOND;
			default -> Mat.ARMOR;
		};
		Mat edge = tier == 3 ? Mat.ARMOR : Mat.ARMOR_TRIM;
		float half = a.half;
		float y = a.saddleY;
		float z = a.saddleZ;
		float chestY = a.chestY;
		float chestZ = a.chestZ;
		float back = a.tailY - 1f;
		// Caparison over the back, from the neck root to the rump, under the seat.
		pen.box(-half - 0.4f, a.neckY - 1f, z - 0.2f, half * 2f + 0.8f, back - a.neckY + 1f, 1.2f, plate);
		// Skirts down both flanks to the belly line.
		pen.pair(-half - 1.2f, a.neckY - 1f, chestZ - 6.5f, 1.2f, back - a.neckY + 1f, z - chestZ + 6.5f, plate);
		pen.pair(-half - 1.4f, a.neckY - 1f, chestZ - 6.5f, 1.4f, back - a.neckY + 1f, 0.8f, edge);
		// Chest plate down the front of the chest.
		pen.box(-half + 0.3f, a.neckY - 3.2f, chestZ - 6.5f, half * 2f - 0.6f, 2.4f, z - chestZ + 6.5f, plate);
		pen.box(-half + 0.8f, a.neckY - 3.5f, chestZ - 6.9f, half * 2f - 1.6f, 1f, 0.8f, edge);
		// Crupper over the rump, with its edge.
		pen.box(-half + 0.2f, back, z - 4f, half * 2f - 0.4f, 1.4f, 5f, plate);
		pen.box(-half + 0.6f, back + 0.3f, z + 1f, half * 2f - 1.2f, 0.8f, 0.8f, edge);
	}

	/** One pair of bags hanging on both flanks: the pouch, its darker flap, and a gold buckle. */
	private static void bag(Pen pen, float half, float y, float z, float length, float drop) {
		pen.pair(-half - 2.4f, y, z - drop, 2.4f, length, drop + 0.6f, Mat.BAG);
		pen.pair(-half - 2.7f, y - 0.3f, z - drop * 0.45f, 2.9f, length + 0.6f, drop * 0.45f + 1.4f, Mat.BAG_DARK);
		pen.pair(-half - 2.9f, y + length * 0.5f - 0.6f, z - drop * 0.45f - 0.4f, 0.5f, 1.2f, 1.4f, Mat.GOLD);
	}

	private static Limbs.Foot foot(Shell shell, Phenotype.FootShow show) {
		if (shell == Shell.ROC && show == Phenotype.FootShow.TALON) {
			return Limbs.Foot.GRIP;
		}
		return switch (show) {
			case HOOF -> Limbs.Foot.HOOF;
			case PAW -> Limbs.Foot.PAW;
			case TALON -> Limbs.Foot.TALON;
		};
	}
}
