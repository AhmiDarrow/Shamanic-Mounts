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

import tk.darrow.shamanicmounts.sound.CallRotation;
import tk.darrow.shamanicmounts.sound.MountSounds;
import tk.darrow.shamanicmounts.world.MountCall;

/** Calls the player's own tame mounts, from any distance and any dimension. */
public class MountFluteItem extends Item {
	/** When nothing answered: long enough that holding the button cannot drone the flute. */
	public static final int BREATH = 40;
	private static final CallRotation CALLS = new CallRotation();

	public MountFluteItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer && !serverPlayer.getCooldowns().isOnCooldown(this)) {
			// The flute sounds whether or not a mount hears it; each blow is the next phrase in turn.
			var call = MountSounds.FLUTE_CALLS.get(CALLS.next(serverPlayer.getUUID(), MountSounds.FLUTE_CALLS.size()));
			serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
					call.get(), serverPlayer.getSoundSource(), 0.9F, 1.0F);
			serverPlayer.getCooldowns().addCooldown(this, MountCall.blow(serverPlayer) ? MountCall.COOLDOWN : BREATH);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tip, TooltipFlag flag) {
		tip.add(Component.translatable("shamanicmounts.tip.flute").withStyle(ChatFormatting.GRAY));
	}
}
