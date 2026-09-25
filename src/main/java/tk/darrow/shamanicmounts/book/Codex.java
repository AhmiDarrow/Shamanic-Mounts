package tk.darrow.shamanicmounts.book;

import java.util.List;

import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;

/** What the herd book says. Spoilers add the breeding chapter and the gift half of the key. */
public final class Codex {
	public enum Page {
		BASICS, LINES, TAMES, KEY, BREEDING
	}

	/** One founder line for the gallery: its name, its genome, and one line on what it does. */
	public record Line(String name, Genome genome, String blurb, boolean spoiler) {
	}

	/** One chapter section: a heading, then its paragraphs. */
	public record Section(String heading, List<String> paragraphs) {
	}

	/** What a spoiler-hidden line says in the gallery. */
	public static final String HIDDEN_LINE = "The eleventh form. Turn spoilers on to read it.";

	private Codex() {
	}

	public static List<Page> open(boolean spoilers) {
		if (spoilers) {
			return List.of(Page.BASICS, Page.LINES, Page.TAMES, Page.KEY, Page.BREEDING);
		}
		return List.of(Page.BASICS, Page.LINES, Page.TAMES, Page.KEY);
	}

	public static List<Section> basics() {
		return List.of(
				new Section("The herd", List.of(
						"Ten lines of spirit mount roam the Overworld, one at a time. Each stands at least as tall as a horse, and the serpent as long as two.",
						"Every mount is built from genes. Its body, head, legs, tail, pelt, size, and what it does when ridden are each passed down on their own.")),
				new Section("Taming", List.of(
						"Food does not tame a spirit mount. Craft a Shamanic Saddle; a vanilla saddle does not fit.",
						"Right-click a wild adult with the saddle. It stays put. Hold jump through four jolts to tame it.",
						"Sneak to step off. On a mount that hides, blinks, or dives, a held sneak keeps you on; tap it to step off.")),
				new Section("Keeping", List.of(
						"Sneak and right-click a tame for its tack: the saddle, Saddle Bags, and horse armor. Foals wear none.",
						"Tell it to follow, stay, or wander. Staying, it lies down.",
						"Your tames are listed here, loaded or not. Open one for its genes and family, to rename it, or to release it.")),
				new Section("This book", List.of(
						"Lines shows every founder and its three pelts. Key explains how genes are written.",
						"Spoilers add the breeding chapter and the gift genes. That switch is saved on this computer for you.")));
	}

	public static List<Section> breeding() {
		return List.of(
				new Section("A pair and a foal", List.of(
						"Any two mounts can breed. Feed a Diamond Apple to one tame adult you own, then the other within ten seconds and eight blocks. One foal. Those two rest for five minutes.",
						"An operator can feed them during that rest, and a mount an operator readied stays ready until the pair is made.",
						"The foal is born wild and keeps near the grown mounts. It wears no saddle, bags, or armor. Once grown, tame it with the saddle. Its parents go in this book then.")),
				new Section("Two copies of everything", List.of(
						"Every gene comes in two copies: one from the dam, one from the sire. Each parent passes one of its two at random.",
						"Genes that sit together usually travel together: the body with its legs, feet, and gait; the head with build, size, pattern, and pelt; the wings with their span. About one time in eight a neighbour swaps over.")),
				new Section("Body and shape", List.of(
						"The body shows by rank: bear over steed over hart over hound over cat over bird over serpent.",
						"The hidden body still counts. It pulls the neck, head, tail, and girth halfway toward its own shape, so a steed carrying serpent has a longer neck and tail.",
						"The head, the feet, and the tail each show one copy, picked at birth. The other is carried and can come back in a foal.")),
				new Section("Size and coat", List.of(
						"Build is the line's frame; the elk and the roc carry giant. Size within the line runs XS to XL, and the two copies average.",
						"Pelt: A shows over B, and B over C. The third pelt of every line needs two copies. Its name follows the body that shows.",
						"Wingspan averages its two copies. A roc carries a vast copy, so bred fliers can have wings three times a crane's.")),
				new Section("Ridden gifts", List.of(
						"Each ridden gift is its own gene, and they stack on whatever body the cross made.",
						"Skin (the hide) and veil (the blink) need both copies. One dream copy flies only at night. The nagual's send-away needs its bond from both parents. The rest show from one copy.")),
				new Section("The chimera", List.of(
						"When both parents have every gift, the foal is an even chance of the normal cross or the chimera.",
						"The chimera is the furry scaled dragon, with every gift at once. The body genes of the cross stay underneath, and its foals inherit them.")));
	}

	/** The ten founders and the chimera. The chimera's line waits for spoilers. */
	public static List<Line> lines() {
		return List.of(
				new Line("Eightfold", Founders.eightfold(), "Eight hooves. Walks on water and steps up a full block.", false),
				new Line("Drum hart", Founders.drumHart(), "Use sounds the drum: Regeneration for you and friends. Night vision.", false),
				new Line("Elk", Founders.elk(), "Bigger saddle bags. Attack rams with strong knockback.", false),
				new Line("Crane", Founders.crane(), "Two riders. Hold jump to glide.", false),
				new Line("Nagual", Founders.nagual(), "Use sends it away. You get Speed and Jump Boost meanwhile.", false),
				new Line("Barghest", Founders.barghest(), "Hostile mobs glow through walls. Stays and guards you.", false),
				new Line("Roc", Founders.roc(), "Hold jump to fly and climb, then glide until the bar refills.", false),
				new Line("Shade", Founders.shade(), "Hold sneak to hide from mobs. Sneak and use blinks forward.", false),
				new Line("Bear", Founders.bear(), "Attack mauls everything in front. Both copies: less damage riding.", false),
				new Line("Serpent", Founders.serpent(), "No legs. Swims and dives. Attack coils one foe.", false),
				new Line("Chimera", Founders.eightfold().asChimera(), "Every ability at once. Bred from two mounts that have them all.",
						true));
	}

	/** How to read the notation, before the key's entries. */
	public static List<String> keyIntro() {
		return List.of(
				"Every mount carries two copies of each gene. The book writes the dam's copy first, then the sire's.",
				"A capital letter shows. A small letter is carried: it does not show on this mount, but it can in a foal.",
				"Two capitals mean both copies show at once, or average.",
				"On a tame's page, hover a gene to see how it passes down.");
	}
}
