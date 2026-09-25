package tk.darrow.shamanicmounts.genome;

import net.minecraft.nbt.CompoundTag;

/** The genome as NBT, so a mount and the herd book can remember it. */
public final class GenomeIO {
	private GenomeIO() {
	}

	public static CompoundTag write(Genome genome) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("HeadM", genome.headFromMaternal);
		tag.putBoolean("FootM", genome.footFromMaternal);
		tag.putBoolean("TailM", genome.tailFromMaternal);
		tag.putBoolean("Chimera", genome.chimera);
		tag.put("Maternal", strand(genome.maternal));
		tag.put("Paternal", strand(genome.paternal));
		return tag;
	}

	public static Genome read(CompoundTag tag) {
		return new Genome(strand(tag.getCompound("Maternal")), strand(tag.getCompound("Paternal")),
				tag.getBoolean("HeadM"), tag.getBoolean("FootM"), tag.getBoolean("TailM"), tag.getBoolean("Chimera"));
	}

	private static CompoundTag strand(Strand strand) {
		CompoundTag tag = new CompoundTag();
		for (Locus locus : Locus.values()) {
			tag.putString(locus.name(), strand.get(locus).code());
		}
		return tag;
	}

	private static Strand strand(CompoundTag tag) {
		java.util.EnumMap<Locus, Allele> map = new java.util.EnumMap<>(Locus.class);
		for (Locus locus : Locus.values()) {
			map.put(locus, allele(locus, tag.getString(locus.name())));
		}
		return new Strand(map);
	}

	static Allele allele(Locus locus, String code) {
		if (code == null || code.isEmpty()) {
			throw new IllegalArgumentException(locus.name());
		}
		if ("none".equals(code) && isGift(locus)) {
			return Marks.Off.of(locus);
		}
		String name = code.toUpperCase();
		return switch (locus) {
			case TORSO -> Marks.Torso.valueOf(name);
			case HEAD -> Marks.Head.valueOf(name);
			case LEGS -> Marks.Leg.valueOf(name);
			case FOOT -> Marks.Foot.valueOf(name);
			case RACK -> Marks.Rack.valueOf(name);
			case GAIT -> Marks.Gait.valueOf(name);
			case WINGS -> Marks.Wing.valueOf(name);
			case WINGSPAN -> Marks.Span.valueOf(name);
			case TAIL -> Marks.Tail.valueOf(name);
			case SCALE -> Marks.Scale.valueOf(name);
			case COAT -> Marks.Coat.valueOf(name);
			case REALM -> Marks.Realm.valueOf(name);
			case PHASE -> Marks.Phase.valueOf(name);
			case SENSE -> Marks.Sense.valueOf(name);
			case BOND -> Marks.Bond.valueOf(name);
			case WARD -> Marks.Ward.valueOf(name);
			case TRAIL -> Marks.Trail.valueOf(name);
			case ROAD, CALL, BEARING, FERRY, SKIN, OMEN, DREAM, VEIL, MIGHT, COIL -> Marks.Gift.valueOf(name);
		};
	}

	private static boolean isGift(Locus locus) {
		return switch (locus) {
			case ROAD, CALL, BEARING, FERRY, SKIN, OMEN, DREAM, VEIL, MIGHT, COIL -> true;
			default -> false;
		};
	}
}
