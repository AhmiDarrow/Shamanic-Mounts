package tk.darrow.shamanicmounts.entity;

import java.util.function.Predicate;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One piece of tack on the mount screen: the saddle, the bags, and in time the armor. Holds one. */
public final class TackSlot extends Slot {
	private final Predicate<ItemStack> fits;

	public TackSlot(Container tack, int index, int x, int y, Predicate<ItemStack> fits) {
		super(tack, index, x, y);
		this.fits = fits;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return fits.test(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}
}
