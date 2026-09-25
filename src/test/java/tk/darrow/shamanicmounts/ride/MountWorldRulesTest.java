package tk.darrow.shamanicmounts.ride;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.GenomeIO;
import tk.darrow.shamanicmounts.genome.Phenotype;

class MountWorldRulesTest {
	@Test
	void genomesRoundTrip() {
		for (Genome genome : new Genome[] { Founders.eightfold(), Founders.drumHart(), Founders.elk(), Founders.crane(),
				Founders.nagual(), Founders.barghest(), Founders.roc(), Founders.shade(),
				Founders.eightfold().asChimera() }) {
			Genome read = GenomeIO.read(GenomeIO.write(genome));
			assertEquals(genome.chimera, read.chimera);
			assertEquals(genome.head(), read.head());
			assertEquals(genome.foot(), read.foot());
			assertEquals(genome.tail(), read.tail());
			assertEquals(Expression.express(genome).gifts, Expression.express(read).gifts);
		}
	}

	@Test
	void breedingNeedsTheDiamondAppleAndARestAfterwards() {
		assertTrue(BreedingRules.canFeed(true, false, true, 0, true, false));
		assertFalse(BreedingRules.canFeed(true, false, true, 0, false, false));
		assertFalse(BreedingRules.canFeed(true, true, true, 0, true, false));
		assertFalse(BreedingRules.canFeed(false, false, true, 0, true, false));
		assertFalse(BreedingRules.canFeed(true, false, true, 1, true, false));
		assertTrue(BreedingRules.canFeed(true, false, true, BreedingRules.REST_TICKS, true, true));
		UUID owner = UUID.randomUUID();
		assertTrue(BreedingRules.canPair(owner, owner, 9));
		assertFalse(BreedingRules.canPair(owner, UUID.randomUUID(), 1));
		assertFalse(BreedingRules.canPair(owner, owner, 80 * 80));
	}

	@Test
	void giftsMatchTheSwitches() {
		Phenotype eight = Expression.express(Founders.eightfold());
		assertTrue(GiftRules.waterWalk(eight));
		assertTrue(GiftRules.fullStep(eight));
		assertFalse(GiftRules.ram(eight));

		Phenotype elk = Expression.express(Founders.elk());
		assertTrue(GiftRules.chest(elk));
		assertTrue(GiftRules.ram(elk));
		assertFalse(GiftRules.drum(elk));

		Phenotype crane = Expression.express(Founders.crane());
		assertTrue(GiftRules.secondSeat(crane));
		assertTrue(GiftRules.glide(crane));
		assertTrue(GiftRules.longevity(crane));

		Phenotype nagual = Expression.express(Founders.nagual());
		assertTrue(GiftRules.nagual(nagual));

		Phenotype barghest = Expression.express(Founders.barghest());
		assertEquals(GiftRules.GLOW_FAR, GiftRules.glowRange(barghest));
		assertTrue(GiftRules.guard(barghest));
		assertTrue(GiftRules.shadowSpeed(barghest));

		Phenotype roc = Expression.express(Founders.roc());
		assertTrue(GiftRules.canClimb(roc, Founders.roc(), false));
		assertFalse(GiftRules.climbFeedsHunger(Founders.roc()));

		Phenotype shade = Expression.express(Founders.shade());
		assertTrue(GiftRules.sneakHide(shade));
		assertTrue(GiftRules.blink(shade));

		assertEquals(1.5f, GiftRules.halvedExhaustion(1.0f, 2.0f));
	}
}
