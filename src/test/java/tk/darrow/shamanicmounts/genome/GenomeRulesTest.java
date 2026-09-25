package tk.darrow.shamanicmounts.genome;

import java.util.EnumSet;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.book.Reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenomeRulesTest {
	@Test
	void everyLocusSitsOnOneChromosome() {
		int seen = 0;
		for (Locus locus : Locus.values()) {
			assertTrue(Chromosome.holding(locus).indexOf(locus) >= 0);
			seen++;
		}
		assertEquals(Locus.values().length, seen);
	}

	@Test
	void noLegsIsRecessiveAndTheSerpentBreedsTrue() {
		assertEquals(Phenotype.LegShow.FOUR, Expression.legs(Marks.Leg.NONE, Marks.Leg.FOUR));
		assertEquals(Phenotype.LegShow.EIGHT, Expression.legs(Marks.Leg.EIGHT, Marks.Leg.NONE));
		assertEquals(Phenotype.LegShow.TWO, Expression.legs(Marks.Leg.NONE, Marks.Leg.TWO));
		assertEquals(Phenotype.LegShow.NONE, Expression.legs(Marks.Leg.NONE, Marks.Leg.NONE));
		Phenotype serpent = Expression.express(Founders.serpent());
		assertEquals(Phenotype.LegShow.NONE, serpent.legs);
		assertTrue(serpent.gifts.contains(Marks.Gift.COIL));
		assertEquals(MountSize.Form.SERPENT, MountSize.form(serpent));
		Phenotype bear = Expression.express(Founders.bear());
		assertTrue(bear.gifts.contains(Marks.Gift.MIGHT));
		assertEquals(MountSize.Form.BEAR, MountSize.form(bear));
	}

	@Test
	void wingspanAveragesBothCopies() {
		Genome wide = new Genome(
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Wing.FULL).with(Marks.Span.VAST),
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Wing.FULL).with(Marks.Span.SMALL),
				true, true, true);
		assertEquals((Marks.Span.VAST.factor + Marks.Span.SMALL.factor) * 0.5f, Expression.express(wide).wingScale, 1.0e-6f);
		assertEquals(1.0f, Expression.express(Founders.crane()).wingScale, 1.0e-6f);
	}

	@Test
	void fourWithEightShowsTheShortMiddlePair() {
		Phenotype phenotype = express(Marks.Leg.FOUR, Marks.Leg.EIGHT);
		assertEquals(Phenotype.LegShow.SPARE, phenotype.legs);
		assertEquals(Reading.Note.INCOMPLETE, line(phenotypeGenome(Marks.Leg.FOUR, Marks.Leg.EIGHT), Locus.LEGS).note());
	}

	@Test
	void eightBreedsTrueAndACrownOverNothingStepsDown() {
		assertEquals(Phenotype.LegShow.EIGHT, Expression.legs(Marks.Leg.EIGHT, Marks.Leg.EIGHT));
		assertEquals(Phenotype.RackShow.FULL, Expression.rack(Marks.Rack.NONE, Marks.Rack.CROWN));
		assertEquals(Phenotype.RackShow.CROWN, Expression.rack(Marks.Rack.CROWN, Marks.Rack.CROWN));
	}

	@Test
	void wingsComeInThroughASmallPair() {
		assertEquals(Phenotype.WingShow.PINION, Expression.wings(Marks.Wing.NONE, Marks.Wing.FULL));
		assertEquals(Phenotype.WingShow.FULL, Expression.wings(Marks.Wing.FULL, Marks.Wing.FULL));
		assertEquals(Phenotype.WingShow.ASTRAL_FULL, Expression.wings(Marks.Wing.FULL, Marks.Wing.ASTRAL));
		assertEquals(Phenotype.WingShow.ASTRAL, Expression.wings(Marks.Wing.ASTRAL, Marks.Wing.ASTRAL));
	}

	@Test
	void torsoBlendKeepsTheSocketAndAveragesTheNeck() {
		Genome cross = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.EIGHT),
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Leg.FOUR).with(Marks.Wing.FULL)
						.with(Marks.Foot.TALON),
				true,
				false,
				true);
		Phenotype phenotype = Expression.express(cross);
		float neck = (Marks.Species.STEED.neck + Marks.Species.BIRD.neck) * 0.5f;
		assertEquals(neck, phenotype.proportions.neck(), 1.0e-5f);
		// The showing body is the higher rank, and the hidden one reshapes it within limits.
		assertEquals(Marks.Torso.STEED, Expression.torso(Marks.Torso.SERPENT, Marks.Torso.STEED));
		assertEquals(Marks.Torso.BEAR, Expression.torso(Marks.Torso.STEED, Marks.Torso.BEAR));
		Phenotype steedOverSerpent = Expression.express(new Genome(Strand.wild(Marks.Torso.STEED, Marks.Head.STEED),
				Strand.wild(Marks.Torso.SERPENT, Marks.Head.SERPENT), true, true, true));
		assertEquals(Marks.Torso.STEED, steedOverSerpent.torso);
		assertEquals(Marks.Torso.SERPENT, steedOverSerpent.carriedTorso);
		assertEquals(1.30f, steedOverSerpent.shape.neck(), 1.0e-5f);
		assertEquals(1.60f, steedOverSerpent.shape.tail(), 1.0e-5f, "the serpent's tail is clamped");
		assertEquals(0.85f, steedOverSerpent.shape.girth(), 1.0e-5f);
		assertTrue(Expression.express(Founders.eightfold()).shape.pure());
		// Pelt: A over B over C.
		assertEquals(0, Expression.pelt(Marks.Pelt.C, Marks.Pelt.A));
		assertEquals(1, Expression.pelt(Marks.Pelt.B, Marks.Pelt.C));
		assertEquals(2, Expression.pelt(Marks.Pelt.C, Marks.Pelt.C));
		assertEquals(Marks.Head.STEED, phenotype.head);
		assertEquals(Marks.Head.BIRD, phenotype.carriedHead);
		assertEquals(Phenotype.LegShow.SPARE, phenotype.legs);
		assertEquals(Phenotype.WingShow.PINION, phenotype.wings);
		assertEquals(Phenotype.FootShow.TALON, phenotype.foot);
		assertEquals(Marks.Foot.HOOF, cross.carriedFoot());
		assertEquals(Reading.Note.SHAPED, Reading.of(cross).get(0).note());
	}

	@Test
	void foundersShowTheirLine() {
		Phenotype eight = Expression.express(Founders.eightfold());
		assertEquals(Phenotype.LegShow.EIGHT, eight.legs);
		assertEquals(Phenotype.GaitShow.SEA, eight.gait);
		assertEquals(Phenotype.WingShow.NONE, eight.wings);
		assertEquals(Phenotype.TailShow.PLUME, eight.tail);
		assertTrue(eight.realms.contains(Marks.Realm.LOWER));
		assertTrue(eight.giftWhole);
		assertTrue(eight.gifts.contains(Marks.Gift.ROAD));

		Phenotype hart = Expression.express(Founders.drumHart());
		assertEquals(Phenotype.RackShow.CROWN, hart.rack);
		assertFalse(hart.crownHeavy);
		assertEquals(Phenotype.BondShow.DRUM, hart.bond);
		assertEquals(Phenotype.TailShow.FLAG, hart.tail);
		assertTrue(hart.gifts.contains(Marks.Gift.CALL));
		assertTrue(hart.trails.contains(Marks.Trail.STAR));
		assertTrue(hart.wards.contains(Marks.Ward.LONGEVITY));
		assertFalse(hart.crownHeavy);

		Phenotype elk = Expression.express(Founders.elk());
		assertEquals(Phenotype.RackShow.CROWN, elk.rack);
		assertTrue(elk.crownHeavy);
		assertEquals(Phenotype.ScaleShow.GREATER, elk.scale);
		assertEquals(Phenotype.TailShow.FLAG, elk.tail);
		assertTrue(elk.giftWhole);
		assertTrue(elk.gifts.contains(Marks.Gift.BEARING));

		Phenotype crane = Expression.express(Founders.crane());
		assertEquals(Phenotype.WingShow.FULL, crane.wings);
		assertEquals(Phenotype.CoatShow.BONE, crane.coat);
		assertEquals(Phenotype.LegShow.TWO, crane.legs);
		assertEquals(Phenotype.FootShow.TALON, crane.foot);
		assertEquals(Marks.Head.BIRD, crane.head);
		assertEquals(Phenotype.TailShow.FAN, crane.tail);
		assertTrue(crane.gifts.contains(Marks.Gift.FERRY));
		assertTrue(crane.wards.contains(Marks.Ward.LONGEVITY));

		Phenotype nagual = Expression.express(Founders.nagual());
		assertEquals(Phenotype.BondShow.NAGUAL, nagual.bond);
		assertTrue(nagual.gifts.contains(Marks.Gift.SKIN));
		assertEquals(Phenotype.TailShow.LASH, nagual.tail);
		assertEquals(Phenotype.TailShow.LASH, Expression.express(Founders.barghest()).tail);
		assertEquals(Phenotype.TailShow.FAN, Expression.express(Founders.roc()).tail);
		assertEquals(Phenotype.WingShow.ASTRAL, Expression.express(Founders.roc()).wings);
		Phenotype barghest = Expression.express(Founders.barghest());
		assertEquals(Phenotype.SenseShow.SCENT, barghest.sense);
		assertTrue(barghest.gifts.contains(Marks.Gift.OMEN));
		assertTrue(barghest.wards.contains(Marks.Ward.GUARD));
		assertEquals(Phenotype.ScaleShow.GREATER, Expression.express(Founders.roc()).scale);
		assertEquals(1.48f, Expression.express(Founders.roc()).uniformScale, 1.0e-5f);
		assertEquals(MountSize.HORSE_HEIGHT, MountSize.height(Expression.express(Genome.homozygous(
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT).with(Marks.Scale.SLIGHT)))), 1.0e-5f);
		assertTrue(MountSize.height(Expression.express(Founders.shade())) >= MountSize.HORSE_HEIGHT);
		assertTrue(MountSize.height(Expression.express(Founders.roc())) > MountSize.height(Expression.express(Founders.shade())));
		assertTrue(Expression.express(Founders.roc()).realms.contains(Marks.Realm.SIDEWAYS));
		assertTrue(Expression.express(Founders.roc()).giftWhole);
		assertTrue(Expression.express(Founders.roc()).gifts.contains(Marks.Gift.DREAM));

		Phenotype shade = Expression.express(Founders.shade());
		assertTrue(shade.coatMasked);
		assertEquals(Phenotype.TailShow.LASH, shade.tail);
		assertFalse(shade.thin);
		assertTrue(shade.gifts.contains(Marks.Gift.VEIL));
		assertTrue(shade.wards.isEmpty());
	}

	@Test
	void ghostNagualWithoutAGuardIsThinAndBoneCoversRosette() {
		Strand strand = Strand.wild(Marks.Torso.CAT, Marks.Head.CAT)
				.with(Marks.Phase.GHOST)
				.with(Marks.Bond.NAGUAL);
		assertTrue(Expression.express(Genome.homozygous(strand)).thin);
		assertEquals(Phenotype.CoatShow.SOLID, Expression.coat(Marks.Coat.SOLID, Marks.Coat.ROSETTE));
		assertEquals(Phenotype.CoatShow.DUSK_ROSETTE, Expression.coat(Marks.Coat.DUSK, Marks.Coat.ROSETTE));
		assertEquals(Phenotype.GaitShow.MIST, Expression.gait(new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Gait.SEA),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Gait.CINDER),
				true,
				true,
				true)));
	}

	@Test
	void noCrossoverKeepsTheWholeStrand() {
		Strand left = Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.EIGHT);
		Strand right = Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Leg.FOUR).with(Marks.Wing.FULL);
		Genome parent = new Genome(left, right, true, true, true);
		Strand maternal = Meiosis.gamete(parent, always(true), 0);
		assertEquals(Marks.Torso.STEED, maternal.get(Locus.TORSO));
		assertEquals(Marks.Leg.EIGHT, maternal.get(Locus.LEGS));
		assertEquals(Marks.Head.STEED, maternal.get(Locus.HEAD));
		assertEquals(Marks.Wing.NONE, maternal.get(Locus.WINGS));
		assertEquals(Marks.Tail.NONE, maternal.get(Locus.TAIL));
		Strand paternal = Meiosis.gamete(parent, always(false), 0);
		assertEquals(Marks.Torso.BIRD, paternal.get(Locus.TORSO));
		assertEquals(Marks.Wing.FULL, paternal.get(Locus.WINGS));
	}

	@Test
	void neighboursStickAndTheShownHeadSplitsEvenly() {
		double p = 0.12;
		assertEquals(1.0, Odds.coupled(Locus.TORSO, Locus.TORSO, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.TORSO, Locus.HEAD, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.HEAD, Locus.FOOT, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.HEAD, Locus.WINGS, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.HEAD, Locus.TAIL, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.WINGS, Locus.TAIL, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.ROAD, Locus.HEAD, p), 1.0e-9);
		assertEquals(0.5, Odds.coupled(Locus.ROAD, Locus.BOND, p), 1.0e-9);
		assertEquals((1.0 + (1.0 - 2.0 * p)) / 2.0, Odds.coupled(Locus.ROAD, Locus.CALL, p), 1.0e-9);
		assertEquals((1.0 + (1.0 - 2.0 * p)) / 2.0, Odds.coupled(Locus.TORSO, Locus.LEGS, p), 1.0e-9);
		assertEquals(Odds.coupled(Locus.TORSO, Locus.LEGS, p), Odds.coupled(Locus.LEGS, Locus.FOOT, p), 1.0e-9);
		Map<Marks.Head, Double> shown = Odds.shownHead(Founders.eightfold(), Founders.crane());
		assertEquals(0.5, shown.get(Marks.Head.STEED), 1.0e-9);
		assertEquals(0.5, shown.get(Marks.Head.BIRD), 1.0e-9);
		double sum = 0;
		for (double value : Odds.gameteDistribution(Founders.eightfold(), Locus.LEGS, p).values()) {
			sum += value;
		}
		assertEquals(1.0, sum, 1.0e-9);
	}

	@Test
	void aBirdHeadCanStandOnHoovesAndTheCraneBreedsOnTwoFeet() {
		assertEquals(Phenotype.LegShow.TWO, Expression.legs(Marks.Leg.TWO, Marks.Leg.TWO));
		assertEquals(Phenotype.LegShow.HITCH, Expression.legs(Marks.Leg.TWO, Marks.Leg.FOUR));
		assertEquals(Phenotype.LegShow.SPARE, Expression.legs(Marks.Leg.TWO, Marks.Leg.EIGHT));
		Genome birdHeadOnHooves = new Genome(
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Leg.TWO).with(Marks.Foot.TALON),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Foot.HOOF),
				true,
				false,
				true);
		Phenotype shown = Expression.express(birdHeadOnHooves);
		assertEquals(Marks.Head.BIRD, shown.head);
		assertEquals(Phenotype.FootShow.HOOF, shown.foot);
		assertEquals(Marks.Foot.TALON, birdHeadOnHooves.carriedFoot());
		assertEquals(Phenotype.LegShow.HITCH, shown.legs);
		assertEquals(Phenotype.LegShow.TWO, Expression.express(Founders.crane()).legs);
		assertEquals(Phenotype.LegShow.TWO, Expression.express(Founders.roc()).legs);
	}

	@Test
	void wingsAndTailSplitFromTheHead() {
		Genome mix = new Genome(
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Leg.TWO).with(Marks.Foot.TALON)
						.with(Marks.Wing.FULL).with(Marks.Tail.FAN),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Leg.EIGHT).with(Marks.Foot.HOOF)
						.with(Marks.Tail.PLUME),
				true,
				false,
				false);
		Phenotype shown = Expression.express(mix);
		assertEquals(Marks.Head.BIRD, shown.head);
		assertEquals(Phenotype.FootShow.HOOF, shown.foot);
		assertEquals(Phenotype.WingShow.PINION, shown.wings);
		assertEquals(Phenotype.TailShow.PLUME, shown.tail);
		assertEquals(Marks.Tail.FAN, mix.carriedTail());
		assertEquals(Phenotype.LegShow.SPARE, shown.legs);
		assertEquals(Phenotype.TailShow.STUB, Expression.tail(Marks.Tail.NONE, Marks.Tail.PLUME, true));
	}

	@Test
	void giftsDoNotFollowTheBody() {
		Genome both = new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(Marks.Gift.ROAD),
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Gift.FERRY),
				true, true, true);
		Phenotype shown = Expression.express(both);
		assertTrue(shown.gifts.contains(Marks.Gift.ROAD));
		assertTrue(shown.gifts.contains(Marks.Gift.FERRY));
		assertFalse(shown.giftWhole);
		assertEquals(Reading.Note.PLAIN, line(both, Locus.ROAD).note());
		assertEquals(Reading.Note.PLAIN, line(both, Locus.FERRY).note());

		Genome glimpse = new Genome(
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD).with(Marks.Gift.DREAM),
				Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD),
				true, true, true);
		assertTrue(Expression.express(glimpse).gifts.contains(Marks.Gift.DREAM));
		assertFalse(Expression.express(glimpse).giftWhole);
		assertEquals(Reading.Note.INCOMPLETE, line(glimpse, Locus.DREAM).note());

		Genome hiddenSkin = new Genome(
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT).with(Marks.Gift.SKIN),
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT),
				true, true, true);
		assertTrue(Expression.express(hiddenSkin).gifts.isEmpty());
		assertEquals(Reading.Note.CARRIED, line(hiddenSkin, Locus.SKIN).note());

		Genome omenOverVeil = new Genome(
				Strand.wild(Marks.Torso.HOUND, Marks.Head.HOUND).with(Marks.Gift.OMEN),
				Strand.wild(Marks.Torso.CAT, Marks.Head.CAT).with(Marks.Gift.VEIL),
				true, true, true);
		Phenotype gate = Expression.express(omenOverVeil);
		assertTrue(gate.gifts.contains(Marks.Gift.OMEN));
		assertFalse(gate.gifts.contains(Marks.Gift.VEIL));
		assertEquals(Reading.Note.CARRIED, line(omenOverVeil, Locus.VEIL).note());
		assertEquals(Reading.Note.PLAIN, line(omenOverVeil, Locus.OMEN).note());
	}

	@Test
	void allAbilitiesKeepTheBredBodyAndTwoCompletesCanThrowAChimera() {
		Genome steed = Genome.homozygous(everyGift(Strand.wild(Marks.Torso.STEED, Marks.Head.STEED)
				.with(Marks.Leg.EIGHT).with(Marks.Tail.PLUME)));
		Genome bird = Genome.homozygous(everyGift(Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD)
				.with(Marks.Leg.TWO).with(Marks.Foot.TALON).with(Marks.Wing.FULL)));
		Phenotype stacked = Expression.express(steed);
		assertEquals(Marks.Gift.values().length - 1, stacked.gifts.size());
		assertTrue(Expression.complete(steed));
		assertFalse(stacked.chimera);
		assertEquals(Marks.Head.STEED, stacked.head);
		assertEquals(Phenotype.LegShow.EIGHT, stacked.legs);
		assertEquals(Phenotype.TailShow.PLUME, stacked.tail);
		assertFalse(Expression.complete(Founders.eightfold()));
		assertFalse(Expression.complete(new Genome(
				everyGift(Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD)),
				everyGift(Strand.wild(Marks.Torso.BIRD, Marks.Head.BIRD)).with(Marks.Off.of(Locus.DREAM)),
				true, true, true)));

		Random random = new Random(4);
		int chimeras = 0;
		boolean sawSteed = false;
		boolean sawBird = false;
		for (int i = 0; i < 200; i++) {
			Genome child = Meiosis.child(steed, bird, random);
			assertTrue(Expression.complete(child));
			Phenotype shown = Expression.express(child);
			if (child.chimera) {
				chimeras++;
				assertTrue(shown.chimera);
				assertEquals("chimera", line(child, Locus.TORSO).shown());
				assertEquals(Reading.Note.CHIMERA, line(child, Locus.HEAD).note());
			} else {
				assertFalse(shown.chimera);
				if (child.head() == Marks.Head.STEED) {
					sawSteed = true;
				}
				if (child.head() == Marks.Head.BIRD) {
					sawBird = true;
				}
			}
		}
		assertTrue(chimeras > 60 && chimeras < 140, "chimeras " + chimeras);
		assertTrue(sawSteed);
		assertTrue(sawBird);

		for (int i = 0; i < 40; i++) {
			assertFalse(Meiosis.child(steed, Founders.eightfold(), random).chimera);
		}
	}

	/**
	 * Five matings have six seats for parent lines. Eight races cannot all be in that foal,
	 * and a gift the parents do not carry cannot appear on it.
	 */
	@Test
	void fiveMatingsCannotGatherTheEightLines() {
		Genome[] founders = {
				Founders.eightfold(), Founders.drumHart(), Founders.elk(), Founders.crane(),
				Founders.nagual(), Founders.barghest(), Founders.roc(), Founders.shade(), Founders.bear(), Founders.serpent()
		};
		EnumSet<Marks.Gift> races = EnumSet.noneOf(Marks.Gift.class);
		for (Genome founder : founders) {
			EnumSet<Marks.Gift> gifts = carried(founder);
			assertEquals(1, gifts.size(), gifts.toString());
			assertFalse(Expression.complete(founder));
			assertTrue(races.add(gifts.iterator().next()));
		}
		assertEquals(Marks.Gift.values().length - 1, races.size());

		for (int seed = 0; seed < 200; seed++) {
			Random random = new Random(seed);
			Genome first = bred(founders[0], founders[1], random);
			Genome second = bred(founders[2], founders[3], random);
			Genome third = bred(founders[4], founders[5], random);
			Genome four = bred(first, second, random);
			Genome five = bred(four, third, random);
			assertFalse(Expression.complete(five));
			assertTrue(carried(five).size() <= 6, carried(five).toString());

			Genome line = founders[5];
			for (int mate = 0; mate < 5; mate++) {
				line = bred(line, founders[mate], random);
			}
			assertFalse(Expression.complete(line));
			assertTrue(carried(line).size() <= 6, carried(line).toString());
		}
	}

	private static Genome bred(Genome dam, Genome sire, Random random) {
		Genome child = Meiosis.child(dam, sire, random);
		assertFalse(child.chimera);
		EnumSet<Marks.Gift> allowed = carried(dam);
		allowed.addAll(carried(sire));
		assertTrue(allowed.containsAll(carried(child)), carried(child) + " from " + allowed);
		return child;
	}

	private static EnumSet<Marks.Gift> carried(Genome genome) {
		EnumSet<Marks.Gift> gifts = EnumSet.noneOf(Marks.Gift.class);
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift == Marks.Gift.NONE) {
				continue;
			}
			if (genome.maternal(gift.locus()) == gift || genome.paternal(gift.locus()) == gift) {
				gifts.add(gift);
			}
		}
		return gifts;
	}

	private static Strand everyGift(Strand strand) {
		Strand out = strand;
		for (Marks.Gift gift : Marks.Gift.values()) {
			if (gift != Marks.Gift.NONE) {
				out = out.with(gift);
			}
		}
		return out;
	}

	@Test
	void theBookWalksBackToAGrandparent() {
		HerdBook book = new HerdBook();
		HerdBook.Entry grand = book.record("Eightfold", true, Founders.eightfold(), null, null);
		HerdBook.Entry other = book.record("Crane", false, Founders.crane(), null, null);
		Genome foal = Meiosis.child(Founders.eightfold(), Founders.crane(), new Random(2));
		Expression.express(foal);
		HerdBook.Entry child = book.record("Cross", true, foal, grand.id(), other.id());
		assertTrue(book.forebears(child.id()).stream().anyMatch(entry -> entry.name().equals("Eightfold")));
		assertTrue(book.forebears(child.id()).stream().anyMatch(entry -> entry.name().equals("Crane")));
		assertEquals(2, book.forebears(child.id()).size());
	}

	private static Phenotype express(Marks.Leg maternal, Marks.Leg paternal) {
		return Expression.express(phenotypeGenome(maternal, paternal));
	}

	private static Genome phenotypeGenome(Marks.Leg maternal, Marks.Leg paternal) {
		return new Genome(
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(maternal),
				Strand.wild(Marks.Torso.STEED, Marks.Head.STEED).with(paternal),
				true,
				true,
				true);
	}

	private static Reading.Line line(Genome genome, Locus locus) {
		return Reading.of(genome).stream().filter(item -> item.locus() == locus).findFirst().orElseThrow();
	}

	private static Random always(boolean value) {
		return new Random() {
			@Override
			public boolean nextBoolean() {
				return value;
			}
		};
	}
}
