package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tk.darrow.shamanicmounts.genome.Locus;

/**
 * Genealogy notation for the book. Each locus has a two-letter symbol and each allele a letter.
 * An allele that is showing is written in upper case and one that is only carried in lower case,
 * so a mount with eight legs over four reads {@code Lg Ef}. Two capitals mean both copies show
 * at once. A gift locus writes its empty copy as a dash: {@code Ro R-} is the road from one parent.
 */
public final class Genotype {
	private static final Map<Locus, String> SYMBOLS = Map.ofEntries(
			Map.entry(Locus.TORSO, "To"), Map.entry(Locus.HEAD, "Hd"), Map.entry(Locus.LEGS, "Lg"),
			Map.entry(Locus.FOOT, "Ft"), Map.entry(Locus.RACK, "Rk"), Map.entry(Locus.GAIT, "Gt"),
			Map.entry(Locus.WINGS, "Wg"), Map.entry(Locus.WINGSPAN, "Ws"), Map.entry(Locus.TAIL, "Tl"),
			Map.entry(Locus.SCALE, "Sc"), Map.entry(Locus.COAT, "Ct"), Map.entry(Locus.REALM, "Rm"),
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
			Map.entry(Locus.COAT, Map.of("solid", "S", "rosette", "R", "star", "T", "dusk", "D", "bone", "B")),
			Map.entry(Locus.REALM, Map.of("hearth", "H", "upper", "U", "lower", "L", "sideways", "S")),
			Map.entry(Locus.PHASE, Map.of("solid", "S", "veil", "V", "ghost", "G")),
			Map.entry(Locus.SENSE, Map.of("eye", "E", "scent", "S")),
			Map.entry(Locus.BOND, Map.of("saddle", "S", "drum", "D", "nagual", "N")),
			Map.entry(Locus.WARD, Map.of("none", "N", "longevity", "L", "guard", "G")),
			Map.entry(Locus.TRAIL, Map.of("none", "N", "star", "S", "shadow", "H", "frost", "F")));

	/** The plain name of every locus, for the key. */
	private static final Map<Locus, String> NAMES = Map.ofEntries(
			Map.entry(Locus.TORSO, "torso"), Map.entry(Locus.HEAD, "head"), Map.entry(Locus.LEGS, "legs"),
			Map.entry(Locus.FOOT, "feet"), Map.entry(Locus.RACK, "rack"), Map.entry(Locus.GAIT, "gait"),
			Map.entry(Locus.WINGS, "wings"), Map.entry(Locus.WINGSPAN, "wingspan"), Map.entry(Locus.TAIL, "tail"),
			Map.entry(Locus.SCALE, "size"), Map.entry(Locus.COAT, "coat"), Map.entry(Locus.REALM, "realm"),
			Map.entry(Locus.PHASE, "phase"), Map.entry(Locus.SENSE, "sense"), Map.entry(Locus.BOND, "bond"),
			Map.entry(Locus.WARD, "ward"), Map.entry(Locus.TRAIL, "trail"), Map.entry(Locus.ROAD, "road"),
			Map.entry(Locus.CALL, "call"), Map.entry(Locus.BEARING, "bearing"), Map.entry(Locus.FERRY, "ferry"),
			Map.entry(Locus.SKIN, "skin"), Map.entry(Locus.OMEN, "omen"), Map.entry(Locus.DREAM, "dream"),
			Map.entry(Locus.VEIL, "veil"), Map.entry(Locus.MIGHT, "might"), Map.entry(Locus.COIL, "coil"));

	/** Allele codes in the order the key lists them. */
	private static final Map<Locus, List<String>> ORDER = Map.ofEntries(
			Map.entry(Locus.TORSO, List.of("steed", "hart", "cat", "bird", "hound", "bear", "serpent")),
			Map.entry(Locus.HEAD, List.of("steed", "hart", "cat", "bird", "hound", "bear", "serpent")),
			Map.entry(Locus.LEGS, List.of("none", "two", "four", "spare", "eight")),
			Map.entry(Locus.FOOT, List.of("hoof", "paw", "talon")),
			Map.entry(Locus.RACK, List.of("none", "buds", "full", "crown")),
			Map.entry(Locus.GAIT, List.of("land", "sea", "cinder")),
			Map.entry(Locus.WINGS, List.of("none", "vestigial", "full", "astral")),
			Map.entry(Locus.WINGSPAN, List.of("small", "mid", "large", "great", "vast")),
			Map.entry(Locus.TAIL, List.of("none", "plume", "flag", "lash", "fan")),
			Map.entry(Locus.SCALE, List.of("slight", "normal", "giant")),
			Map.entry(Locus.COAT, List.of("solid", "rosette", "star", "dusk", "bone")),
			Map.entry(Locus.REALM, List.of("hearth", "upper", "lower", "sideways")),
			Map.entry(Locus.PHASE, List.of("solid", "veil", "ghost")),
			Map.entry(Locus.SENSE, List.of("eye", "scent")),
			Map.entry(Locus.BOND, List.of("saddle", "drum", "nagual")),
			Map.entry(Locus.WARD, List.of("none", "longevity", "guard")),
			Map.entry(Locus.TRAIL, List.of("none", "star", "shadow", "frost")));

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

	/** The letter for one allele code at a locus, before any case is applied. */
	public static String letter(Locus locus, String code) {
		if (isGift(locus)) {
			return "none".equals(code) ? "-" : NAMES.get(locus).substring(0, 1).toUpperCase(Locale.ROOT);
		}
		String letter = LETTERS.get(locus).get(code);
		return letter == null ? "?" : letter;
	}

	/**
	 * Both alleles as letters, the showing one in upper case and a carried one in lower case. A
	 * blended, incomplete, or codominant reading, or two copies of the same allele, both show.
	 */
	public static String notation(Reading.Line line) {
		Locus locus = line.locus();
		String maternal = letter(locus, line.maternal());
		String paternal = letter(locus, line.paternal());
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

	/** The key, one line per body locus: symbol, name, then each letter and its meaning. */
	public static List<String> bodyKey() {
		ArrayList<String> lines = new ArrayList<>();
		for (Locus locus : Locus.values()) {
			if (isGift(locus)) {
				continue;
			}
			StringBuilder text = new StringBuilder(symbol(locus)).append(' ').append(name(locus)).append(':');
			for (String code : ORDER.get(locus)) {
				text.append(' ').append(letter(locus, code)).append(' ').append(code);
			}
			lines.add(text.toString());
		}
		return lines;
	}

	/** The key for the gift loci, which the breeding chapter's spoiler unlocks. */
	public static List<String> giftKey() {
		ArrayList<String> lines = new ArrayList<>();
		for (Locus locus : Locus.values()) {
			if (isGift(locus)) {
				lines.add(symbol(locus) + " " + name(locus) + ": " + letter(locus, "on") + " on, - empty");
			}
		}
		return lines;
	}
}
