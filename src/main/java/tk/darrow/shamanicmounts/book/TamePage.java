package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** One tame, as the book prints it. Gene notes stay hidden while spoilers are off. */
public final class TamePage {
	/** One gene row: the locus symbol, its genealogy notation, and the plain word for what shows. */
	public record Row(String locus, String symbol, String notation, String shown, String maternal, String paternal,
			String note) {
	}

	/** The three coats of each line, in pelt order. */
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
			rows.add(new Row(line.locus().name().toLowerCase(Locale.ROOT), Genotype.symbol(line.locus()),
					Genotype.notation(line), line.shown(), line.maternal(), line.paternal(),
					spoilers ? line.note().name().toLowerCase(Locale.ROOT) : ""));
		}
		return rows;
	}

	/** The name of the coat this mount wears. */
	public static String pelt(Genome genome, int pelt) {
		Phenotype phenotype = Expression.express(genome);
		List<String> names = PELTS.get(MountSize.form(phenotype));
		return names.get(Math.floorMod(pelt, names.size()));
	}

	/** The wing line, or an empty string for a mount without wings. */
	public static String wings(Genome genome) {
		Phenotype phenotype = Expression.express(genome);
		if (phenotype.wings == Phenotype.WingShow.NONE && !phenotype.chimera) {
			return "";
		}
		return String.format(Locale.ROOT, "Wings x%.1f", phenotype.wingScale);
	}

	public static List<String> family(HerdBook book, HerdBook.Entry entry) {
		ArrayList<String> lines = new ArrayList<>();
		HerdBook.Entry dam = entry.dam() == null ? null : book.get(entry.dam());
		HerdBook.Entry sire = entry.sire() == null ? null : book.get(entry.sire());
		lines.add("Dam: " + (dam == null ? "unknown" : dam.name()));
		lines.add("Sire: " + (sire == null ? "unknown" : sire.name()));
		List<HerdBook.Entry> foals = book.foals(entry.id());
		if (foals.isEmpty()) {
			lines.add("Foals: none");
		} else {
			StringBuilder names = new StringBuilder("Foals:");
			for (HerdBook.Entry foal : foals) {
				names.append(' ').append(foal.name());
			}
			lines.add(names.toString());
		}
		return lines;
	}
}
