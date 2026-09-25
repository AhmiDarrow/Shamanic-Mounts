package tk.darrow.shamanicmounts.ride;

import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.Phenotype;

/** A readable name for a fresh tame or a foal. The herd book can rename it. */
public final class MountNames {
	private MountNames() {
	}

	public static String of(Phenotype phenotype, boolean baby) {
		if (phenotype.chimera) {
			return "Chimera";
		}
		if (baby) {
			return "Foal";
		}
		if (phenotype.head == Marks.Head.STEED && phenotype.gifts.contains(Marks.Gift.ROAD)) {
			return "Eightfold";
		}
		if (phenotype.head == Marks.Head.HART && phenotype.gifts.contains(Marks.Gift.CALL)) {
			return "Drum Hart";
		}
		if (phenotype.head == Marks.Head.HART && phenotype.gifts.contains(Marks.Gift.BEARING)) {
			return "Elk";
		}
		if (phenotype.head == Marks.Head.BIRD && phenotype.gifts.contains(Marks.Gift.DREAM)) {
			return "Roc";
		}
		if (phenotype.head == Marks.Head.BIRD && phenotype.gifts.contains(Marks.Gift.FERRY)) {
			return "Crane";
		}
		if (phenotype.head == Marks.Head.CAT && phenotype.gifts.contains(Marks.Gift.SKIN)) {
			return "Nagual";
		}
		if (phenotype.head == Marks.Head.HOUND && phenotype.gifts.contains(Marks.Gift.OMEN)) {
			return "Barghest";
		}
		if (phenotype.head == Marks.Head.CAT && phenotype.gifts.contains(Marks.Gift.VEIL)) {
			return "Shade";
		}
		if (phenotype.head == Marks.Head.BEAR && phenotype.gifts.contains(Marks.Gift.MIGHT)) {
			return "Bear";
		}
		if (phenotype.head == Marks.Head.SERPENT && phenotype.gifts.contains(Marks.Gift.COIL)) {
			return "Serpent";
		}
		return "Mount";
	}
}
