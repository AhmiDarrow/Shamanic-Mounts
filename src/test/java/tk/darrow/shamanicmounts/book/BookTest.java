package tk.darrow.shamanicmounts.book;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.Strand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTest {
	@Test
	void spoilersOffLeavesBasicsAndTamesAndRemembersThat(@TempDir Path dir) throws java.io.IOException {
		assertEquals(java.util.List.of(Codex.Page.BASICS, Codex.Page.LINES, Codex.Page.TAMES, Codex.Page.KEY), Codex.open(false));
		assertTrue(Codex.open(true).contains(Codex.Page.BREEDING));
		String basics = String.join(" ", Codex.basics()).toLowerCase();
		assertFalse(basics.contains("chimera"));
		assertFalse(basics.contains("both copies"));
		String breeding = String.join(" ", Codex.breeding()).toLowerCase();
		assertTrue(breeding.contains("chimera"));
		assertTrue(breeding.contains("both copies"));

		Path file = dir.resolve("spoilers.txt");
		UUID player = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		SpoilerPref pref = new SpoilerPref(file);
		assertFalse(pref.shown(player));
		pref.set(player, false);
		pref.set(other, true);
		SpoilerPref again = new SpoilerPref(file);
		assertFalse(again.shown(player));
		assertTrue(again.shown(other));
		assertTrue(java.nio.file.Files.readString(file).contains(player + " false"));
	}

	@Test
	void aTamePageListsGenesAndReleaseKeepsTheFamilyLine() {
		UUID keeper = UUID.randomUUID();
		UUID stranger = UUID.randomUUID();
		HerdBook book = new HerdBook();
		HerdBook.Entry dam = book.keep(keeper, "  Brook  ", true, Founders.eightfold(), null, null);
		Genome carriedSkin = new Genome(
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT).with(Marks.Gift.SKIN),
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT),
				true, true, true);
		HerdBook.Entry foal = book.keep(keeper, "Ash", false, carriedSkin, dam.id(), null);
		assertEquals("Brook", dam.name());
		assertEquals(2, book.tames(keeper).size());

		assertTrue(book.rename(keeper, foal.id(), "  Ashen Step  "));
		assertEquals("Ashen Step", book.get(foal.id()).name());
		assertFalse(book.rename(stranger, foal.id(), "Nope"));
		assertFalse(book.rename(keeper, foal.id(), "   "));
		assertEquals("Ashen Step", book.get(foal.id()).name());

		TamePage.Row hidden = TamePage.genes(foal.genome(), false).stream()
				.filter(row -> row.locus().equals("skin")).findFirst().orElseThrow();
		assertEquals("none", hidden.shown());
		assertEquals("", hidden.note());
		TamePage.Row noted = TamePage.genes(foal.genome(), true).stream()
				.filter(row -> row.locus().equals("skin")).findFirst().orElseThrow();
		assertEquals("carried", noted.note());
		assertEquals("Dam: Brook", TamePage.family(book, foal).get(0));
		assertTrue(TamePage.family(book, dam).get(2).contains("Ashen Step"));

		assertTrue(book.release(keeper, dam.id()));
		assertEquals(1, book.tames(keeper).size());
		assertEquals("Ashen Step", book.tames(keeper).get(0).name());
		assertTrue(book.forebears(foal.id()).stream().anyMatch(entry -> entry.id().equals(dam.id())));
		assertFalse(book.release(keeper, dam.id()));
	}

	@Test
	void notationCapitalisesTheShowingAlleleAndLowersTheCarriedOne() {
		Genome eightOverFour = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.EIGHT),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.FOUR),
				true, true, true);
		assertEquals("SS", row(eightOverFour, "torso").notation());
		assertEquals("EF", row(eightOverFour, "legs").notation(), "eight with four is the spare pair, both showing");
		assertEquals("Lg", row(eightOverFour, "legs").symbol());
		assertEquals("spare", row(eightOverFour, "legs").shown());
		Genome hoofOverPaw = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Foot.HOOF),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Foot.PAW),
				true, true, true);
		assertEquals("Hp", row(hoofOverPaw, "foot").notation(), "the showing hoof is upper case, the carried paw lower");

		Genome fourOverEight = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.FOUR),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.EIGHT),
				true, true, true);
		assertEquals("FE", row(fourOverEight, "legs").notation(), "a spare pair is both copies showing");

		Genome blend = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED),
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD),
				true, true, true);
		assertEquals("SB", row(blend, "torso").notation());
		assertEquals("Sb", row(blend, "head").notation(), "the maternal head shows, the paternal is carried");

		Genome oneRoad = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Gift.ROAD),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED),
				true, true, true);
		assertEquals("R-", row(oneRoad, "road").notation());
		assertEquals("Ro", row(oneRoad, "road").symbol());
		Genome twoRoads = Genome.homozygous(Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Gift.ROAD));
		assertEquals("RR", row(twoRoads, "road").notation());
		assertEquals("--", row(blend, "road").notation());
		Genome oneSkin = new Genome(
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT).with(Marks.Gift.SKIN),
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT),
				true, true, true);
		assertEquals("s-", row(oneSkin, "skin").notation(), "a lone skin is carried, so its letter is lower case");

		String key = String.join(" ", Codex.key(false));
		assertTrue(key.contains("UPPER"));
		assertTrue(key.contains("Lg legs: N none T two F four P spare E eight"));
		assertFalse(key.contains("Ro road"));
		assertTrue(String.join(" ", Codex.key(true)).contains("Ro road: R on, - empty"));
		assertEquals("Bay", TamePage.pelt(Founders.eightfold(), 0));
		assertEquals("Snow", TamePage.pelt(Founders.nagual(), 2));
		assertEquals("Wings x1.0", TamePage.wings(Founders.crane()));
		assertEquals("", TamePage.wings(Founders.eightfold()));
		assertEquals(11, Codex.lines().size());
	}

	private static TamePage.Row row(Genome genome, String locus) {
		return TamePage.genes(genome, true).stream().filter(row -> row.locus().equals(locus)).findFirst().orElseThrow();
	}
}
