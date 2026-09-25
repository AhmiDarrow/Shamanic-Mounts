package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tk.darrow.shamanicmounts.genome.Locus;
import tk.darrow.shamanicmounts.genome.Marks;

/**
 * Genealogy notation for the book. Each gene has a two-letter symbol and each allele a letter.
 * An allele that is showing is written in upper case and one that is only carried in lower case,
 * dam's copy first, so a hoof over a paw reads {@code Ft Hp}. Two capitals mean both copies show at
 * once or average, as eight legs with four do: {@code Lg EF}. Size prints its classes whole with a
 * slash: {@code Sz XS/L}. A
 * gift locus writes its empty copy as a dash: {@code Ro R-} is the road from one parent.
 */
public final class Genotype {
	private static final Map<Locus, String> SYMBOLS = Map.ofEntries(
			Map.entry(Locus.TORSO, "To"), Map.entry(Locus.HEAD, "Hd"), Map.entry(Locus.LEGS, "Lg"),
			Map.entry(Locus.FOOT, "Ft"), Map.entry(Locus.RACK, "Rk"), Map.entry(Locus.GAIT, "Gt"),
			Map.entry(Locus.WINGS, "Wg"), Map.entry(Locus.WINGSPAN, "Ws"), Map.entry(Locus.TAIL, "Tl"),
			Map.entry(Locus.SCALE, "Bu"), Map.entry(Locus.SIZE, "Sz"), Map.entry(Locus.COAT, "Ct"),
			Map.entry(Locus.PELT, "Pt"), Map.entry(Locus.REALM, "Rm"),
			Map.entry(Locus.PHASE, "Ph"), Map.entry(Locus.SENSE, "Se"), Map.entry(Locus.BOND, "Bd"),
			Map.entry(Locus.WARD, "Wd"), Map.entry(Locus.TRAIL, "Tr"), Map.entry(Locus.ROAD, "Ro"),
			Map.entry(Locus.CALL, "Ca"), Map.entry(Locus.BEARING, "Be"), Map.entry(Locus.FERRY, "Fe"),
			Map.entry(Locus.SKIN, "Sk"), Map.entry(Locus.OMEN, "Om"), Map.entry(Locus.DREAM, "Dr"),
			Map.entry(Locus.VEIL, "Ve"), Map.entry(Locus.MIGHT, "Mi"), Map.entry(Locus.COIL, "Co"));

	/** Allele code to letter, per locus. Gift loci are not here: their letter is the gift's initial. */
	private static final Map<Locus, Map<String, String>> LETTERS = Map.ofEntries(
			Map.entry(Locus.TORSO, Map.of("steed", "S", "hart", "H", "cat", "C", "bird", "B", "hound", "D", "bear", "U", "serpent", "N")),
			Map.entry(Locus.HEAD, Map.of("steed", "S", "hart", "H", "cat", "C", "bird", "B", "hound", "D", "bear", "U", "serpent", "N")),
			Map.entry(Locus.LEGS, Map.of("none", "N", "two", "T", "four", "F", "spare", "P", "eight", "E")),
			Map.entry(Locus.FOOT, Map.of("hoof", "H", "paw", "P", "talon", "T")),
			Map.entry(Locus.RACK, Map.of("none", "N", "buds", "B", "full", "F", "crown", "C")),
			Map.entry(Locus.GAIT, Map.of("land", "L", "sea", "S", "cinder", "C")),
			Map.entry(Locus.WINGS, Map.of("none", "N", "vestigial", "V", "full", "F", "astral", "A")),
			Map.entry(Locus.WINGSPAN, Map.of("small", "S", "mid", "M", "large", "L", "great", "G", "vast", "V")),
			Map.entry(Locus.TAIL, Map.of("none", "N", "plume", "P", "flag", "F", "lash", "L", "fan", "A")),
			Map.entry(Locus.SCALE, Map.of("slight", "S", "normal", "N", "giant", "G")),
			Map.entry(Locus.SIZE, Map.of("xs", "XS", "s", "S", "m", "M", "l", "L", "xl", "XL")),
			Map.entry(Locus.COAT, Map.of("solid", "S", "rosette", "R", "star", "T", "dusk", "D", "bone", "B")),
			Map.entry(Locus.PELT, Map.of("a", "A", "b", "B", "c", "C")),
			Map.entry(Locus.REALM, Map.of("hearth", "H", "upper", "U", "lower", "L", "sideways", "S")),
			Map.entry(Locus.PHASE, Map.of("solid", "S", "veil", "V", "ghost", "G")),
			Map.entry(Locus.SENSE, Map.of("eye", "E", "scent", "S")),
			Map.entry(Locus.BOND, Map.of("saddle", "S", "drum", "D", "nagual", "N")),
			Map.entry(Locus.WARD, Map.of("none", "N", "longevity", "L", "guard", "G")),
			Map.entry(Locus.TRAIL, Map.of("none", "N", "star", "S", "shadow", "H", "frost", "F")));

