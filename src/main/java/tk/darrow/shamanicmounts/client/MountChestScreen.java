package tk.darrow.shamanicmounts.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import tk.darrow.shamanicmounts.entity.MountChestMenu;
import tk.darrow.shamanicmounts.entity.MountMode;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.net.MountPayloads;

/**
 * The mount screen, laid out like the horse screen: saddle, bags, and barding slots down the left, the mount standing
 * beside them turning to follow the mouse, the saddle bags' rows on the right once bags are on,
 * and under them three buttons choosing whether it follows, stays, or wanders while nobody rides.
 */
public class MountChestScreen extends AbstractContainerScreen<MountChestMenu> {
	private static final int PORTRAIT_X = 28;
	private static final int PORTRAIT_W = 48;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_DARK = 0xFF8B8B8B;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int SLOT_LIGHT = 0xFFFFFFFF;
	private static final int SLOT_FACE = 0xFF8B8B8B;
	private static final int WELL = 0xFF4A4A4A;

	private Button follow;
	private Button stay;
	private Button wander;

	public MountChestScreen(MountChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 192;
		this.inventoryLabelY = MountChestMenu.INVENTORY_Y - 12;
	}

	@Override
	protected void init() {
		super.init();
		int y = this.topPos + MountChestMenu.GRID_Y + 3 * 18 + 6;
		int width = 52;
		follow = mode(MountMode.FOLLOW, this.leftPos + 8, y, width);
		stay = mode(MountMode.STAY, this.leftPos + 8 + width + 4, y, width);
		wander = mode(MountMode.WANDER, this.leftPos + 8 + (width + 4) * 2, y, width);
		refreshModes();
	}

	private Button mode(MountMode mode, int x, int y, int width) {
		Button button = Button.builder(Component.translatable(mode.key()), pressed -> choose(mode))
				.bounds(x, y, width, 18).build();
		addRenderableWidget(button);
		return button;
	}

	private void choose(MountMode mode) {
		ShamanicMount mount = this.menu.mount();
		if (mount == null) {
			return;
		}
		mount.setMode(mode);
		PacketDistributor.sendToServer(new MountPayloads.MountModeChoice(mount.getId(), mode.ordinal()));
		refreshModes();
	}

	/** The chosen mode's button is pressed in; the other two stay live. */
	private void refreshModes() {
		ShamanicMount mount = this.menu.mount();
		MountMode current = mount == null ? MountMode.WANDER : mount.mode();
		follow.active = current != MountMode.FOLLOW;
		stay.active = current != MountMode.STAY;
		wander.active = current != MountMode.WANDER;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;
		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
		graphics.fill(x, y, x + this.imageWidth - 1, y + 1, PANEL_LIGHT);
		graphics.fill(x, y, x + 1, y + this.imageHeight - 1, PANEL_LIGHT);
		graphics.fill(x + 1, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
		graphics.fill(x + this.imageWidth - 1, y + 1, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
		int wellTop = y + MountChestMenu.GRID_Y - 1;
		int wellBottom = y + MountChestMenu.GRID_Y + 3 * 18 + 1;
		graphics.fill(x + PORTRAIT_X - 1, wellTop, x + PORTRAIT_X + PORTRAIT_W + 1, wellBottom, WELL);
		graphics.fill(x + PORTRAIT_X, wellTop + 1, x + PORTRAIT_X + PORTRAIT_W, wellBottom - 1, 0xFF2A2A2A);
		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				slotFrame(graphics, x + slot.x, y + slot.y);
			}
		}
		// Ghost marks in the empty tack slots: saddle, bags, and barding.
		int[] ghosts = { 0x55603010, 0x55405030, 0x55404858 };
		for (int index = 0; index < 3; index++) {
			Slot slot = this.menu.slots.get(index);
			if (!slot.hasItem()) {
				graphics.fill(x + slot.x + 4, y + slot.y + 5, x + slot.x + 12, y + slot.y + 11, ghosts[index]);
			}
		}
		ShamanicMount mount = this.menu.mount();
		if (mount != null) {
			float scale = 34f / Math.max(1f, mount.getBbHeight());
			InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + PORTRAIT_X, wellTop, x + PORTRAIT_X + PORTRAIT_W,
					wellBottom, Math.max(6, Math.round(scale)), 0.0625f, mouseX, mouseY, mount);
		}
	}

	/** One vanilla-looking slot: dark top-left edge, light bottom-right edge, grey face. */
	private static void slotFrame(GuiGraphics graphics, int x, int y) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
		graphics.fill(x, y, x + 18, y + 18, SLOT_LIGHT);
		graphics.fill(x, y, x + 17, y + 17, SLOT_FACE);
		graphics.fill(x - 1, y - 1, x + 17, y, SLOT_DARK);
		graphics.fill(x - 1, y - 1, x, y + 17, SLOT_DARK);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		refreshModes();
	}
}
