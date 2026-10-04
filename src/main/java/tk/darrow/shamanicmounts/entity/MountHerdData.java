package tk.darrow.shamanicmounts.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.book.HerdIO;
import tk.darrow.shamanicmounts.genome.Genome;

/** Every tame, kept with the overworld so the herd book works while the animal is unloaded. */
public final class MountHerdData extends SavedData {
	private static final String NAME = "shamanicmounts_herd";
	private static final Factory<MountHerdData> FACTORY = new Factory<>(MountHerdData::new, MountHerdData::load, null);

	private final HerdBook book = new HerdBook();
	/** Server-only. Not part of a herd-book entry, and not sent on the herd packet. */
	private final Map<UUID, Where> places = new LinkedHashMap<>();

	/**
	 * Last place a tame was saved, so a flute can load that chunk, and its order then ({@link MountMode} ordinal;
	 * {@link #UNKNOWN_MODE} for places saved before the order was kept), so a flute can leave a parked mount unloaded.
	 */
	public record Where(String dim, double x, double y, double z, int mode) {
	}

	public static final int UNKNOWN_MODE = -1;

	public static MountHerdData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
	}

	private static MountHerdData load(CompoundTag tag, HolderLookup.Provider registries) {
		MountHerdData data = new MountHerdData();
		HerdBook loaded = HerdIO.read(tag.getList("Entries", Tag.TAG_COMPOUND));
		for (HerdBook.Entry entry : loaded.entries()) {
			data.book.load(entry);
		}
		for (Tag id : tag.getList("ChimeraBred", Tag.TAG_INT_ARRAY)) {
			data.book.bredChimera(net.minecraft.nbt.NbtUtils.loadUUID(id));
		}
		if (tag.contains("Where", Tag.TAG_LIST)) {
			ListTag places = tag.getList("Where", Tag.TAG_COMPOUND);
			for (int i = 0; i < places.size(); i++) {
				CompoundTag one = places.getCompound(i);
				if (!one.hasUUID("Id")) {
					continue;
				}
				String dim = one.getString("Dim");
				if (dim.isEmpty()) {
					continue;
				}
				data.places.put(one.getUUID("Id"), new Where(dim, one.getDouble("X"), one.getDouble("Y"), one.getDouble("Z"),
						one.contains("Mode") ? one.getInt("Mode") : UNKNOWN_MODE));
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.put("Entries", HerdIO.write(book));
		ListTag bred = new ListTag();
		for (UUID keeper : book.chimeraBreeders()) {
			bred.add(net.minecraft.nbt.NbtUtils.createUUID(keeper));
		}
		tag.put("ChimeraBred", bred);
		ListTag places = new ListTag();
		for (Map.Entry<UUID, Where> entry : this.places.entrySet()) {
			CompoundTag one = new CompoundTag();
			Where at = entry.getValue();
			one.putUUID("Id", entry.getKey());
			one.putString("Dim", at.dim());
			one.putDouble("X", at.x());
			one.putDouble("Y", at.y());
			one.putDouble("Z", at.z());
			one.putInt("Mode", at.mode());
			places.add(one);
		}
		tag.put("Where", places);
		return tag;
	}

	/** Remember where this tame is. Dirty only when the recorded place changes. */
	public void note(UUID id, String dim, double x, double y, double z, MountMode mode) {
		if (id == null || dim == null || dim.isEmpty()) {
			return;
		}
		Where next = new Where(dim, x, y, z, mode == null ? UNKNOWN_MODE : mode.ordinal());
		if (next.equals(places.get(id))) {
			return;
		}
		places.put(id, next);
		setDirty();
	}

	@Nullable
	public Where where(UUID id) {
		return id == null ? null : places.get(id);
	}

	public HerdBook book() {
		return book;
	}

	/** How many tames a player has in the book, for numbering the next foal. */
	public int count(UUID owner) {
		return book.tames(owner).size();
	}

	public HerdBook.Entry adopt(UUID id, UUID owner, String name, boolean male, Genome genome, UUID dam, UUID sire,
			int pelt) {
		return adopt(id, owner, name, male, genome, dam, sire, pelt, tk.darrow.shamanicmounts.ride.MountStats.MISSING,
				tk.darrow.shamanicmounts.ride.MountStats.MISSING, tk.darrow.shamanicmounts.ride.MountStats.MISSING,
				tk.darrow.shamanicmounts.ride.MountStats.MISSING);
	}

	public HerdBook.Entry adopt(UUID id, UUID owner, String name, boolean male, Genome genome, UUID dam, UUID sire,
			int pelt, int health, int speed, int jump, int stamina) {
		HerdBook.Entry entry = book.adopt(id, owner, name, male, genome, dam, sire, pelt, health, speed, jump, stamina);
		setDirty();
		return entry;
	}

	/** The tame record moves to the new owner. Pedigree stays. */
	public boolean give(UUID id, UUID newOwner) {
		boolean ok = book.give(id, newOwner);
		if (ok) {
			setDirty();
		}
		return ok;
	}

	public boolean rename(UUID player, UUID id, String name) {
		boolean ok = book.rename(player, id, name);
		if (ok) {
			setDirty();
		}
		return ok;
	}

	public boolean release(UUID player, UUID id) {
		boolean ok = book.release(player, id);
		if (ok) {
			setDirty();
		}
		return ok;
	}

	/** A chimera was born to this keeper's pair. From now on the book lists it for them. */
	public void bredChimera(UUID keeper) {
		if (book.bredChimera(keeper)) {
			setDirty();
		}
	}

	public boolean hasBredChimera(UUID keeper) {
		return book.hasBredChimera(keeper);
	}

	public ListTag writePlayer(UUID player) {
		return HerdIO.write(HerdIO.forPlayer(book, player));
	}
}
