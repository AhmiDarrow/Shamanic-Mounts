package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** One tame, as the book prints it: a summary, its family, and every gene with both copies. */
public final class TamePage {
	/**
	 * One gene row: its group, symbol, genealogy notation, what shows, each parent's copy in words,
	 * the note on how they combine, and the rule for how the gene passes down.
	 */
	public record Row(Reading.Group group, String locus, String symbol, String notation, String shown, String dam,
			String sire, String note, String rule) {
	}

	/** One relative: how it is related, its name, and its id so the book can open it. */
	public record Kin(String relation, String name, UUID id) {
	}

	/** The three coats of each body, in pelt order: A, B, C. */
	private static final Map<MountSize.Form, List<String>> PELTS = Map.ofEntries(
			Map.entry(MountSize.Form.STEED, List.of("Bay", "Black", "Palomino")),
			Map.entry(MountSize.Form.HART, List.of("Tan", "Red", "White")),
			Map.entry(MountSize.Form.ELK, List.of("Dark", "Brown", "Grey")),
			Map.entry(MountSize.Form.CRANE, List.of("White", "Grey", "Black")),
			Map.entry(MountSize.Form.NAGUAL, List.of("Gold", "Panther", "Snow")),
			Map.entry(MountSize.Form.BARGHEST, List.of("Black", "Grey", "Red")),
			Map.entry(MountSize.Form.ROC, List.of("Bone", "Storm", "Red")),
			Map.entry(MountSize.Form.SHADE, List.of("Dusk", "Silver", "Ink")),
			Map.entry(MountSize.Form.CHIMERA, List.of("Amber", "Jade", "Night")),
			Map.entry(MountSize.Form.BEAR, List.of("Black", "Grizzly", "Polar")),
			Map.entry(MountSize.Form.SERPENT, List.of("River", "Jungle", "Bone")));

	private TamePage() {
	}

	public static List<Row> genes(Genome genome, boolean spoilers) {
		ArrayList<Row> rows = new ArrayList<>();
		for (Reading.Line line : Reading.of(genome)) {
			String note = line.note() == Reading.Note.PLAIN ? "" : noteWord(line.note());
			rows.add(new Row(line.group(), Genotype.name(line.locus()), Genotype.symbol(line.locus()),
					Genotype.notation(line), line.word(), line.damWord(), line.sireWord(), spoilers ? note : "",
					Genotype.rule(line.locus())));
		}
		return rows;
	}

	/** The note as the reader sees it. */
	public static String noteWord(Reading.Note note) {
		return switch (note) {
			case PLAIN -> "";
			case CARRIED -> "carried";
			case BLENDED -> "averaged";
			case INCOMPLETE -> "partial";
			case CODOMINANT -> "both show";
			case MASKED -> "masked";
			case HEAVY -> "heavy";
			case THIN -> "thinned";
			case CHIMERA -> "chimera";
			case SHAPED -> "shape blends";
		};
	}

	/** The name of one pelt of the body a phenotype shows. */
	public static String peltName(MountSize.Form form, int pelt) {
		List<String> names = PELTS.get(form);
		return names.get(Math.floorMod(pelt, names.size()));
	}

	/** The three pelt names of a body, A to C. */
	public static List<String> pelts(MountSize.Form form) {
		return PELTS.get(form);
	}

	/** The name of the coat this mount wears. */
	public static String pelt(Genome genome) {
		Phenotype phenotype = Expression.express(genome);
		return peltName(MountSize.form(phenotype), phenotype.pelt);
	}

	/** The body a mount shows, as a word: the founder line whose body it is. */
	public static String body(Genome genome) {
		Phenotype phenotype = Expression.express(genome);
		String form = MountSize.form(phenotype).name().toLowerCase(Locale.ROOT);
		return switch (form) {
			case "steed" -> "Eightfold";
			case "hart" -> "Drum hart";
			default -> Character.toUpperCase(form.charAt(0)) + form.substring(1);
		};
	}

	/** One line for a tame: the body it shows, its pelt, its size, and its sex. */
	public static String summary(Genome genome, boolean male) {
		Phenotype phenotype = Expression.express(genome);
		String carried = phenotype.torso != phenotype.carriedTorso && !genome.chimera
				? " (carries " + phenotype.carriedTorso.code() + ")" : "";
		return body(genome) + " body" + carried + ", " + pelt(genome) + ", size " + phenotype.size.name() + ", "
				+ (male ? "male" : "female");
	}

	/** The wing line, or an empty string for a mount without wings. */
	public static String wings(Genome genome) {
		Phenotype phenotype = Expression.express(genome);
		if (phenotype.wings == Phenotype.WingShow.NONE && !phenotype.chimera) {
			return "";
		}
		return String.format(Locale.ROOT, "Wings x%.1f", phenotype.wingScale);
	}

	/** Dam, sire, and every foal, each with its id so the book can open it. A missing parent has no id. */
	public static List<Kin> kin(HerdBook book, HerdBook.Entry entry) {
		ArrayList<Kin> kin = new ArrayList<>();
		HerdBook.Entry dam = entry.dam() == null ? null : book.get(entry.dam());
		HerdBook.Entry sire = entry.sire() == null ? null : book.get(entry.sire());
		kin.add(new Kin("Dam", dam == null ? "unknown" : dam.name(), dam == null ? null : dam.id()));
		kin.add(new Kin("Sire", sire == null ? "unknown" : sire.name(), sire == null ? null : sire.id()));
		for (HerdBook.Entry foal : book.foals(entry.id())) {
			kin.add(new Kin("Foal", foal.name(), foal.id()));
		}
		return kin;
	}

	/** The family as plain lines: dam, sire, then foals on one line. */
	public static List<String> family(HerdBook book, HerdBook.Entry entry) {
		ArrayList<String> lines = new ArrayList<>();
		StringBuilder foals = new StringBuilder();
		for (Kin kin : kin(book, entry)) {
			if ("Foal".equals(kin.relation())) {
				foals.append(foals.length() == 0 ? "Foals: " : ", ").append(kin.name());
			} else {
				lines.add(kin.relation() + ": " + kin.name());
			}
		}
		lines.add(foals.length() == 0 ? "Foals: none" : foals.toString());
		return lines;
	}
}
