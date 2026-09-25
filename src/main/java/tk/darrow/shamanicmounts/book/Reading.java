package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Locus;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;

/**
 * One mount's genes as the book reads them: what shows, both copies in plain words, and why a
 * hidden copy still matters. Lines come in groups, in the order the tame page prints them.
 */
public final class Reading {
	public enum Note {
		/** One copy, or two alike: nothing hidden. */
		PLAIN,
		/** The other copy is hidden here and can show in a foal. */
		CARRIED,
		/** The copies average. */
		BLENDED,
		/** A mixed pair shows a smaller or partial piece. */
		INCOMPLETE,
		/** Both copies show at once. */
		CODOMINANT,
		/** Another gene hides this one. */
		MASKED,
		/** Two crowns: the heavy rack. */
		HEAVY,
		/** The nagual's bond thins the body to a ghost. */
		THIN,
		/** The chimera form covers the body genes. */
		CHIMERA,
		/** The showing body wins, and the hidden body pulls its shape. */
		SHAPED
	}

	public enum Group {
		BODY("Body"), SIZE("Size"), COAT("Coat"), WINGS("Wings"), NATURE("Nature"), GIFTS("Ridden gifts");

		public final String title;

		Group(String title) {
			this.title = title;
		}
	}

	/**
	 * One gene. {@code shown}, {@code maternal}, and {@code paternal} are allele codes the notation
	 * compares; {@code word}, {@code damWord}, and {@code sireWord} are what the reader sees.
	 */
	public record Line(Group group, Locus locus, String shown, String maternal, String paternal, Note note, String word,
			String damWord, String sireWord) {
	}

	private Reading() {
	}

