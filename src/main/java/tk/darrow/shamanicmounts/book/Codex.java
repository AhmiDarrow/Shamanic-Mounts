package tk.darrow.shamanicmounts.book;

import java.util.List;
import java.util.Locale;

import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;

/** What the herd book says. Everything is open except the chimera, which waits until the reader breeds one. */
public final class Codex {
	public enum Page {
		BASICS, LINES, TAMES, KEY, BREEDING
	}

	/**
	 * One founder line for the gallery. {@code name} stays the English id the harness picks.
	 * {@code blurb()} is the English sentence; the screen translates {@code blurbKey} and {@code nameKey}.
	 */
	public record Line(String name, Genome genome, String blurbKey, boolean untilBred) {
		public String blurb() {
			return Copy.en(blurbKey);
		}

		public String nameKey() {
			return "book.shamanicmounts.line." + name.toLowerCase(Locale.ROOT).replace(' ', '_') + ".name";
		}
	}

	/** One chapter section. {@code heading()} and {@code paragraphs()} are the English source. */
	public record Section(String headingKey, List<String> paragraphKeys) {
		public String heading() {
			return Copy.en(headingKey);
		}

		public List<String> paragraphs() {
			return paragraphKeys.stream().map(Copy::en).toList();
		}
	}

	private Codex() {
	}

	public static List<Page> open() {
		return List.of(Page.BASICS, Page.LINES, Page.TAMES, Page.KEY, Page.BREEDING);
	}

	public static List<Section> basics() {
		return List.of(
				section("book.shamanicmounts.basics.herd",
						"book.shamanicmounts.basics.herd.1",
						"book.shamanicmounts.basics.herd.2",
						"book.shamanicmounts.basics.herd.3",
						"book.shamanicmounts.basics.herd.4"),
				section("book.shamanicmounts.basics.taming",
						"book.shamanicmounts.basics.taming.1",
						"book.shamanicmounts.basics.taming.2",
						"book.shamanicmounts.basics.taming.3",
						"book.shamanicmounts.basics.taming.4",
						"book.shamanicmounts.basics.taming.5"),
				section("book.shamanicmounts.basics.keeping",
						"book.shamanicmounts.basics.keeping.1",
						"book.shamanicmounts.basics.keeping.2",
						"book.shamanicmounts.basics.keeping.3",
						"book.shamanicmounts.basics.keeping.4",
						"book.shamanicmounts.basics.keeping.5"),
				section("book.shamanicmounts.basics.book",
						"book.shamanicmounts.basics.book.1",
						"book.shamanicmounts.basics.book.2"));
	}

	/** The breeding chapter; its chimera section only for a reader who has bred one. */
	public static List<Section> breeding(boolean bredChimera) {
		List<Section> all = breeding();
		return bredChimera ? all
				: all.stream().filter(section -> !"book.shamanicmounts.breeding.chimera".equals(section.headingKey())).toList();
	}

	public static List<Section> breeding() {
		return List.of(
				section("book.shamanicmounts.breeding.pair",
						"book.shamanicmounts.breeding.pair.1",
						"book.shamanicmounts.breeding.pair.2",
						"book.shamanicmounts.breeding.pair.3"),
				section("book.shamanicmounts.breeding.copies",
						"book.shamanicmounts.breeding.copies.1",
						"book.shamanicmounts.breeding.copies.2"),
				section("book.shamanicmounts.breeding.shape",
						"book.shamanicmounts.breeding.shape.1",
						"book.shamanicmounts.breeding.shape.2",
						"book.shamanicmounts.breeding.shape.3"),
				section("book.shamanicmounts.breeding.coat",
						"book.shamanicmounts.breeding.coat.1",
						"book.shamanicmounts.breeding.coat.2",
						"book.shamanicmounts.breeding.coat.3"),
				section("book.shamanicmounts.breeding.gifts",
						"book.shamanicmounts.breeding.gifts.1",
						"book.shamanicmounts.breeding.gifts.2"),
				section("book.shamanicmounts.breeding.chimera",
						"book.shamanicmounts.breeding.chimera.1",
						"book.shamanicmounts.breeding.chimera.2",
						"book.shamanicmounts.breeding.chimera.3"));
	}

	/**
	 * The ten founders and the chimera. Built once: the genomes never change, and the book caches each
	 * line's gene rows by its genome.
	 */
	public static List<Line> lines() {
		return LinesHolder.LINES;
	}

	/** The lines one reader can see: the chimera is not listed until they have bred one. */
	public static List<Line> lines(boolean bredChimera) {
		return bredChimera ? lines() : lines().stream().filter(line -> !line.untilBred()).toList();
	}

	private static final class LinesHolder {
		static final List<Line> LINES = buildLines();
	}

	private static List<Line> buildLines() {
		return List.of(
				line("Eightfold", Founders.eightfold(), "book.shamanicmounts.line.eightfold.blurb", false),
				line("Drum hart", Founders.drumHart(), "book.shamanicmounts.line.drum_hart.blurb", false),
				line("Elk", Founders.elk(), "book.shamanicmounts.line.elk.blurb", false),
				line("Crane", Founders.crane(), "book.shamanicmounts.line.crane.blurb", false),
				line("Nagual", Founders.nagual(), "book.shamanicmounts.line.nagual.blurb", false),
				line("Barghest", Founders.barghest(), "book.shamanicmounts.line.barghest.blurb", false),
				line("Roc", Founders.roc(), "book.shamanicmounts.line.roc.blurb", false),
				line("Shade", Founders.shade(), "book.shamanicmounts.line.shade.blurb", false),
				line("Bear", Founders.bear(), "book.shamanicmounts.line.bear.blurb", false),
				line("Serpent", Founders.serpent(), "book.shamanicmounts.line.serpent.blurb", false),
				line("Chimera", Founders.eightfold().asChimera(), "book.shamanicmounts.line.chimera.blurb", true));
	}

	/** How to read the notation, before the key's entries. English, for the harness and the tests. */
	public static List<String> keyIntro() {
		return keyIntroKeys().stream().map(Copy::en).toList();
	}

	public static List<String> keyIntroKeys() {
		return List.of(
				"book.shamanicmounts.key.intro.1",
				"book.shamanicmounts.key.intro.2",
				"book.shamanicmounts.key.intro.3",
				"book.shamanicmounts.key.intro.4");
	}

	private static Section section(String headingKey, String... paragraphKeys) {
		return new Section(headingKey, List.of(paragraphKeys));
	}

	private static Line line(String name, Genome genome, String blurbKey, boolean untilBred) {
		return new Line(name, genome, blurbKey, untilBred);
	}
}
