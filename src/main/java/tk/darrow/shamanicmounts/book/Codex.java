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
						"Ten lines of spirit mount roam the Overworld, one at a time, each in its own biomes: bears in the woods and the snow, cranes on the rivers, rocs in the mountains. Each stands at least as tall as a horse, and the serpent as long as two.",
						"Where a mount is born decides its pelt: a polar bear on the snow, a black bear in the forest, a snow leopard on the slopes. The cold grows them a little larger.",
						"Every mount is built from genes. Its body, head, legs, tail, pelt, size, and what it does when ridden are each passed down on their own.")),
				new Section("Taming", List.of(
						"Wild adults attack. A golden apple makes one stop for two minutes. Food does not tame a spirit mount.",
						"While it is calm, right-click with a Shamanic Saddle to put it on. A vanilla saddle does not fit.",
						"Right-click again to mount. For thirty seconds it shows a direction. Press that direction with the movement keys or the arrow keys. The mount steps the other way.",
						"Fifteen prompts, two seconds each. Four misses are allowed. Getting off fails the try.",
						"Sneak to step off. On a mount that hides, blinks, or dives, a held sneak keeps you on; tap it to step off.")),
				new Section("Keeping", List.of(
						"Sneak and right-click a tame for its tack: the saddle, Saddle Bags, and horse armor. Foals wear none.",
						"A new tame follows you, and keeps following when you step off. On that screen, tell it to stay and it lies down wherever you leave it, or to wander nearby.",
						"A following mount near you goes with you into another dimension: by portal, by command, or home from the End. Ride one through and you arrive in the saddle.",
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
						"The foal is born wild and keeps near the grown mounts. It wears no saddle, bags, or armor. Once grown, calm it with a golden apple and tame it from the saddle. Its parents go in this book then.")),
				new Section("Two copies of everything", List.of(
						"Every gene comes in two copies: one from the dam, one from the sire. Each parent passes one of its two at random.",
						"Genes that sit together usually travel together: the body with its legs, feet, and gait; the head with build, size, pattern, and pelt; the wings with their span. About one time in eight a neighbour swaps over.")),
				new Section("Body and shape", List.of(
						"The body shows by rank: bear over steed over hart over hound over cat over bird over serpent.",
						"The hidden body still counts. It pulls the neck, head, tail, and girth halfway toward its own shape, so a steed carrying serpent has a longer neck and tail.",
						"The head, the feet, and the tail each show one copy, picked at birth. The other is carried and can come back in a foal.")),
				new Section("Size and coat", List.of(
						"Build is the line's frame; the elk, the roc, and the bear carry giant. Size within the line runs XS to XL, and the two copies average.",
						"Pelt: A shows over B, and B over C. The third pelt of every line needs two copies. Its name follows the body that shows.",
						"Wingspan averages its two copies. A roc carries a vast copy, so bred fliers can have wings three times a crane's.")),
				new Section("Ridden gifts", List.of(
						"Each ridden gift is its own gene, and they stack on whatever body the cross made.",
						"Skin (the nagual's send-away) and veil (the blink) need both copies. One dream copy flies only at night. The shade's hide is not a gift: it needs the ghost phase from both parents. The rest show from one copy.")),
				new Section("The chimera", List.of(
						"When both parents have every gift, the foal is an even chance of the normal cross or the chimera.",
						"The chimera is the furry scaled dragon, with all ten ridden gifts at once, both copies of each: it walks on water, drums, rams, carries two, sends itself away, makes mobs glow, flies any time, blinks, mauls, and coils.",
						"Hiding, gliding, and guarding are not gifts. They come from the ghost phase, full wings, and the guard ward, so the chimera has them only if the cross underneath does. The body genes of the cross stay underneath, and its foals inherit them.")));
	}

	/**
	 * The ten founders and the chimera. The chimera's line waits for spoilers. Built once: the genomes
	 * never change, and the book caches each line's gene rows by its genome.
	 */
	public static List<Line> lines() {
		return LinesHolder.LINES;
	}

	private static final class LinesHolder {
		static final List<Line> LINES = buildLines();
	}

	private static List<Line> buildLines() {
		return List.of(
				new Line("Eightfold", Founders.eightfold(), "Eight hooves. Walks on water and steps up a full block.", false),
				new Line("Drum hart", Founders.drumHart(), "Use sounds the drum: Regeneration for you and friends. Night vision.", false),
				new Line("Elk", Founders.elk(), "Bigger saddle bags. Attack rams with strong knockback.", false),
				new Line("Crane", Founders.crane(), "Two riders. Hold jump to glide.", false),
				new Line("Nagual", Founders.nagual(), "Use sends it away. You get Speed and Jump Boost meanwhile.", false),
				new Line("Barghest", Founders.barghest(), "Hostile mobs glow through walls. Guards: gets up from a stay to fight.", false),
				new Line("Roc", Founders.roc(), "Hold jump to fly and climb, then glide until the bar refills.", false),
				new Line("Shade", Founders.shade(), "Hold sneak to hide from mobs. Sneak and use blinks forward.", false),
				new Line("Bear", Founders.bear(), "Attack mauls everything in front. Both copies: less damage riding.", false),
				new Line("Serpent", Founders.serpent(), "No legs. Swims and dives. Attack coils one foe.", false),
				new Line("Chimera", Founders.eightfold().asChimera(),
						"All ten ridden gifts at once. Hides, glides, or guards only if the cross beneath does. Bred from two mounts with every gift.",
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
