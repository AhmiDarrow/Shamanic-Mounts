package tk.darrow.shamanicmounts.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import tk.darrow.shamanicmounts.ShamanicMounts;

public final class MountMenus {
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ShamanicMounts.MOD_ID);

	public static final DeferredHolder<MenuType<?>, MenuType<MountChestMenu>> CHEST = MENUS.register("mount_chest",
			() -> IMenuTypeExtension.create((windowId, inventory, buffer) -> new MountChestMenu(windowId, inventory, buffer)));

	private MountMenus() {
	}
}
