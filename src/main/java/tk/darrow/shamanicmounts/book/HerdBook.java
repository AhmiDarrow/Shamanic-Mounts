package tk.darrow.shamanicmounts.book;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tk.darrow.shamanicmounts.genome.Genome;

/**
 * The mounts one keeper can open in the book, including animals whose chunk is not loaded.
 * Parent ids are the family tree.
 */
public final class HerdBook {
	/** One mount. {@code pelt} is which of its line's three coats it wears, 0 to 2. */
	public record Entry(UUID id, UUID owner, String name, boolean male, boolean tame, Genome genome, UUID dam,
			UUID sire, int pelt) {
		public Entry(UUID id, UUID owner, String name, boolean male, boolean tame, Genome genome, UUID dam, UUID sire) {
			this(id, owner, name, male, tame, genome, dam, sire, 0);
		}
	}

	private final Map<UUID, Entry> byId = new LinkedHashMap<>();

	public Entry record(String name, boolean male, Genome genome, UUID dam, UUID sire) {
		return keep(null, name, male, genome, dam, sire);
	}

	/** A tame that belongs to one player, loaded or not. */
	public Entry keep(UUID owner, String name, boolean male, Genome genome, UUID dam, UUID sire) {
		return adopt(UUID.randomUUID(), owner, name, male, genome, dam, sire, 0);
	}

	public Entry keep(UUID owner, String name, boolean male, Genome genome, UUID dam, UUID sire, int pelt) {
		return adopt(UUID.randomUUID(), owner, name, male, genome, dam, sire, pelt);
	}

	/** The entity's own id, so the book and the animal stay the same mount. */
	public Entry adopt(UUID id, UUID owner, String name, boolean male, Genome genome, UUID dam, UUID sire, int pelt) {
		String clean = name == null ? "" : name.strip();
		if (clean.isEmpty()) {
			clean = "Tame";
		}
		if (clean.length() > 24) {
			clean = clean.substring(0, 24).strip();
		}
		Entry entry = new Entry(id, owner, clean, male, true, genome, dam, sire, pelt);
		byId.put(id, entry);
		return entry;
	}

	public void load(Entry entry) {
		byId.put(entry.id(), entry);
	}

	public List<Entry> entries() {
		return List.copyOf(byId.values());
	}

	public List<Entry> tames(UUID owner) {
		ArrayList<Entry> out = new ArrayList<>();
		if (owner == null) {
			return out;
		}
		for (Entry entry : byId.values()) {
			if (entry.tame() && owner.equals(entry.owner())) {
				out.add(entry);
			}
		}
		return out;
	}

	public List<Entry> foals(UUID id) {
		ArrayList<Entry> out = new ArrayList<>();
		for (Entry entry : byId.values()) {
			if (id.equals(entry.dam()) || id.equals(entry.sire())) {
				out.add(entry);
			}
		}
		return out;
	}

	/** Only the owner of a current tame can rename it. Blank names are refused. Longer names cut at 24. */
	public boolean rename(UUID player, UUID id, String name) {
		Entry entry = ownedTame(player, id);
		if (entry == null || name == null) {
			return false;
		}
		String clean = name.strip();
		if (clean.length() > 24) {
			clean = clean.substring(0, 24).strip();
		}
		if (clean.isEmpty()) {
			return false;
		}
		byId.put(id, new Entry(entry.id(), entry.owner(), clean, entry.male(), true, entry.genome(), entry.dam(),
				entry.sire(), entry.pelt()));
		return true;
	}

	/**
	 * Release drops the mount off the tames page. The record stays so a foal's family line can still name it.
	 */
	public boolean release(UUID player, UUID id) {
		Entry entry = ownedTame(player, id);
		if (entry == null) {
			return false;
		}
		byId.put(id, new Entry(entry.id(), entry.owner(), entry.name(), entry.male(), false, entry.genome(),
				entry.dam(), entry.sire(), entry.pelt()));
		return true;
	}

	private Entry ownedTame(UUID player, UUID id) {
		Entry entry = byId.get(id);
		if (entry == null || player == null || !entry.tame() || !player.equals(entry.owner())) {
			return null;
		}
		return entry;
	}

	public Entry get(UUID id) {
		return byId.get(id);
	}

	/** Dam, sire, then their parents, each mount once. */
	public List<Entry> forebears(UUID id) {
		Entry start = byId.get(id);
		ArrayList<Entry> out = new ArrayList<>();
		if (start == null) {
			return out;
		}
		ArrayDeque<UUID> next = new ArrayDeque<>();
		if (start.dam() != null) {
			next.add(start.dam());
		}
		if (start.sire() != null) {
			next.add(start.sire());
		}
		HashSet<UUID> seen = new HashSet<>();
		while (!next.isEmpty()) {
			UUID parent = next.removeFirst();
			if (!seen.add(parent)) {
				continue;
			}
			Entry entry = byId.get(parent);
			if (entry == null) {
				continue;
			}
			out.add(entry);
			if (entry.dam() != null) {
				next.add(entry.dam());
			}
			if (entry.sire() != null) {
				next.add(entry.sire());
			}
		}
		return out;
	}
}
