package tk.darrow.shamanicmounts.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import tk.darrow.shamanicmounts.world.MountCall;

/** Calls the player's own tame mounts, from any distance and any dimension. */
public class MountFluteItem extends Item {
	public MountFluteItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer && !serverPlayer.getCooldowns().isOnCooldown(this)) {
			if (MountCall.blow(serverPlayer)) {
				serverPlayer.getCooldowns().addCooldown(this, MountCall.COOLDOWN);
			}
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tip, TooltipFlag flag) {
		tip.add(Component.translatable("shamanicmounts.tip.flute").withStyle(ChatFormatting.GRAY));
	}
}
