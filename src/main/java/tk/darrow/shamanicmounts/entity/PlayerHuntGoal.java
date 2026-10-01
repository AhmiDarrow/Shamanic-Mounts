package tk.darrow.shamanicmounts.entity;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** A wild adult picks the nearest player it can see and closes to bite. Calm, a foal, or a trial stops it. */
final class PlayerHuntGoal extends Goal {
	/** Close enough to matter when you walk up, not across a field. */
	static final double RANGE = 16.0;

	private final ShamanicMount mount;
	private Player wanted;
	private int cooldown;

	PlayerHuntGoal(ShamanicMount mount) {
		this.mount = mount;
		this.setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (!mount.huntsPlayers()) {
			return false;
		}
		if (cooldown > 0) {
			cooldown--;
			return false;
		}
		cooldown = 10;
		Player player = mount.level().getNearestPlayer(mount, RANGE);
		if (!worth(player) || !mount.getSensing().hasLineOfSight(player)) {
			return false;
		}
		wanted = player;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return mount.huntsPlayers() && worth(wanted) && mount.distanceToSqr(wanted) <= RANGE * RANGE;
	}

	@Override
	public void start() {
		mount.setTarget(wanted);
	}

	@Override
	public void stop() {
		if (mount.getTarget() == wanted) {
			mount.setTarget(null);
		}
		wanted = null;
		cooldown = 20;
	}

	private static boolean worth(LivingEntity player) {
		return player instanceof Player rider && rider.isAlive() && !rider.isSpectator() && !rider.isCreative();
	}
}