	/** The plain name of every locus. */
	private static final Map<Locus, String> NAMES = Map.ofEntries(
			Map.entry(Locus.TORSO, "body"), Map.entry(Locus.HEAD, "head"), Map.entry(Locus.LEGS, "legs"),
			Map.entry(Locus.FOOT, "feet"), Map.entry(Locus.RACK, "antlers"), Map.entry(Locus.GAIT, "gait"),
			Map.entry(Locus.WINGS, "wings"), Map.entry(Locus.WINGSPAN, "wingspan"), Map.entry(Locus.TAIL, "tail"),
			Map.entry(Locus.SCALE, "build"), Map.entry(Locus.SIZE, "size"), Map.entry(Locus.COAT, "pattern"),
			Map.entry(Locus.PELT, "pelt"), Map.entry(Locus.REALM, "realm"),
			Map.entry(Locus.PHASE, "phase"), Map.entry(Locus.SENSE, "sense"), Map.entry(Locus.BOND, "bond"),
			Map.entry(Locus.WARD, "ward"), Map.entry(Locus.TRAIL, "trail"), Map.entry(Locus.ROAD, "road"),
			Map.entry(Locus.CALL, "call"), Map.entry(Locus.BEARING, "bearing"), Map.entry(Locus.FERRY, "ferry"),
			Map.entry(Locus.SKIN, "skin"), Map.entry(Locus.OMEN, "omen"), Map.entry(Locus.DREAM, "dream"),
			Map.entry(Locus.VEIL, "veil"), Map.entry(Locus.MIGHT, "might"), Map.entry(Locus.COIL, "coil"));

	/** Allele codes in the order the key lists them. Where one copy wins, the winner comes first. */
	private static final Map<Locus, List<String>> ORDER = Map.ofEntries(
			Map.entry(Locus.TORSO, List.of("bear", "steed", "hart", "hound", "cat", "bird", "serpent")),
			Map.entry(Locus.HEAD, List.of("steed", "hart", "cat", "bird", "hound", "bear", "serpent")),
			Map.entry(Locus.LEGS, List.of("none", "two", "four", "spare", "eight")),
			Map.entry(Locus.FOOT, List.of("hoof", "paw", "talon")),
			Map.entry(Locus.RACK, List.of("crown", "full", "buds", "none")),
			Map.entry(Locus.GAIT, List.of("sea", "cinder", "land")),
			Map.entry(Locus.WINGS, List.of("none", "vestigial", "full", "astral")),
			Map.entry(Locus.WINGSPAN, List.of("small", "mid", "large", "great", "vast")),
			Map.entry(Locus.TAIL, List.of("none", "plume", "flag", "lash", "fan")),
			Map.entry(Locus.SCALE, List.of("giant", "normal", "slight")),
			Map.entry(Locus.SIZE, List.of("xs", "s", "m", "l", "xl")),
			Map.entry(Locus.COAT, List.of("bone", "solid", "rosette", "star", "dusk")),
			Map.entry(Locus.PELT, List.of("a", "b", "c")),
			Map.entry(Locus.REALM, List.of("hearth", "upper", "lower", "sideways")),
			Map.entry(Locus.PHASE, List.of("veil", "solid", "ghost")),
			Map.entry(Locus.SENSE, List.of("eye", "scent")),
			Map.entry(Locus.BOND, List.of("drum", "saddle", "nagual")),
			Map.entry(Locus.WARD, List.of("none", "longevity", "guard")),
			Map.entry(Locus.TRAIL, List.of("none", "star", "shadow", "frost")));

