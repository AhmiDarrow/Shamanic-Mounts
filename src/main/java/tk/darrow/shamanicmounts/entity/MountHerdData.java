package tk.darrow.shamanicmounts.entity;

import java.util.UUID;

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

	public static MountHerdData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
	}

	private static MountHerdData load(CompoundTag tag, HolderLookup.Provider registries) {
		MountHerdData data = new MountHerdData();
		HerdBook loaded = HerdIO.read(tag.getList("Entries", Tag.TAG_COMPOUND));
		for (HerdBook.Entry entry : loaded.entries()) {
			data.book.load(entry);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.put("Entries", HerdIO.write(book));
		return tag;
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
		HerdBook.Entry entry = book.adopt(id, owner, name, male, genome, dam, sire, pelt);
		setDirty();
		return entry;
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

	public ListTag writePlayer(UUID player) {
		return HerdIO.write(HerdIO.forPlayer(book, player));
	}
}
