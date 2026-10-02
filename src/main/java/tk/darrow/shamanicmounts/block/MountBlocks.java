package tk.darrow.shamanicmounts.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import tk.darrow.shamanicmounts.ShamanicMounts;

public final class MountBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ShamanicMounts.MOD_ID);

	public static final DeferredBlock<MountTradingPostBlock> MOUNT_TRADING_POST = BLOCKS.registerBlock("mount_trading_post",
			MountTradingPostBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)
					.sound(SoundType.STONE).noOcclusion());

	private MountBlocks() {
	}
}
