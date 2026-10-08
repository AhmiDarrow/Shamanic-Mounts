package tk.darrow.shamanicmounts.tame;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.tack.FeedRules;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedRulesTest {
	@Test
	void meatMendsAHurtTameByItsNutrition() {
		// cooked beef, 8 nutrition, on a mount down 20
		assertEquals(8.0f, FeedRules.mealHeal(true, true, 20.0f, 40.0f, 8));
		// raw beef, 3
		assertEquals(3.0f, FeedRules.mealHeal(true, true, 20.0f, 40.0f, 3));
		// a cut with no food value still mends the minimum
		assertEquals(FeedRules.MIN_HEAL, FeedRules.mealHeal(true, true, 20.0f, 40.0f, 0));
		// never past full
		assertEquals(1.5f, FeedRules.mealHeal(true, true, 38.5f, 40.0f, 8));
	}

	@Test
	void whoLeavesTheMeal() {
		// a sound mount: the meal stays in hand and the click rides
		assertEquals(0.0f, FeedRules.mealHeal(true, true, 40.0f, 40.0f, 8));
		// a wild mount is not fed into tameness
		assertEquals(0.0f, FeedRules.mealHeal(false, true, 20.0f, 40.0f, 8));
		// bread is not a meal for a spirit mount
		assertEquals(0.0f, FeedRules.mealHeal(true, false, 20.0f, 40.0f, 5));
	}
}
