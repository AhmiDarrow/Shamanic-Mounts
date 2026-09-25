package tk.darrow.shamanicmounts;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import tk.darrow.shamanicmounts.entity.MountEntities;
import tk.darrow.shamanicmounts.entity.MountMenus;
import tk.darrow.shamanicmounts.item.MountItems;
import tk.darrow.shamanicmounts.net.MountPayloads;

@Mod(ShamanicMounts.MOD_ID)
public final class ShamanicMounts {
	public static final String MOD_ID = "shamanicmounts";
	public static final Logger LOGGER = LogUtils.getLogger();

	public ShamanicMounts(IEventBus modBus) {
		MountItems.ITEMS.register(modBus);
		MountItems.TABS.register(modBus);
		MountEntities.ENTITIES.register(modBus);
		MountMenus.MENUS.register(modBus);
		modBus.addListener(MountItems::creative);
		modBus.addListener(MountEntities::attributes);
		modBus.addListener(MountEntities::placements);
		modBus.addListener(MountPayloads::register);
		LOGGER.info("Shamanic Mounts genome ready");
	}
}
