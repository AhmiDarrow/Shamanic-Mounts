package tk.darrow.shamanicmounts.tame;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import tk.darrow.shamanicmounts.tack.SaddleRules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TackTest {
	@Test
	void calmThenSaddleThenMount() {
		assertFalse(SaddleRules.canRide(false, true));
		assertFalse(SaddleRules.canRide(true, false));
		assertTrue(SaddleRules.canRide(true, true));
		assertFalse(SaddleRules.vanillaSaddleFits());
		assertFalse(SaddleRules.foodStartsTrial());
		assertEquals(20 * 120, SaddleRules.CALM_TICKS);

		assertTrue(SaddleRules.hostile(false, false, 0, false));
		assertFalse(SaddleRules.hostile(true, false, 0, false));
		assertFalse(SaddleRules.hostile(false, true, 0, false));
		assertFalse(SaddleRules.hostile(false, false, 1, false));
		assertFalse(SaddleRules.hostile(false, false, 0, true));

		assertTrue(SaddleRules.canCalm(true, true, false));
		assertFalse(SaddleRules.canCalm(false, true, false));
		assertFalse(SaddleRules.canCalm(true, false, false));
		assertFalse(SaddleRules.canCalm(true, true, true));

		assertTrue(SaddleRules.canPlaceSaddle(true, true, true, false, false));
		assertFalse(SaddleRules.canPlaceSaddle(false, true, true, false, false));
		assertFalse(SaddleRules.canPlaceSaddle(true, false, true, false, false));
		assertFalse(SaddleRules.canPlaceSaddle(true, true, false, false, false));
		assertFalse(SaddleRules.canPlaceSaddle(true, true, true, true, false));
		assertFalse(SaddleRules.canPlaceSaddle(true, true, true, false, true));

		assertTrue(SaddleRules.canMount(true, true, true, 0, false, false));
		assertFalse(SaddleRules.canMount(false, true, true, 0, false, false));
		assertFalse(SaddleRules.canMount(true, false, true, 0, false, false));
		assertFalse(SaddleRules.canMount(true, true, false, 0, false, false));
		assertFalse(SaddleRules.canMount(true, true, true, 1, false, false));
		assertFalse(SaddleRules.canMount(true, true, true, 0, true, false));
		assertFalse(SaddleRules.canMount(true, true, true, 0, false, true));
	}

	@Test
	void everyPromptAnsweredTamesWithNoMisses() {
		ReinTrial.Trial trial = ReinTrial.start(2L);
		int all = ReinTrial.PRESS_LEFT | ReinTrial.PRESS_RIGHT | ReinTrial.PRESS_FORWARD | ReinTrial.PRESS_BACK;
		ReinTrial.Press press = ReinTrial.Press.of(all);
		for (int tick = 0; tick < ReinTrial.DURATION_TICKS; tick++) {
			ReinTrial.tick(trial, press, false);
		}
		assertTrue(trial.done());
		assertFalse(trial.failed());
		assertEquals(0, trial.misses());
	}

	@Test
	void fiveEmptyBeatsFailAndFourCanStillBeFinished() {
		ReinTrial.Trial failed = ReinTrial.start(3L);
		ReinTrial.Press none = ReinTrial.Press.of(0);
		for (int tick = 0; tick < 5 * ReinTrial.BEAT_TICKS; tick++) {
			ReinTrial.tick(failed, none, false);
		}
		assertTrue(failed.failed());
		assertEquals(5, failed.misses());

		ReinTrial.Trial saved = ReinTrial.start(4L);
		for (int tick = 0; tick < 4 * ReinTrial.BEAT_TICKS; tick++) {
			ReinTrial.tick(saved, none, false);
		}
		assertFalse(saved.failed());
		assertEquals(4, saved.misses());
		for (int tick = 4 * ReinTrial.BEAT_TICKS; tick < ReinTrial.DURATION_TICKS; tick++) {
			ReinTrial.tick(saved, ReinTrial.Press.of(bit(saved.cue())), false);
		}
		assertTrue(saved.done());
		assertFalse(saved.failed());
		assertEquals(4, saved.misses());
	}

	@Test
	void gettingOffFailsAtOnce() {
		ReinTrial.Trial trial = ReinTrial.start(5L);
		ReinTrial.tick(trial, ReinTrial.Press.of(bit(trial.cue())), true);
		assertTrue(trial.failed());
		assertFalse(trial.done());
	}

	@Test
	void aLateCorrectPressCatchesAndAWrongBeatIsOneMiss() {
		ReinTrial.Trial caught = ReinTrial.start(6L);
		ReinTrial.Press wrong = ReinTrial.Press.of(other(caught.cue()));
		for (int tick = 0; tick < ReinTrial.BEAT_TICKS - 1; tick++) {
			ReinTrial.tick(caught, wrong, false);
		}
		assertFalse(caught.missedBeat());
		assertEquals(0, caught.misses());
		ReinTrial.tick(caught, ReinTrial.Press.of(bit(caught.cue())), false);
		assertTrue(caught.caughtBeat());
		assertEquals(0, caught.misses());
		assertFalse(caught.failed());

		ReinTrial.Trial missed = ReinTrial.start(7L);
		ReinTrial.Press onlyWrong = ReinTrial.Press.of(other(missed.cue()));
		for (int tick = 0; tick < ReinTrial.BEAT_TICKS; tick++) {
			ReinTrial.tick(missed, onlyWrong, false);
		}
		assertTrue(missed.missedBeat());
		assertEquals(1, missed.misses());
		assertFalse(missed.failed());
	}

	@Test
	void askedDirectionStepsTheOtherWay() {
		assertEquals(-ReinTrial.LUNGE_STRAFE, ReinTrial.Dir.LEFT.strafe(), 0.0001f);
		assertEquals(0.0f, ReinTrial.Dir.LEFT.forward(), 0.0001f);
		assertEquals(ReinTrial.LUNGE_STRAFE, ReinTrial.Dir.RIGHT.strafe(), 0.0001f);
		assertEquals(0.0f, ReinTrial.Dir.RIGHT.forward(), 0.0001f);
		assertEquals(0.0f, ReinTrial.Dir.FORWARD.strafe(), 0.0001f);
		assertEquals(-ReinTrial.LUNGE_FORWARD, ReinTrial.Dir.FORWARD.forward(), 0.0001f);
		assertEquals(0.0f, ReinTrial.Dir.BACK.strafe(), 0.0001f);
		assertEquals(ReinTrial.LUNGE_FORWARD, ReinTrial.Dir.BACK.forward(), 0.0001f);
		assertTrue(ReinTrial.buck(0) > ReinTrial.buck(10));
		assertTrue(ReinTrial.buck(10) > ReinTrial.buck(20));
		assertTrue(ReinTrial.buck(30) > 0.0f);
		assertTrue(ReinTrial.hops(0));
		assertFalse(ReinTrial.hops(1));
	}

	@Test
	void fifteenPromptsOpenWithEveryDirectionAndNeverRepeat() {
		ReinTrial.Trial trial = ReinTrial.start(1L);
		assertEquals(15, ReinTrial.BEATS);
		assertEquals(600, ReinTrial.DURATION_TICKS);
		EnumSet<ReinTrial.Dir> opening = EnumSet.noneOf(ReinTrial.Dir.class);
		ReinTrial.Dir previous = null;
		for (int beat = 0; beat < ReinTrial.BEATS; beat++) {
			ReinTrial.Dir cue = trial.cueAt(beat);
			if (beat < 4) {
				opening.add(cue);
			}
			if (previous != null) {
				assertNotEquals(previous, cue);
			}
			previous = cue;
		}
		assertEquals(4, opening.size());
	}

	private static int bit(ReinTrial.Dir dir) {
		return switch (dir) {
			case LEFT -> ReinTrial.PRESS_LEFT;
			case RIGHT -> ReinTrial.PRESS_RIGHT;
			case FORWARD -> ReinTrial.PRESS_FORWARD;
			case BACK -> ReinTrial.PRESS_BACK;
		};
	}

	private static int other(ReinTrial.Dir dir) {
		return switch (dir) {
			case LEFT -> ReinTrial.PRESS_RIGHT;
			case RIGHT -> ReinTrial.PRESS_LEFT;
			case FORWARD -> ReinTrial.PRESS_BACK;
			case BACK -> ReinTrial.PRESS_FORWARD;
		};
	}
}
