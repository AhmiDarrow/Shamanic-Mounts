package tk.darrow.shamanicmounts.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.Strand;

/**
 * Where each line lives and what it looks like there. Everything is a tag under
 * {@code data/shamanicmounts/tags}, written by {@code tools/write_spawn_tags.py}: the lines use the
 * common {@code c:} biome tags, so a modded biome tagged as a forest, a taiga, or a snowy plain gets
 * the right mounts, and a datapack can add any biome by name.
 */
public final class MountBiomes {
	/** A founder line: its tag name and its founder genome. */
	public enum Line {
		EIGHTFOLD("eightfold", Founders::eightfold),
		DRUM_HART("drum_hart", Founders::drumHart),
		ELK("elk", Founders::elk),
		CRANE("crane", Founders::crane),
		NAGUAL("nagual", Founders::nagual),
		BARGHEST("barghest", Founders::barghest),
		ROC("roc", Founders::roc),
		SHADE("shade", Founders::shade),
		BEAR("bear", Founders::bear),
		SERPENT("serpent", Founders::serpent);

		public final String key;
		private final Supplier<Genome> founder;
		public final TagKey<Biome> spawns;
		/** Home biomes of pelts A, B, and C. */
		private final List<TagKey<Biome>> pelts;

		Line(String key, Supplier<Genome> founder) {
			this.key = key;
			this.founder = founder;
			this.spawns = biome("spawns/" + key);
			this.pelts = List.of(biome("pelts/" + key + "/a"), biome("pelts/" + key + "/b"), biome("pelts/" + key + "/c"));
		}

		public Genome founder() {
			return founder.get();
		}

		/** The pelt at home in this biome, rarest first, or -1 when none is. */
		public int homePelt(Holder<Biome> biome) {
			for (int pelt = 2; pelt >= 0; pelt--) {
				if (biome.is(pelts.get(pelt))) {
					return pelt;
				}
			}
			return -1;
		}
	}

	public static final TagKey<Biome> LARGER = biome("size/larger");
	public static final TagKey<Biome> SMALLER = biome("size/smaller");
	public static final TagKey<Block> SPAWNABLE_ON = TagKey.create(Registries.BLOCK,
			ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, "spawnable_on"));
	/** How often each copy of the pelt gene takes the biome's home pelt. Two copies make it show. */
	private static final float HOME = 0.85f;

	private MountBiomes() {
	}

	private static TagKey<Biome> biome(String path) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, path));
	}

	/** Every line that lives in this biome. */
	public static List<Line> linesIn(Holder<Biome> biome) {
		ArrayList<Line> lines = new ArrayList<>();
		for (Line line : Line.values()) {
			if (biome.is(line.spawns)) {
				lines.add(line);
			}
		}
		return lines;
	}

	/** A line for a wild mount here: one that lives in the biome, or any line where none does. */
	public static Line pick(Holder<Biome> biome, RandomSource random) {
		List<Line> lines = linesIn(biome);
		if (lines.isEmpty()) {
			return Line.values()[random.nextInt(Line.values().length)];
		}
		return lines.get(random.nextInt(lines.size()));
	}

	/**
	 * A wild mount of {@code founder}'s line born in {@code biome}: each copy of its size and pelt rolled
	 * on its own. The biome's home pelt takes most copies, the cold leans a size up, the heat a size down.
	 */
	public static Genome wild(Genome founder, Line line, Holder<Biome> biome, RandomSource random) {
		int home = line == null ? -1 : line.homePelt(biome);
		int lean = biome.is(LARGER) ? 1 : biome.is(SMALLER) ? -1 : 0;
		return new Genome(roll(founder.maternal, home, lean, random), roll(founder.paternal, home, lean, random),
				founder.headFromMaternal, founder.footFromMaternal, founder.tailFromMaternal, founder.chimera);
	}

	private static Strand roll(Strand strand, int home, int lean, RandomSource random) {
		int sizeRoll = random.nextInt(100);
		int size = sizeRoll < 10 ? 0 : sizeRoll < 30 ? 1 : sizeRoll < 70 ? 2 : sizeRoll < 90 ? 3 : 4;
		size = Math.max(0, Math.min(4, size + lean));
		Marks.Pelt pelt;
		if (home >= 0 && random.nextFloat() < HOME) {
			pelt = Marks.Pelt.values()[home];
		} else {
			int peltRoll = random.nextInt(100);
			pelt = peltRoll < 40 ? Marks.Pelt.A : peltRoll < 70 ? Marks.Pelt.B : Marks.Pelt.C;
		}
		return strand.with(Marks.Size.values()[size]).with(pelt);
	}

	/** The line whose founder genome this is, or null. Every founder carries its own one gift. */
	public static Line lineOf(Genome founder) {
		for (Line line : Line.values()) {
			Genome genome = line.founder();
			boolean same = true;
			for (Marks.Gift gift : Marks.Gift.values()) {
				if (gift != Marks.Gift.NONE && genome.maternal.get(gift.locus()) != founder.maternal.get(gift.locus())) {
					same = false;
					break;
				}
			}
			if (same) {
				return line;
			}
		}
		return null;
	}

	/** Wild mounts stand on grass, dirt, sand, snow, or stone, in daylight, like other animals. */
	public static boolean canSpawn(EntityType<?> type, LevelAccessor level, MobSpawnType reason, BlockPos pos,
			RandomSource random) {
		return level.getBlockState(pos.below()).is(SPAWNABLE_ON) && level.getRawBrightness(pos, 0) > 8;
	}

	/** The biome at a spot. */
	public static Holder<Biome> at(ServerLevelAccessor level, BlockPos pos) {
		return level.getBiome(pos);
	}
}