	public static List<Line> of(Genome genome) {
		Phenotype phenotype = Expression.express(genome);
		MountSize.Form form = MountSize.form(phenotype);
		ArrayList<Line> lines = new ArrayList<>();

		// Body.
		Note torsoNote = genome.chimera ? Note.CHIMERA
				: phenotype.torsoMaternal != phenotype.torsoPaternal ? Note.SHAPED : Note.PLAIN;
		lines.add(line(Group.BODY, genome, Locus.TORSO, genome.chimera ? "chimera" : phenotype.torso.code(), torsoNote,
				genome.chimera ? "chimera" : phenotype.torso.code()));
		lines.add(line(Group.BODY, genome, Locus.HEAD, genome.chimera ? "chimera" : phenotype.head.code(),
				genome.chimera ? Note.CHIMERA : (phenotype.head != phenotype.carriedHead ? Note.CARRIED : Note.PLAIN),
				genome.chimera ? "chimera" : phenotype.head.code()));
		boolean legsDiffer = genome.maternal(Locus.LEGS) != genome.paternal(Locus.LEGS);
		boolean legsBlend = phenotype.legs == Phenotype.LegShow.SPARE || phenotype.legs == Phenotype.LegShow.HITCH;
		String legs = lower(phenotype.legs.name());
		Note legNote = legsBlend && legsDiffer ? Note.INCOMPLETE : legsDiffer ? Note.CARRIED : Note.PLAIN;
		lines.add(line(Group.BODY, genome, Locus.LEGS, legs, legNote, legs));
		String foot = lower(phenotype.foot.name());
		lines.add(line(Group.BODY, genome, Locus.FOOT, foot,
				genome.maternal(Locus.FOOT) != genome.paternal(Locus.FOOT) ? Note.CARRIED : Note.PLAIN, foot));
		Note tailNote = Note.PLAIN;
		if (phenotype.tail == Phenotype.TailShow.STUB) {
			tailNote = Note.INCOMPLETE;
		} else if (genome.maternal(Locus.TAIL) != genome.paternal(Locus.TAIL)) {
			tailNote = Note.CARRIED;
		}
		String tail = lower(phenotype.tail.name());
		lines.add(line(Group.BODY, genome, Locus.TAIL, tail, tailNote, tail));
		String rack = lower(phenotype.rack.name());
		Note rackNote = phenotype.crownHeavy ? Note.HEAVY
				: genome.maternal(Locus.RACK) != genome.paternal(Locus.RACK) ? Note.INCOMPLETE : Note.PLAIN;
		lines.add(line(Group.BODY, genome, Locus.RACK, rack, rackNote, rack));

		// Size: the line's build, then size within it.
		String build = lower(phenotype.scale.name());
		lines.add(line(Group.SIZE, genome, Locus.SCALE, build,
				genome.maternal(Locus.SCALE) != genome.paternal(Locus.SCALE) ? Note.INCOMPLETE : Note.PLAIN, build));
		lines.add(new Line(Group.SIZE, Locus.SIZE, phenotype.size.code(), genome.maternal(Locus.SIZE).code(),
				genome.paternal(Locus.SIZE).code(),
				genome.maternal(Locus.SIZE) != genome.paternal(Locus.SIZE) ? Note.BLENDED : Note.PLAIN,
				String.format(Locale.ROOT, "%s (x%.2f)", phenotype.size.name(), phenotype.sizeFactor),
				genome.maternal(Locus.SIZE).code().toUpperCase(Locale.ROOT),
				genome.paternal(Locus.SIZE).code().toUpperCase(Locale.ROOT)));

		// Coat: the pelt, the pattern, and the phase.
		Marks.Pelt peltM = (Marks.Pelt) genome.maternal(Locus.PELT);
		Marks.Pelt peltP = (Marks.Pelt) genome.paternal(Locus.PELT);
		String peltShown = Marks.Pelt.values()[phenotype.pelt].code();
		lines.add(new Line(Group.COAT, Locus.PELT, peltShown, peltM.code(), peltP.code(),
				peltM != peltP ? Note.CARRIED : Note.PLAIN, lower(TamePage.peltName(form, phenotype.pelt)),
				lower(TamePage.peltName(form, peltM.ordinal())), lower(TamePage.peltName(form, peltP.ordinal()))));
		String coat = lower(phenotype.coat.name()).replace('_', ' ');
		lines.add(line(Group.COAT, genome, Locus.COAT, coat,
				phenotype.coatMasked ? Note.MASKED
						: coatMixed(phenotype.coat) ? Note.CODOMINANT
						: genome.maternal(Locus.COAT) != genome.paternal(Locus.COAT) ? Note.CARRIED : Note.PLAIN, coat));
		String phase = lower(phenotype.phase.name()).replace('_', ' ');
		lines.add(line(Group.COAT, genome, Locus.PHASE, phase,
				phenotype.phase == Phenotype.PhaseShow.DEEP_VEIL ? Note.CODOMINANT
						: genome.maternal(Locus.PHASE) != genome.paternal(Locus.PHASE) ? Note.CARRIED : Note.PLAIN, phase));

		// Wings.
		Note wingNote = Note.PLAIN;
		if (phenotype.wings == Phenotype.WingShow.ASTRAL_FULL) {
			wingNote = Note.CODOMINANT;
		} else if (phenotype.wings == Phenotype.WingShow.PINION
				&& genome.maternal(Locus.WINGS) != genome.paternal(Locus.WINGS)) {
			wingNote = Note.INCOMPLETE;
		} else if (genome.maternal(Locus.WINGS) != genome.paternal(Locus.WINGS)) {
			wingNote = Note.CARRIED;
		}
		String wings = lower(phenotype.wings.name()).replace('_', '+');
		lines.add(line(Group.WINGS, genome, Locus.WINGS, wings, wingNote, wings));
		lines.add(line(Group.WINGS, genome, Locus.WINGSPAN, String.format(Locale.ROOT, "x%.1f", phenotype.wingScale),
				genome.maternal(Locus.WINGSPAN) != genome.paternal(Locus.WINGSPAN) ? Note.BLENDED : Note.PLAIN,
				String.format(Locale.ROOT, "x%.1f", phenotype.wingScale)));

		// Nature: how it moves, where it goes, what it senses, and its bond.
		String gait = lower(phenotype.gait.name());
		lines.add(line(Group.NATURE, genome, Locus.GAIT, gait,
				phenotype.gait == Phenotype.GaitShow.MIST ? Note.CODOMINANT
						: genome.maternal(Locus.GAIT) != genome.paternal(Locus.GAIT) ? Note.CARRIED : Note.PLAIN, gait));
		String realms = phenotype.realms.isEmpty() ? "hearth" : words(phenotype.realms);
		lines.add(line(Group.NATURE, genome, Locus.REALM, realms,
				phenotype.realms.size() > 1 ? Note.CODOMINANT : Note.PLAIN, realms));
		String sense = lower(phenotype.sense.name());
		lines.add(line(Group.NATURE, genome, Locus.SENSE, sense,
				genome.maternal(Locus.SENSE) != genome.paternal(Locus.SENSE) ? Note.CARRIED : Note.PLAIN, sense));
		String bond = lower(phenotype.bond.name());
		lines.add(line(Group.NATURE, genome, Locus.BOND, bond,
				phenotype.thin ? Note.THIN : genome.maternal(Locus.BOND) != genome.paternal(Locus.BOND) ? Note.CARRIED : Note.PLAIN,
				bond));
		String wards = phenotype.wards.isEmpty() ? "none" : words(phenotype.wards);
		lines.add(line(Group.NATURE, genome, Locus.WARD, wards,
				phenotype.wards.size() > 1 ? Note.CODOMINANT : Note.PLAIN, wards));
		String trails = phenotype.trails.isEmpty() ? "none" : words(phenotype.trails);
		lines.add(line(Group.NATURE, genome, Locus.TRAIL, trails,
				phenotype.trails.size() > 1 ? Note.CODOMINANT : Note.PLAIN, trails));

		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift != Marks.Gift.NONE) {
				lines.add(giftLine(genome, gift));
			}
		}
		return lines;
	}

	private static boolean coatMixed(Phenotype.CoatShow coat) {
		return switch (coat) {
			case SPECKLED, DUSK_WASH, DUSK_ROSETTE, DUSK_STAR -> true;
			default -> false;
		};
	}

	private static String lower(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	/** The members of a set as lower-case words joined with a plus: {@code upper+lower}. */
	private static String words(java.util.Set<? extends Enum<?>> set) {
		StringBuilder out = new StringBuilder();
		for (Enum<?> member : set) {
			if (out.length() > 0) {
				out.append('+');
			}
			out.append(member.name().toLowerCase(Locale.ROOT));
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
		String damWord = maternal ? "on" : "-";
		String sireWord = paternal ? "on" : "-";
		String word = "none".equals(shown) ? "none" : note == Note.INCOMPLETE ? "night only" : "on";
		return new Line(Group.GIFTS, gift.locus(), shown, genome.maternal(gift.locus()).code(),
				genome.paternal(gift.locus()).code(), note, word, damWord, sireWord);
	}

	private static Line line(Group group, Genome genome, Locus locus, String shown, Note note, String word) {
		String dam = genome.maternal(locus).code();
		String sire = genome.paternal(locus).code();
		return new Line(group, locus, shown, dam, sire, note, word, dam, sire);
	}
}
