package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Locus;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** One book page: the piece that is showing, both alleles, and why a hidden one still matters. */
public final class Reading {
	public enum Note {
		PLAIN, CARRIED, BLENDED, INCOMPLETE, CODOMINANT, MASKED, HEAVY, THIN, CHIMERA
	}

	public record Line(Locus locus, String shown, String maternal, String paternal, Note note) {
	}

	private Reading() {
	}

	public static List<Line> of(Genome genome) {
		Phenotype phenotype = Expression.express(genome);
		ArrayList<Line> lines = new ArrayList<>();
		boolean torsoDiffers = phenotype.torsoMaternal != phenotype.torsoPaternal;
		lines.add(line(genome, Locus.TORSO,
				genome.chimera ? "chimera" : (torsoDiffers ? "blended" : phenotype.torsoMaternal.code()),
				genome.chimera ? Note.CHIMERA : (torsoDiffers ? Note.BLENDED : Note.PLAIN)));
		lines.add(line(genome, Locus.HEAD, genome.chimera ? "chimera" : phenotype.head.code(),
				genome.chimera ? Note.CHIMERA
						: (phenotype.head != phenotype.carriedHead ? Note.CARRIED : Note.PLAIN)));
		boolean legsDiffer = genome.maternal(Locus.LEGS) != genome.paternal(Locus.LEGS);
		boolean legsBlend = phenotype.legs == Phenotype.LegShow.SPARE || phenotype.legs == Phenotype.LegShow.HITCH;
		lines.add(line(genome, Locus.LEGS, phenotype.legs.name().toLowerCase(),
				legsBlend && legsDiffer ? Note.INCOMPLETE : Note.PLAIN));
		lines.add(line(genome, Locus.FOOT, phenotype.foot.name().toLowerCase(),
				genome.maternal(Locus.FOOT) != genome.paternal(Locus.FOOT) ? Note.CARRIED : Note.PLAIN));
		lines.add(line(genome, Locus.RACK, phenotype.rack.name().toLowerCase(),
				phenotype.crownHeavy ? Note.HEAVY : Note.PLAIN));
		Note wingNote = Note.PLAIN;
		if (phenotype.wings == Phenotype.WingShow.ASTRAL_FULL) {
			wingNote = Note.CODOMINANT;
		} else if (phenotype.wings == Phenotype.WingShow.PINION
				&& genome.maternal(Locus.WINGS) != genome.paternal(Locus.WINGS)) {
			wingNote = Note.INCOMPLETE;
		}
		lines.add(line(genome, Locus.WINGS, phenotype.wings.name().toLowerCase(), wingNote));
		lines.add(line(genome, Locus.WINGSPAN, String.format(java.util.Locale.ROOT, "x%.1f", phenotype.wingScale),
				genome.maternal(Locus.WINGSPAN) != genome.paternal(Locus.WINGSPAN) ? Note.BLENDED : Note.PLAIN));
		Note tailNote = Note.PLAIN;
		if (phenotype.tail == Phenotype.TailShow.STUB) {
			tailNote = Note.INCOMPLETE;
		} else if (genome.maternal(Locus.TAIL) != genome.paternal(Locus.TAIL)) {
			tailNote = Note.CARRIED;
		}
		lines.add(line(genome, Locus.TAIL, phenotype.tail.name().toLowerCase(), tailNote));
		lines.add(line(genome, Locus.GAIT, phenotype.gait.name().toLowerCase(),
				phenotype.gait == Phenotype.GaitShow.MIST ? Note.CODOMINANT : Note.PLAIN));
		lines.add(line(genome, Locus.COAT, phenotype.coat.name().toLowerCase(),
				phenotype.coatMasked ? Note.MASKED : Note.PLAIN));
		lines.add(line(genome, Locus.SCALE, phenotype.scale.name().toLowerCase(), Note.PLAIN));
		lines.add(line(genome, Locus.REALM, phenotype.realms.isEmpty() ? "hearth" : words(phenotype.realms),
				phenotype.realms.size() > 1 ? Note.CODOMINANT : Note.PLAIN));
		lines.add(line(genome, Locus.PHASE, phenotype.phase.name().toLowerCase(), Note.PLAIN));
		lines.add(line(genome, Locus.SENSE, phenotype.sense.name().toLowerCase(), Note.PLAIN));
		lines.add(line(genome, Locus.BOND, phenotype.bond.name().toLowerCase(),
				phenotype.thin ? Note.THIN : Note.PLAIN));
		lines.add(line(genome, Locus.WARD, phenotype.wards.isEmpty() ? "none" : words(phenotype.wards),
				phenotype.wards.size() > 1 ? Note.CODOMINANT : Note.PLAIN));
		lines.add(line(genome, Locus.TRAIL, phenotype.trails.isEmpty() ? "none" : words(phenotype.trails),
				phenotype.trails.size() > 1 ? Note.CODOMINANT : Note.PLAIN));
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift != Marks.Gift.NONE) {
				lines.add(giftLine(genome, gift));
			}
		}
		return lines;
	}

	/** The members of a set as lower-case words joined with a plus: {@code upper+lower}. */
	private static String words(java.util.Set<? extends Enum<?>> set) {
		StringBuilder out = new StringBuilder();
		for (Enum<?> member : set) {
			if (out.length() > 0) {
				out.append('+');
			}
			out.append(member.name().toLowerCase(java.util.Locale.ROOT));
		}
		return out.toString();
	}

	private static Line giftLine(Genome genome, Marks.Gift gift) {
		boolean maternal = genome.maternal(gift.locus()) == gift;
		boolean paternal = genome.paternal(gift.locus()) == gift;
		String shown = "none";
		Note note = Note.PLAIN;
		if (maternal && paternal) {
			shown = gift.code();
		} else if (maternal || paternal) {
			if (gift.recessive()) {
				note = Note.CARRIED;
			} else if (gift == Marks.Gift.DREAM) {
				shown = gift.code();
				note = Note.INCOMPLETE;
			} else {
				shown = gift.code();
			}
		}
		return new Line(gift.locus(), shown, genome.maternal(gift.locus()).code(), genome.paternal(gift.locus()).code(),
				note);
	}

	private static Line line(Genome genome, Locus locus, String shown, Note note) {
		return new Line(locus, shown, genome.maternal(locus).code(), genome.paternal(locus).code(), note);
	}
}
