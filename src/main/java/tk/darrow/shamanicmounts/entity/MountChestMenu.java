package tk.darrow.shamanicmounts.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import tk.darrow.shamanicmounts.item.MountItems;
import tk.darrow.shamanicmounts.ride.GiftRules;

/**
 * The mount screen. Three tack slots on the left: the saddle, the saddle bags, horse armor; the mount's
 * portrait beside them; and the bags' rows on the right, live only while bags are strapped on.
 * A plain mount's bags hold two rows of five; a mount with the elk's bearing holds three.
 */
public class MountChestMenu extends AbstractContainerMenu {
	public static final int COLUMNS = 5;
	public static final int TACK_X = 8;
	public static final int TACK_Y = 18;
	public static final int GRID_X = 80;
	public static final int GRID_Y = 18;
	/** Where the player's inventory starts. The hotbar sits under it. */
	public static final int INVENTORY_Y = 110;
	public static final int SADDLE_SLOT = 0;
	public static final int BAGS_SLOT = 1;
	public static final int ARMOR_SLOT = 2;
	public static final int FIRST_BAG = 3;

	private final Container tack;
	private final Container chest;
	@Nullable private final ShamanicMount mount;
	private final int rows;

	/** The client copy: the mount is looked up by id so the screen can draw it. */
	public MountChestMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
		this(id, playerInventory, new SimpleContainer(3), new SimpleContainer(GiftRules.CHEST_SLOTS),
				clientMount(playerInventory, buffer.readVarInt()), buffer.readVarInt());
	}

	public MountChestMenu(int id, Inventory playerInventory, Container tack, Container chest, @Nullable ShamanicMount mount,
			int rows) {
		super(MountMenus.CHEST.get(), id);
		this.tack = tack;
		this.chest = chest;
		this.mount = mount;
		this.rows = rows;
		checkContainerSize(tack, 3);
		checkContainerSize(chest, GiftRules.CHEST_SLOTS);
		chest.startOpen(playerInventory.player);
		this.addSlot(new TackSlot(tack, SADDLE_SLOT, TACK_X, TACK_Y, grown(stack -> stack.is(MountItems.SHAMANIC_SADDLE.get()))));
		this.addSlot(new TackSlot(tack, BAGS_SLOT, TACK_X, TACK_Y + 18, grown(stack -> stack.is(MountItems.SADDLE_BAGS.get()))));
		this.addSlot(new TackSlot(tack, ARMOR_SLOT, TACK_X, TACK_Y + 36, grown(MountChestMenu::isHorseArmor)));
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				this.addSlot(new Slot(chest, column + row * COLUMNS, GRID_X + column * 18, GRID_Y + row * 18) {
					@Override
					public boolean isActive() {
						return hasBags();
					}
				});
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(playerInventory, column, 8 + column * 18, INVENTORY_Y + 58));
		}
	}

	/** Vanilla horse armor: leather, iron, gold, or diamond. */
	public static boolean isHorseArmor(ItemStack stack) {
		return stack.getItem() instanceof net.minecraft.world.item.AnimalArmorItem armor
				&& armor.getBodyType() == net.minecraft.world.item.AnimalArmorItem.BodyType.EQUESTRIAN;
	}

	@Nullable
	private static ShamanicMount clientMount(Inventory inventory, int entityId) {
		Entity entity = inventory.player.level().getEntity(entityId);
		return entity instanceof ShamanicMount mount ? mount : null;
	}

	/** Tack only goes on a grown mount: a foal wears no saddle, bags, or armor. */
	private java.util.function.Predicate<ItemStack> grown(java.util.function.Predicate<ItemStack> fits) {
		return stack -> (mount == null || !mount.isBaby()) && fits.test(stack);
	}

	/** Rows the bags would hold on this mount: three with the elk's bearing, two otherwise. */
	public static int rowsFor(ShamanicMount mount) {
		return GiftRules.chest(mount.phenotype()) ? 3 : 2;
	}

	@Nullable
	public ShamanicMount mount() {
		return mount;
	}

	public int rows() {
		return rows;
	}

	public boolean hasBags() {
		return !tack.getItem(BAGS_SLOT).isEmpty();
	}

	private int bagEnd() {
		return FIRST_BAG + rows * COLUMNS;
	}

	@Override
	public boolean stillValid(Player player) {
		if (mount == null) {
			return true;
		}
		return mount.isAlive() && player.distanceToSqr(mount) < 64.0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack empty = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return empty;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int bagEnd = bagEnd();
		if (index < bagEnd) {
			if (!this.moveItemStackTo(stack, bagEnd, this.slots.size(), true)) {
				return empty;
			}
		} else {
			boolean moved = false;
			if (this.slots.get(SADDLE_SLOT).mayPlace(stack) && !this.slots.get(SADDLE_SLOT).hasItem()) {
				moved = this.moveItemStackTo(stack, SADDLE_SLOT, SADDLE_SLOT + 1, false);
			} else if (this.slots.get(BAGS_SLOT).mayPlace(stack) && !this.slots.get(BAGS_SLOT).hasItem()) {
				moved = this.moveItemStackTo(stack, BAGS_SLOT, BAGS_SLOT + 1, false);
			} else if (this.slots.get(ARMOR_SLOT).mayPlace(stack) && !this.slots.get(ARMOR_SLOT).hasItem()) {
				moved = this.moveItemStackTo(stack, ARMOR_SLOT, ARMOR_SLOT + 1, false);
			} else if (hasBags()) {
				moved = this.moveItemStackTo(stack, FIRST_BAG, bagEnd, false);
			}
			if (!moved) {
				return empty;
			}
		}
		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.chest.stopOpen(player);
	}
}
