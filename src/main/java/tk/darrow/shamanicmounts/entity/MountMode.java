package tk.darrow.shamanicmounts.entity;

/** What a tame mount does while nobody rides it. Chosen from the saddle bags screen. */
public enum MountMode {
	/** Keeps up with its owner, and teleports after them if they get far ahead. */
	FOLLOW,
	/** Sits where it was left. */
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
}
