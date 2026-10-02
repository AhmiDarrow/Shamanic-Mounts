package tk.darrow.shamanicmounts.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.trade.MountPosts;

/** A short carved post. Any of them uses the same offer book. */
public class MountTradingPostBlock extends Block {
	/** The post footprint. The cloth and the ring do not stop a mount. */
	private static final VoxelShape COLLISION = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(7, 2, 7, 9, 16, 9));
	/** Wide enough to click: the post column plus the hanging cloth. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 16, 12), Block.box(11, 4, 6, 15, 14, 10));

	public MountTradingPostBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}

	@Override
	protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.block();
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer server) {
			boolean sneaking = server.isShiftKeyDown();
			if (server.getVehicle() instanceof ShamanicMount mount) {
				sneaking = mount.riderSneak();
			}
			MountPosts.use(server, pos, sneaking);
		}
		return InteractionResult.CONSUME;
	}
}
