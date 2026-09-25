package tk.darrow.shamanicmounts.genome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MountSizeTest {
	@Test
	void everyFounderHasItsOwnForm() {
		assertEquals(MountSize.Form.STEED, MountSize.form(Expression.express(Founders.eightfold())));
		assertEquals(MountSize.Form.HART, MountSize.form(Expression.express(Founders.drumHart())));
		assertEquals(MountSize.Form.ELK, MountSize.form(Expression.express(Founders.elk())));
		assertEquals(MountSize.Form.CRANE, MountSize.form(Expression.express(Founders.crane())));
		assertEquals(MountSize.Form.NAGUAL, MountSize.form(Expression.express(Founders.nagual())));
		assertEquals(MountSize.Form.BARGHEST, MountSize.form(Expression.express(Founders.barghest())));
		assertEquals(MountSize.Form.ROC, MountSize.form(Expression.express(Founders.roc())));
		assertEquals(MountSize.Form.SHADE, MountSize.form(Expression.express(Founders.shade())));
		assertEquals(MountSize.Form.CHIMERA, MountSize.form(Expression.express(Founders.eightfold().asChimera())));
	}

	@Test
	void theSeatSitsOnTheSaddleInsideTheHitbox() {
		for (Genome genome : new Genome[] { Founders.eightfold(), Founders.elk(), Founders.crane(), Founders.roc(),
				Founders.shade(), Founders.eightfold().asChimera() }) {
			Phenotype phenotype = Expression.express(genome);
			float[] first = MountSize.seat(phenotype, 0);
			float[] second = MountSize.seat(phenotype, 1);
			float height = MountSize.height(phenotype);
			// The crane's back stands above a horse's poll, so the seat may rise past the hitbox a little.
			assertTrue(first[1] > height * 0.6f && first[1] < height * 1.2f, genome + " seat height " + first[1]);
			assertTrue(Math.abs(first[0]) < MountSize.width(phenotype) * 0.5f, genome + " seat along " + first[0]);
			assertTrue(second[0] > first[0], "the second seat is behind the first");
		}
	}

	@Test
	void theRigIsSlidForwardByTheBodyCentre() {
		Phenotype steed = Expression.express(Founders.eightfold());
		assertEquals(MountSize.Form.STEED.centre / 16f * steed.uniformScale, MountSize.centreOffset(steed), 1.0e-6f);
	}
}
