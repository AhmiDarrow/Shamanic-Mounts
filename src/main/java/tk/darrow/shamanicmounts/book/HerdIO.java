package tk.darrow.shamanicmounts.book;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import tk.darrow.shamanicmounts.genome.GenomeIO;

/** Herd book entries on disk and on the wire. */
public final class HerdIO {
	private HerdIO() {
	}

	public static ListTag write(HerdBook book) {
		ListTag list = new ListTag();
		for (HerdBook.Entry entry : book.entries()) {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", entry.id());
			if (entry.owner() != null) {
				tag.putUUID("Owner", entry.owner());
			}
			tag.putString("Name", entry.name());
			tag.putBoolean("Male", entry.male());
			tag.putBoolean("Tame", entry.tame());
			tag.put("Genome", GenomeIO.write(entry.genome()));
			tag.putInt("Pelt", entry.pelt());
			if (entry.dam() != null) {
				tag.putUUID("Dam", entry.dam());
			}
			if (entry.sire() != null) {
				tag.putUUID("Sire", entry.sire());
			}
			list.add(tag);
		}
		return list;
	}

	public static HerdBook read(ListTag list) {
		HerdBook book = new HerdBook();
		for (Tag raw : list) {
			CompoundTag tag = (CompoundTag) raw;
			try {
			UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
			UUID dam = tag.hasUUID("Dam") ? tag.getUUID("Dam") : null;
			UUID sire = tag.hasUUID("Sire") ? tag.getUUID("Sire") : null;
			HerdBook.Entry entry = new HerdBook.Entry(tag.getUUID("Id"), owner, tag.getString("Name"),
					tag.getBoolean("Male"), tag.getBoolean("Tame"), GenomeIO.read(tag.getCompound("Genome")), dam, sire,
					tag.getInt("Pelt"));
			book.load(entry);
			} catch (RuntimeException unreadable) {
				tk.darrow.shamanicmounts.ShamanicMounts.LOGGER.warn("Skipping an unreadable herd book entry: {}", unreadable.toString());
			}
		}
		return book;
	}

	/** The pages one player should see, including released parents so a foal can still name them. */
	public static HerdBook forPlayer(HerdBook world, UUID player) {
		HerdBook book = new HerdBook();
		for (HerdBook.Entry entry : world.entries()) {
			if (player.equals(entry.owner())) {
				book.load(entry);
				for (HerdBook.Entry forebear : world.forebears(entry.id())) {
					book.load(forebear);
				}
			}
		}
		return book;
	}
}
