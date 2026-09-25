package tk.darrow.shamanicmounts.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import tk.darrow.shamanicmounts.net.MountPayloads;

/** The herd book. Use it to read your tames. Breeding details stay behind the local spoiler switch. */
public class HerdBookItem extends Item {
	public HerdBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			MountPayloads.sendHerd(serverPlayer);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tip, TooltipFlag flag) {
		tip.add(Component.translatable("book.shamanicmounts.tip").withStyle(ChatFormatting.GRAY));
	}
}
