package tk.darrow.shamanicmounts.book;

import java.util.ArrayList;
import java.util.List;

import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;

/** What the herd book shows. Spoilers add the breeding chapter and the gift half of the key. */
public final class Codex {
	public enum Page {
		BASICS, LINES, TAMES, KEY, BREEDING
	}

	/** One founder line for the gallery: its name, its genome, and one line on what it does. */
	public record Line(String name, Genome genome, String blurb, boolean spoiler) {
	}

	private Codex() {
	}

	public static List<Page> open(boolean spoilers) {
		if (spoilers) {
			return List.of(Page.BASICS, Page.LINES, Page.TAMES, Page.KEY, Page.BREEDING);
		}
		return List.of(Page.BASICS, Page.LINES, Page.TAMES, Page.KEY);
	}

	public static List<String> basics() {
		return List.of(
				"Ten mounts. Each one stands at least as tall as a horse, and the serpent as long as two.",
				"Every mount needs a Shamanic Saddle. A vanilla saddle does not fit.",
				"Right-click a wild adult with the saddle. It stays put. Hold jump through four jolts to tame it.",
				"Sneak to step off. A mount that hides or blinks keeps you on while sneak is held; tap it to step off.",
				"Sneak and right-click a tame for its tack: the saddle, Saddle Bags, and horse armor. Tell it to follow, stay, or wander. Staying, it lies down.",
				"Your tames are listed here, loaded or not, with their genes, parents, and children.",
				"Rename a tame, or release it.",
				"Spoilers add the breeding chapter. That switch is saved on this computer for you.");
	}

	public static List<String> breeding() {
		return List.of(
				"Any two mounts can breed. The foal is one animal.",
				"Head, feet, wings, and tail are separate genes. Wingspan is its own gene and the two copies average.",
				"Each ridden ability is its own gene, and they stack on whatever body the cross made.",
				"Send-away, hide, blink, and the full dream need both copies. The other abilities show from one copy.",
				"Feed a Diamond Apple to one tame adult you own, then the other within ten seconds and eight blocks. One foal. Those two rest for five minutes.",
				"An operator can feed them during that rest, and a mount an operator readied stays ready until the pair is made.",
				"A foal wears one of its parents' coats. It is born wild and keeps near the grown mounts.",
				"A foal wears no saddle, bags, or armor. Once grown, tame it with the saddle. Its parents go in this book then.",
				"When both parents have every ability, the foal is an even chance of the normal cross or the chimera.",
				"The chimera is the furry scaled dragon. It has every ability. The body genes of the cross stay underneath.");
	}

	/** The eight founders and the chimera, with a line each. The chimera's line waits for spoilers. */
	public static List<Line> lines() {
		return List.of(
				new Line("Eightfold", Founders.eightfold(), "Eight hooves. Walks on water and steps up a block.", false),
				new Line("Drum hart", Founders.drumHart(), "Use sounds the drum: Regeneration for you and friends. Night vision.", false),
				new Line("Elk", Founders.elk(), "Bigger saddle bags. Attack rams with strong knockback.", false),
				new Line("Crane", Founders.crane(), "Two riders. Hold jump to glide.", false),
				new Line("Nagual", Founders.nagual(), "Use sends it away. You get Speed and Jump Boost meanwhile.", false),
				new Line("Barghest", Founders.barghest(), "Hostile mobs glow through walls. Sits and guards you.", false),
				new Line("Roc", Founders.roc(), "Hold jump to fly and climb, then glide until the bar refills.", false),
				new Line("Shade", Founders.shade(), "Hold sneak to hide from mobs. Sneak and use blinks forward.", false),
				new Line("Bear", Founders.bear(), "Attack mauls everything in front. Both copies: you take less damage riding.", false),
				new Line("Serpent", Founders.serpent(), "No legs. Swims; hold jump to rise, hold sneak to sink. Attack coils one foe.", false),
				new Line("Chimera", Founders.eightfold().asChimera(), "Every ability at once. Bred from two mounts that have them all.",
						true));
	}

	/** The key page: how to read the notation, then the body loci, then the gifts if spoilers are on. */
	public static List<String> key(boolean spoilers) {
		ArrayList<String> lines = new ArrayList<>();
		lines.add("UPPER case shows. lower case is carried. Two capitals blend or share.");
		lines.add("Every mount has two copies of each gene, dam's then sire's.");
		lines.add("");
		lines.addAll(Genotype.bodyKey());
		if (spoilers) {
			lines.add("");
			lines.add("Ridden abilities, one letter on and a dash for an empty copy:");
			lines.addAll(Genotype.giftKey());
		}
		return lines;
	}
}