	/** How each gene passes down and shows, in one line. The key prints it and a tame's page shows it on hover. */
	private static final Map<Locus, String> RULES = Map.ofEntries(
			Map.entry(Locus.TORSO, "Dominant: U > S > H > D > C > B > N. The hidden body pulls the neck, head, tail, and girth toward its own."),
			Map.entry(Locus.HEAD, "One copy shows, picked at birth. The other is carried."),
			Map.entry(Locus.LEGS, "Mixed counts meet in the middle: eight with four shows the spare pair, two with four a hitched pair. None is recessive."),
			Map.entry(Locus.FOOT, "One copy shows, picked at birth. The head has no say."),
			Map.entry(Locus.RACK, "The bigger rack shows. Over none it comes in one step smaller. Two crowns make the elk's heavy rack."),
			Map.entry(Locus.GAIT, "Sea shows over land. Cinder needs two copies. Sea with cinder shows mist."),
			Map.entry(Locus.WINGS, "Two full or two astral show whole. Full with astral shows both. Full over vestigial shows full. Any other mix shows pinions."),
			Map.entry(Locus.WINGSPAN, "The two copies average: small x0.6, mid x1.0, large x1.6, great x2.2, vast x3.0."),
			Map.entry(Locus.TAIL, "One copy shows, picked at birth. A tail over none comes in as a stub."),
			Map.entry(Locus.SCALE, "The line's frame. Giant shows from one copy and is greater with two. Slight needs two."),
			Map.entry(Locus.SIZE, "Size within the line. The copies average: XS x0.86, S x0.93, M x1.0, L x1.07, XL x1.14."),
			Map.entry(Locus.COAT, "Bone covers every pattern. Solid over rosette shows solid. Other mixes show both."),
			Map.entry(Locus.PELT, "Dominant: A > B > C. The names follow the body that shows."),
			Map.entry(Locus.REALM, "Every realm other than hearth shows, and two different realms show together."),
			Map.entry(Locus.PHASE, "Veil shows from one copy. Ghost needs two. Veil with ghost shows the deep veil."),
			Map.entry(Locus.SENSE, "Eye shows over scent. Scent needs two copies."),
			Map.entry(Locus.BOND, "Drum shows from one copy. The nagual's bond needs two."),
			Map.entry(Locus.WARD, "Every ward shows from one copy, and two different wards show together."),
			Map.entry(Locus.TRAIL, "Every trail shows from one copy, and two different trails show together."));

	private Genotype() {
	}

	public static String symbol(Locus locus) {
		return SYMBOLS.get(locus);
	}

	public static String name(Locus locus) {
		return NAMES.get(locus);
	}

	public static boolean isGift(Locus locus) {
		return !LETTERS.containsKey(locus);
	}

	/** How a gene passes down, in one line. */
	public static String rule(Locus locus) {
		if (isGift(locus)) {
			Marks.Gift gift = giftAt(locus);
			if (gift == Marks.Gift.DREAM) {
				return "One copy flies by night. Two copies fly day and night, without hunger.";
			}
			return gift != null && gift.recessive() ? "Shows only with two copies. One copy is carried."
					: "Shows from one copy.";
		}
		return RULES.getOrDefault(locus, "");
	}

