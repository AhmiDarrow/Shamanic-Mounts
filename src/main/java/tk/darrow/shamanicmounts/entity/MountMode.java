package tk.darrow.shamanicmounts.entity;

/**
 * What a tame mount does while nobody rides it. Chosen from the saddle bags screen. A new tame follows,
 * and stepping off never changes the order.
 */
public enum MountMode {
	/** Keeps up with its owner, teleports after them if they get far ahead, and goes with them into other dimensions. */
	FOLLOW,
	/** Lies down where it was left, and lies down again wherever its rider steps off. */
	STAY,
	/** Roams near where it was left. */
	WANDER;

	public static MountMode of(int ordinal) {
		MountMode[] all = values();
		return all[Math.floorMod(ordinal, all.length)];
	}

	public String key() {
		return "mount.shamanicmounts." + name().toLowerCase(java.util.Locale.ROOT);
	}

	/** The action-bar line confirming this order, with the mount's name. */
	public String orderKey() {
		return "mount.shamanicmounts.ordered." + name().toLowerCase(java.util.Locale.ROOT);
	}
}
