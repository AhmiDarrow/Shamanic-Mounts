package tk.darrow.shamanicmounts.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import tk.darrow.shamanicmounts.entity.MountEntities;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.genome.Genome;

/** A creative egg for one founder line. The icon is a painted egg from {@code tools/paint_item_icons.py}; the two colours only feed the spawn-egg registry. */
public class FounderEggItem extends DeferredSpawnEggItem {
	private final Genome genome;

	public FounderEggItem(Genome genome, int base, int spots, Properties properties) {
		super(MountEntities.MOUNT, base, spots, properties);
		this.genome = genome;
	}

	public Genome genome() {
		return genome;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (spawn(server, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getPlayer() == null ? 0.0f
				: context.getPlayer().getYRot()) == null) {
			return InteractionResult.FAIL;
		}
		if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.CONSUME;
	}

	/** Air use stays quiet. A plain egg would otherwise spawn a random mount. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return InteractionResultHolder.pass(player.getItemInHand(hand));
	}

	@Override
	protected DispenseItemBehavior createDispenseBehavior() {
		return this::dispense;
	}

	private ItemStack dispense(BlockSource source, ItemStack stack) {
		if (!(stack.getItem() instanceof FounderEggItem egg)) {
			return stack;
		}
		Direction facing = source.state().getValue(DispenserBlock.FACING);
		BlockPos pos = source.pos().relative(facing);
		if (egg.spawn(source.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.toYRot()) == null) {
			return stack;
		}
		stack.shrink(1);
		return stack;
	}

	private ShamanicMount spawn(ServerLevel level, double x, double y, double z, float yaw) {
		ShamanicMount mount = MountEntities.MOUNT.get().create(level);
		if (mount == null) {
			return null;
		}
		mount.moveTo(x, y, z, yaw, 0.0f);
		// An egg hatches its line in the pelt and size the biome here would give a wild one.
		mount.setGenome(tk.darrow.shamanicmounts.world.MountBiomes.wild(genome, tk.darrow.shamanicmounts.world.MountBiomes.lineOf(genome),
				level.getBiome(BlockPos.containing(x, y, z)), level.getRandom()), true);
		mount.rollSex();
		mount.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.SPAWN_EGG,
				null);
		if (!level.noCollision(mount)) {
			return null;
		}
		level.addFreshEntity(mount);
		return mount;
	}
}
