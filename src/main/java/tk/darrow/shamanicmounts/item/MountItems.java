package tk.darrow.shamanicmounts.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import tk.darrow.shamanicmounts.genome.Founders;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.shamanicmounts.ShamanicMounts;
import tk.darrow.shamanicmounts.ride.BreedingRules;
import tk.darrow.shamanicmounts.tack.SaddleRules;

public final class MountItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ShamanicMounts.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
			ShamanicMounts.MOD_ID);

	public static final DeferredItem<Item> SHAMANIC_SADDLE = ITEMS.register(SaddleRules.ITEM_ID,
			() -> new Item(new Item.Properties().stacksTo(1)));

	/** Strapped on behind the saddle from the mount screen. Two rows of five; three with the elk's bearing. */
	public static final DeferredItem<Item> SADDLE_BAGS = ITEMS.register("saddle_bags",
			() -> new Item(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<Item> HERD_BOOK = ITEMS.register("herd_book",
			() -> new HerdBookItem(new Item.Properties().stacksTo(1)));

	/** Eight diamonds around a carrot. Feed one to each tame adult you want to breed. */
	public static final DeferredItem<Item> DIAMOND_APPLE = ITEMS.register(BreedingRules.ITEM_ID,
			() -> new Item(new Item.Properties().stacksTo(64)
					.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

	public static final DeferredItem<Item> EGG_EIGHTFOLD = egg("egg_eightfold", Founders.eightfold(), 0x8B5A2B, 0x3E2414);
	public static final DeferredItem<Item> EGG_DRUM_HART = egg("egg_drum_hart", Founders.drumHart(), 0xE6C98A, 0x8C6239);
	public static final DeferredItem<Item> EGG_ELK = egg("egg_elk", Founders.elk(), 0x6B3E24, 0x1A100C);
	public static final DeferredItem<Item> EGG_CRANE = egg("egg_crane", Founders.crane(), 0xF4F1EA, 0x2A8F92);
	public static final DeferredItem<Item> EGG_NAGUAL = egg("egg_nagual", Founders.nagual(), 0xE39B2B, 0x2C160C);
	public static final DeferredItem<Item> EGG_BARGHEST = egg("egg_barghest", Founders.barghest(), 0xB7B3BA, 0x2E2A30);
	public static final DeferredItem<Item> EGG_ROC = egg("egg_roc", Founders.roc(), 0xE4D0A4, 0x4C3496);
	public static final DeferredItem<Item> EGG_SHADE = egg("egg_shade", Founders.shade(), 0x9B90B4, 0x322C48);
	public static final DeferredItem<Item> EGG_BEAR = egg("egg_bear", Founders.bear(), 0x4B3621, 0xEDE6D6);
	public static final DeferredItem<Item> EGG_SERPENT = egg("egg_serpent", Founders.serpent(), 0x3F6B2E, 0xD9C15A);

	private static DeferredItem<Item> egg(String name, tk.darrow.shamanicmounts.genome.Genome genome, int base, int spots) {
		return ITEMS.register(name, () -> new FounderEggItem(genome, base, spots, new Item.Properties().stacksTo(64)));
	}

	/** The mod's own creative tab: the tack, the book, the apple, and the ten eggs. */
	public static final java.util.function.Supplier<CreativeModeTab> TAB = TABS.register("shamanicmounts",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.shamanicmounts"))
					.icon(() -> SHAMANIC_SADDLE.get().getDefaultInstance())
					.displayItems((parameters, output) -> {
						output.accept(SHAMANIC_SADDLE.get());
						output.accept(SADDLE_BAGS.get());
						output.accept(HERD_BOOK.get());
						output.accept(DIAMOND_APPLE.get());
						output.accept(EGG_EIGHTFOLD.get());
						output.accept(EGG_DRUM_HART.get());
						output.accept(EGG_ELK.get());
						output.accept(EGG_CRANE.get());
						output.accept(EGG_NAGUAL.get());
						output.accept(EGG_BARGHEST.get());
						output.accept(EGG_ROC.get());
						output.accept(EGG_SHADE.get());
						output.accept(EGG_BEAR.get());
						output.accept(EGG_SERPENT.get());
					})
					.build());

	private MountItems() {
	}

	public static void creative(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(SHAMANIC_SADDLE);
			event.accept(SADDLE_BAGS);
			event.accept(HERD_BOOK);
		}
		if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			event.accept(DIAMOND_APPLE);
		}
		if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			event.accept(EGG_EIGHTFOLD);
			event.accept(EGG_DRUM_HART);
			event.accept(EGG_ELK);
			event.accept(EGG_CRANE);
			event.accept(EGG_NAGUAL);
			event.accept(EGG_BARGHEST);
			event.accept(EGG_ROC);
			event.accept(EGG_SHADE);
			event.accept(EGG_BEAR);
			event.accept(EGG_SERPENT);
		}
	}
}
