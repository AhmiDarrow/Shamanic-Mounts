package tk.darrow.shamanicmounts.ride;

import tk.darrow.shamanicmounts.entity.MountMode;

/**
 * Follow is the order a tame keeps until its owner says otherwise. Stepping off never changes it,
 * and a following mount near its owner crosses into another dimension with them.
 */
public final class FollowRules {
	/** The order a mount is given when it is tamed, and the one an unknown or missing order falls back to. */
	public static final MountMode DEFAULT = MountMode.FOLLOW;
	/**
	 * The save format of the order. Version 0 is every save before follow became the default: then a new tame
	 * was set to wander and a guarding mount was set to stay whenever its rider stepped off.
	 */
	public static final int ORDERS_VERSION = 1;
	/** How near its owner, in blocks, a following mount must be when the owner leaves a dimension. */
	public static final double CROSSING_RANGE = 24.0;
	/** The most following mounts that cross with one owner at once, nearest first. */
	public static final int CROSSING_CAP = 8;
	/** How long, in ticks, mounts wait to cross while their owner is between dimensions, as on the end credits. */
	public static final int CROSSING_WAIT_TICKS = 1200;
	/**
	 * A teleport steps its rider off before moving them. A rider stepped off this many ticks before they left
	 * the dimension was riding when they went, and is put back in the saddle on arrival.
	 */
	public static final int RIDER_GRACE_TICKS = 1;
	/** How far from the owner, in blocks, a crossing mount may be set down. */
	public static final int LANDING_REACH = 4;

	private FollowRules() {
	}

	/**
	 * The order a mount loads with.
	 *
	 * @param saved   the saved order's name, or null when the save has none
	 * @param version the saved {@link #ORDERS_VERSION}, 0 when absent
	 * @param sitting whether the save has it sitting
	 * @param guard   whether it carries the guard ward
	 */
	public static MountMode loaded(String saved, int version, boolean sitting, boolean guard) {
		if (saved == null || saved.isEmpty()) {
			return sitting ? MountMode.STAY : DEFAULT;
		}
		MountMode mode;
		try {
			mode = MountMode.valueOf(saved);
		} catch (IllegalArgumentException unknown) {
			return DEFAULT;
		}
		if (version >= ORDERS_VERSION) {
			return mode;
		}
		// Older saves: wander was what every new tame got, and a guarding mount's stay was what stepping
		// off gave it. Neither can be told apart from an order the owner chose, so both become follow.
		// A stay on any other mount was only ever an order, and it is kept.
		if (mode == MountMode.WANDER || mode == MountMode.STAY && guard) {
			return MountMode.FOLLOW;
		}
		return mode;
	}

	/**
	 * Whether a mount goes along when its owner leaves the dimension.
	 *
	 * @param otherRider anyone but the owner is on it
	 * @param distanceSqr squared distance from the owner as they left
	 */
	public static boolean crosses(boolean tame, boolean owned, MountMode mode, boolean sitting, boolean leashed,
			boolean passenger, boolean otherRider, boolean away, double distanceSqr) {
		return tame && owned && mode == MountMode.FOLLOW && !sitting && !leashed && !passenger && !otherRider && !away
				&& distanceSqr <= CROSSING_RANGE * CROSSING_RANGE;
	}

	/** Whether a rider stepped off at {@code dismountTick} was riding when they left at {@code leftTick}. */
	public static boolean wasRiding(long dismountTick, long leftTick) {
		return dismountTick >= 0 && leftTick >= dismountTick && leftTick - dismountTick <= RIDER_GRACE_TICKS;
	}
}