	private static Marks.Gift giftAt(Locus locus) {
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift != Marks.Gift.NONE && gift.locus() == locus) {
				return gift;
			}
		}
		return null;
	}

	/** The letter for one allele code at a locus, before any case is applied. */
	public static String letter(Locus locus, String code) {
		if (isGift(locus)) {
			return "none".equals(code) ? "-" : NAMES.get(locus).substring(0, 1).toUpperCase(Locale.ROOT);
		}
		String letter = LETTERS.get(locus).get(code);
		return letter == null ? "?" : letter;
	}

	/**
	 * Both alleles as letters, dam's first, the showing one in upper case and a carried one in lower
	 * case. A blended, incomplete, or codominant reading, or two copies of the same allele, both show.
	 */
	public static String notation(Reading.Line line) {
		Locus locus = line.locus();
		String maternal = letter(locus, line.maternal());
		String paternal = letter(locus, line.paternal());
		if (locus == Locus.SIZE) {
			return maternal + "/" + paternal;
		}
		boolean bothShow = line.note() == Reading.Note.BLENDED || line.note() == Reading.Note.CODOMINANT
				|| line.note() == Reading.Note.INCOMPLETE || line.note() == Reading.Note.CHIMERA
				|| line.maternal().equals(line.paternal());
		if (isGift(locus)) {
			boolean shown = !"none".equals(line.shown());
			return caseOf(maternal, shown) + caseOf(paternal, shown);
		}
		if (bothShow) {
			return maternal + paternal;
		}
		boolean maternalShows = line.maternal().equals(line.shown());
		boolean paternalShows = line.paternal().equals(line.shown());
		if (!maternalShows && !paternalShows) {
			return maternal + paternal;
		}
		return caseOf(maternal, maternalShows) + caseOf(paternal, paternalShows);
	}

	private static String caseOf(String letter, boolean shows) {
		if ("-".equals(letter)) {
			return letter;
		}
		return shows ? letter.toUpperCase(Locale.ROOT) : letter.toLowerCase(Locale.ROOT);
	}

	/** One key entry: the symbol and name, every letter with its meaning, and the rule. */
	public record KeyEntry(String symbol, String name, String letters, String rule) {
	}

	/** The key for the body loci, in the order a tame's page prints them. */
	public static List<KeyEntry> bodyKey() {
		ArrayList<KeyEntry> entries = new ArrayList<>();
		for (Locus locus : KEY_ORDER) {
			StringBuilder letters = new StringBuilder();
			for (String code : ORDER.get(locus)) {
				if (letters.length() > 0) {
					letters.append("  ");
				}
				String word = locus == Locus.PELT ? peltWord(code) : code;
				letters.append(letter(locus, code)).append(' ').append(word);
			}
			entries.add(new KeyEntry(symbol(locus), name(locus), letters.toString(), rule(locus)));
		}
		return entries;
	}

	private static String peltWord(String code) {
		return switch (code) {
			case "a" -> "first";
			case "b" -> "second";
			default -> "third";
		};
	}

	/** Body loci in page order: body, size, coat, wings, nature. */
	private static final List<Locus> KEY_ORDER = List.of(Locus.TORSO, Locus.HEAD, Locus.LEGS, Locus.FOOT, Locus.TAIL,
			Locus.RACK, Locus.SCALE, Locus.SIZE, Locus.PELT, Locus.COAT, Locus.PHASE, Locus.WINGS, Locus.WINGSPAN,
			Locus.GAIT, Locus.REALM, Locus.SENSE, Locus.BOND, Locus.WARD, Locus.TRAIL);

	/** The key for the gift loci, which the breeding chapter's spoiler unlocks. */
	public static List<KeyEntry> giftKey() {
		ArrayList<KeyEntry> entries = new ArrayList<>();
		for (Locus locus : Locus.values()) {
			if (isGift(locus)) {
				entries.add(new KeyEntry(symbol(locus), name(locus), letter(locus, "on") + " on  - empty", rule(locus)));
			}
		}
		return entries;
	}
}
