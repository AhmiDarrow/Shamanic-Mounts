package tk.darrow.shamanicmounts.tame;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.tack.SaddleRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TackTest {
	@Test
	void onlyATamedMountWithTheShamanicSaddleCanBeRidden() {
		assertFalse(SaddleRules.canRide(false, true));
		assertFalse(SaddleRules.canRide(true, false));
		assertTrue(SaddleRules.canRide(true, true));
		assertFalse(SaddleRules.vanillaSaddleFits());
		assertFalse(SaddleRules.foodStartsTrial());
		assertFalse(SaddleRules.canOffer(true, true, 1));
		assertTrue(SaddleRules.canOffer(true, true, 0));
		assertFalse(SaddleRules.canOffer(false, true, 0));
	}

	@Test
	void fourBracedJoltsTameAndAMissFails() {
		BraceTrial.Trial held = BraceTrial.start();
		for (int i = 0; i < 90; i++) {
			BraceTrial.tick(held, true, false, false);
		}
		assertTrue(held.done());
		assertEquals(BraceTrial.JOLTS, held.jolts());

		BraceTrial.Trial missed = BraceTrial.start();
		for (int i = 0; i < BraceTrial.WINDUP_TICKS - 1; i++) {
			BraceTrial.tick(missed, false, false, false);
		}
		BraceTrial.tick(missed, false, false, false);
		assertTrue(missed.failed());
		assertEquals(0, missed.jolts());

		BraceTrial.Trial fell = BraceTrial.start();
		BraceTrial.tick(fell, true, true, false);
		assertTrue(fell.failed());

		BraceTrial.Trial struck = BraceTrial.start();
		BraceTrial.tick(struck, true, false, true);
		assertTrue(struck.failed());
	}
}
