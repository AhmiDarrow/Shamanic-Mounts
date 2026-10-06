package tk.darrow.shamanicmounts.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import tk.darrow.shamanicmounts.item.FounderEggItem;

import java.util.List;

/**
 * Grey how-to lines for items that declare {@code <descriptionId>.hint} in en_us, and one shared line for the
 * ten founder eggs. Long lines wrap to a tooltip's width.
 */
public final class ItemHints {
	private static final int WRAP_WIDTH = 220;

	private ItemHints() {
	}

	public static void tooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		List<Component> lines = event.getToolTip();
		String key = stack.getItem() instanceof FounderEggItem ? "item.shamanicmounts.egg.hint" : stack.getItem().getDescriptionId() + ".hint";
		if (I18n.exists(key)) addWrapped(lines, Component.translatable(key));
	}

	private static void addWrapped(List<Component> lines, Component text) {
		for (FormattedText part : Minecraft.getInstance().font.getSplitter().splitLines(text, WRAP_WIDTH, Style.EMPTY)) {
			lines.add(Component.literal(part.getString()).withStyle(ChatFormatting.GRAY));
		}
	}
}
