package tk.darrow.shamanicmounts.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import tk.darrow.shamanicmounts.book.Codex;
import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.book.SpoilerPref;
import tk.darrow.shamanicmounts.book.TamePage;
import tk.darrow.shamanicmounts.entity.MountEntities;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.genome.Genome;

/**
 * The herd book. Basics, a gallery of the lines, your tames with their portraits and genes in
 * genealogy notation, the key to that notation, and, with spoilers on, the breeding chapter.
 */
public final class HerdBookScreen extends Screen {
	private static final int LEFT = 8;
	private static final int LEFT_W = 110;
	private static final int TEXT_X = 132;
	private static final int RIGHT = 8;
	private static final int TOP = 24;
	private static final int LINE = 12;
	private static final int INK = 0xFFE6D7B8;
	private static final int PALE = 0xFFB9AB90;
	private static final int GOLD = 0xFFE8C16A;
	private static final int PAGE = 0xF01C160F;
	private static final int WELL = 0xFF2A2218;
	private static final int WELL_EDGE = 0xFF5A4A34;
	/** The gene columns: the symbol and notation, the plain word, then the note. */
	private static final int COLUMN_WORD = 78;
	private static final int COLUMN_NOTE = 160;
	private static final int PORTRAIT_W = 92;
	private static final int PORTRAIT_H = 100;
	private static final int GALLERY_W = 108;
	private static final int GALLERY_H = 72;

	private final UUID player;
	private final HerdBook book;
	private final SpoilerPref spoilers;
	private final Map<UUID, ShamanicMount> previews = new HashMap<>();
	private final Map<String, ShamanicMount> linePreviews = new HashMap<>();
	private Codex.Page page = Codex.Page.BASICS;
	@Nullable private UUID selected;
	private double scroll;
	private boolean confirmRelease;
	@Nullable private EditBox nameBox;

	public HerdBookScreen(UUID player, HerdBook book, SpoilerPref spoilers) {
		super(Component.translatable("item.shamanicmounts.herd_book"));
		this.player = player;
		this.book = book;
		this.spoilers = spoilers;
	}

	@Override
	protected void init() {
		if (page == Codex.Page.BREEDING && !spoilers.shown(player)) {
			page = Codex.Page.BASICS;
		}
		int y = 28;
		for (Codex.Page chapter : Codex.open(spoilers.shown(player))) {
			Codex.Page choice = chapter;
			addRenderableWidget(Button.builder(Component.translatable("book.shamanicmounts." + chapter.name().toLowerCase()),
					button -> {
						page = choice;
						selected = null;
						confirmRelease = false;
						scroll = 0;
						rebuildWidgets();
					}).bounds(LEFT, y, LEFT_W, 20).build());
			y += 22;
		}
		addRenderableWidget(Button.builder(Component.translatable(spoilers.shown(player)
				? "book.shamanicmounts.spoilers_on" : "book.shamanicmounts.spoilers_off"), button -> {
					spoilers.set(player, !spoilers.shown(player));
					scroll = 0;
					rebuildWidgets();
				}).bounds(LEFT, this.height - 28, LEFT_W, 20).build());

		HerdBook.Entry tame = selected == null ? null : book.get(selected);
		if (page == Codex.Page.TAMES && tame != null && tame.tame() && player.equals(tame.owner())) {
			addRenderableWidget(Button.builder(Component.translatable("book.shamanicmounts.back"), button -> {
				selected = null;
				confirmRelease = false;
				scroll = 0;
				rebuildWidgets();
			}).bounds(TEXT_X, 28, 60, 20).build());
			nameBox = new EditBox(this.font, TEXT_X, 52, 140, 18, Component.translatable("book.shamanicmounts.rename"));
			nameBox.setMaxLength(24);
			nameBox.setValue(tame.name());
			addRenderableWidget(nameBox);
			addRenderableWidget(Button.builder(Component.translatable("book.shamanicmounts.rename"), button -> {
				if (book.rename(player, selected, nameBox.getValue())) {
					confirmRelease = false;
					ClientBook.tellServer(0, selected, book.get(selected).name());
				}
			}).bounds(TEXT_X + 146, 52, 70, 18).build());
			addRenderableWidget(Button.builder(Component.translatable(confirmRelease
					? "book.shamanicmounts.release_sure" : "book.shamanicmounts.release"), button -> {
						if (!confirmRelease) {
							confirmRelease = true;
							rebuildWidgets();
							return;
						}
						UUID released = selected;
						book.release(player, selected);
						ClientBook.tellServer(1, released, "");
						selected = null;
						confirmRelease = false;
						scroll = 0;
						rebuildWidgets();
					}).bounds(TEXT_X, 76, 90, 18).build());
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0, scroll - scrollY * 12);
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (page == Codex.Page.TAMES && selected == null && button == 0 && mouseX >= TEXT_X) {
			List<HerdBook.Entry> tames = book.tames(player);
			int index = Math.floorDiv((int) (mouseY - 32 + scroll), 14);
			if (index >= 0 && index < tames.size()) {
				selected = tames.get(index).id();
				scroll = 0;
				confirmRelease = false;
				rebuildWidgets();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/**
	 * The page is drawn after the widgets. {@code super.render} blurs whatever is already on screen as
	 * the menu background, so text drawn before it would be blurred with the world.
	 */
	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(TEXT_X - 8, TOP, this.width - RIGHT, this.height - 8, PAGE);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		drawPage(graphics, mouseX, mouseY);
	}

	private void drawPage(GuiGraphics graphics, int mouseX, int mouseY) {
		int width = this.width - TEXT_X - RIGHT - 8;
		int y = 32 - (int) scroll;
		switch (page) {
			case BASICS -> {
				y = heading(graphics, "Basics", y);
				paragraph(graphics, Codex.basics(), y, width);
			}
			case BREEDING -> {
				y = heading(graphics, "Breeding", y);
				paragraph(graphics, Codex.breeding(), y, width);
			}
			case KEY -> {
				y = heading(graphics, "Key", y);
				for (String text : Codex.key(spoilers.shown(player))) {
					if (text.isEmpty()) {
						y += 6;
						continue;
					}
					for (String wrapped : wrap(text, width)) {
						y = line(graphics, wrapped, y, INK);
					}
				}
			}
			case LINES -> gallery(graphics, mouseX, mouseY, y, width);
			case TAMES -> {
				if (selected == null) {
					// Rows start at the top so a click at 36 + 14n picks row n.
					for (String text : tameLines()) {
						if (y > 20 && y < this.height - 12) {
							graphics.drawString(this.font, text, TEXT_X, y, INK, false);
						}
						y += 14;
					}
				} else {
					tamePage(graphics, mouseX, mouseY);
				}
			}
		}
	}

	/** The nine founders in a grid, each standing in its own well over its name and a line about it. */
	private void gallery(GuiGraphics graphics, int mouseX, int mouseY, int y, int width) {
		y = heading(graphics, "Lines", y);
		int columns = Math.max(1, width / (GALLERY_W + 8));
		int index = 0;
		boolean spoiled = spoilers.shown(player);
		for (Codex.Line entry : Codex.lines()) {
			int column = index % columns;
			int row = index / columns;
			int x = TEXT_X + column * (GALLERY_W + 8);
			int top = y + row * (GALLERY_H + 44);
			index++;
			if (top + GALLERY_H + 40 < TOP || top > this.height) {
				continue;
			}
			well(graphics, x, top, GALLERY_W, GALLERY_H);
			ShamanicMount preview = linePreview(entry.name(), entry.genome());
			if (preview != null && top >= TOP - 2 && top + GALLERY_H <= this.height - 10) {
				portrait(graphics, preview, x, top, GALLERY_W, GALLERY_H, mouseX, mouseY);
			}
			int textY = top + GALLERY_H + 3;
			graphics.drawString(this.font, entry.name(), x + 2, textY, GOLD, false);
			String blurb = entry.spoiler() && !spoiled ? "The ninth mount." : entry.blurb();
			int blurbY = textY + 11;
			for (String wrapped : wrap(blurb, GALLERY_W - 2)) {
				if (blurbY > TOP && blurbY < this.height - 10) {
					graphics.drawString(this.font, wrapped, x + 2, blurbY, PALE, false);
				}
				blurbY += 10;
			}
		}
	}

	/** One tame: its portrait beside the rename and release controls, then family, coat, and genes. */
	private void tamePage(GuiGraphics graphics, int mouseX, int mouseY) {
		HerdBook.Entry entry = book.get(selected);
		if (entry == null) {
			return;
		}
		// The portrait sits top right, beside the rename and release controls, and shrinks on a narrow screen.
		int wellW = Math.min(PORTRAIT_W, this.width - RIGHT - 8 - (TEXT_X + 222));
		int wellX = this.width - RIGHT - 8 - wellW;
		int wellY = 28 - (int) scroll;
		if (wellW >= 36) {
			well(graphics, wellX, wellY, wellW, PORTRAIT_H);
			ShamanicMount preview = preview(entry);
			if (preview != null && wellY >= TOP - 2) {
				portrait(graphics, preview, wellX, wellY, wellW, PORTRAIT_H, mouseX, mouseY);
			}
		}
		int y = 100 - (int) scroll;
		boolean showNotes = spoilers.shown(player);
		y = heading(graphics, entry.name(), y);
		for (String text : TamePage.family(book, entry)) {
			y = line(graphics, text, y, INK);
		}
		y = line(graphics, "Pelt: " + TamePage.pelt(entry.genome(), entry.pelt()), y, INK);
		String wings = TamePage.wings(entry.genome());
		if (!wings.isEmpty()) {
			y = line(graphics, wings, y, INK);
		}
		y += 6;
		y = subheading(graphics, "Genes", y);
		for (TamePage.Row row : TamePage.genes(entry.genome(), showNotes)) {
			if (y > 20 && y < this.height - 12) {
				graphics.drawString(this.font, row.symbol() + " " + row.notation(), TEXT_X, y, GOLD, false);
				graphics.drawString(this.font, row.shown(), TEXT_X + COLUMN_WORD, y, INK, false);
				if (showNotes && !row.note().isEmpty() && !"plain".equals(row.note())) {
					graphics.drawString(this.font, row.note(), TEXT_X + COLUMN_NOTE, y, PALE, false);
				}
			}
			y += LINE;
		}
	}

	private void well(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, WELL_EDGE);
		graphics.fill(x, y, x + w, y + h, WELL);
	}

	private static void portrait(GuiGraphics graphics, ShamanicMount preview, int x, int y, int w, int h, int mouseX,
			int mouseY) {
		// Antlers, necks, and frills stand above the hitbox, so the portrait leaves them a fifth more room.
		int scale = Math.max(8, Math.round((h - 12) * 0.8f / Math.max(1f, preview.getBbHeight())));
		// A three-quarter view by default, so eyes set in the side of the head face the reader. Hovering
		// the portrait lets it follow the mouse instead.
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		int lookX = hover ? mouseX : x + w / 2 - 60;
		int lookY = hover ? mouseY : y + h / 2 + 10;
		InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, x + w, y + h, scale, 0.0625f, lookX, lookY,
				preview);
	}

	/** Closing the book drops every preview, so nothing of it stays behind. */
	@Override
	public void removed() {
		super.removed();
		previews.clear();
		linePreviews.clear();
	}

	/** A client-side copy of the tame, built from its genome and coat and never added to the world. */
	@Nullable
	private ShamanicMount preview(HerdBook.Entry entry) {
		return previews.computeIfAbsent(entry.id(), id -> build(entry.genome(), entry.pelt()));
	}

	@Nullable
	private ShamanicMount linePreview(String name, Genome genome) {
		return linePreviews.computeIfAbsent(name, key -> build(genome, 0));
	}

	@Nullable
	private static ShamanicMount build(Genome genome, int pelt) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return null;
		}
		ShamanicMount mount = MountEntities.MOUNT.get().create(minecraft.level);
		if (mount == null) {
			return null;
		}
		mount.setGenome(genome, true);
		mount.setPelt(pelt);
		return mount;
	}

	/** The tame list, or one tame's family and genes, in the same words the page draws. */
	private List<String> tameLines() {
		ArrayList<String> lines = new ArrayList<>();
		if (selected == null) {
			List<HerdBook.Entry> tames = book.tames(player);
			if (tames.isEmpty()) {
				lines.add("No tames in the book yet.");
				return lines;
			}
			for (HerdBook.Entry entry : tames) {
				lines.add(entry.name());
			}
			return lines;
		}
		HerdBook.Entry entry = book.get(selected);
		if (entry == null) {
			return lines;
		}
		boolean showNotes = spoilers.shown(player);
		lines.addAll(TamePage.family(book, entry));
		lines.add("Pelt: " + TamePage.pelt(entry.genome(), entry.pelt()));
		String wings = TamePage.wings(entry.genome());
		if (!wings.isEmpty()) {
			lines.add(wings);
		}
		lines.add("");
		for (TamePage.Row row : TamePage.genes(entry.genome(), showNotes)) {
			String text = row.symbol() + " " + row.notation() + "  " + row.shown();
			if (showNotes && !row.note().isEmpty() && !"plain".equals(row.note())) {
				text = text + "  " + row.note();
			}
			lines.add(text);
		}
		return lines;
	}

	/** The lines the open page is drawing, for the harness. */
	private List<String> pageLines() {
		return switch (page) {
			case BASICS -> Codex.basics();
			case BREEDING -> Codex.breeding();
			case KEY -> Codex.key(spoilers.shown(player));
			case LINES -> {
				ArrayList<String> out = new ArrayList<>();
				for (Codex.Line entry : Codex.lines()) {
					out.add(entry.name() + ": " + (entry.spoiler() && !spoilers.shown(player) ? "The ninth mount." : entry.blurb()));
				}
				yield out;
			}
			case TAMES -> tameLines();
		};
	}

	/** One line for the live harness: page, spoilers, tame names, and the lines on the page. */
	String harnessReport() {
		StringBuilder out = new StringBuilder();
		out.append("page=").append(page.name());
		out.append("|spoilers=").append(spoilers.shown(player));
		out.append("|tames=");
		boolean first = true;
		for (HerdBook.Entry entry : book.tames(player)) {
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append(entry.name());
		}
		HerdBook.Entry selectedEntry = selected == null ? null : book.get(selected);
		out.append("|selected=").append(selectedEntry == null ? "" : selectedEntry.name());
		out.append("|confirm=").append(confirmRelease);
		out.append("|lines=");
		first = true;
		for (String line : pageLines()) {
			if (!first) {
				out.append(';');
			}
			first = false;
			out.append(line.replace('|', '/').replace(';', ','));
		}
		return out.toString();
	}

	/** The harness types a new name into the open tame's box. */
	String harnessType(String text) {
		if (nameBox == null) {
			return "error no name box";
		}
		nameBox.setValue(text);
		return "ok";
	}

	private int heading(GuiGraphics graphics, String text, int y) {
		if (y > 20 && y < this.height - 12) {
			graphics.drawString(this.font, text, TEXT_X, y, GOLD, false);
			graphics.fill(TEXT_X, y + 10, TEXT_X + Math.max(60, this.font.width(text) + 8), y + 11, GOLD);
		}
		return y + 18;
	}

	private int subheading(GuiGraphics graphics, String text, int y) {
		if (y > 20 && y < this.height - 12) {
			graphics.drawString(this.font, text, TEXT_X, y, GOLD, false);
		}
		return y + LINE;
	}

	private int paragraph(GuiGraphics graphics, List<String> sentences, int y, int width) {
		for (String sentence : sentences) {
			for (String wrapped : wrap(sentence, width)) {
				y = line(graphics, wrapped, y, INK);
			}
			y += 6;
		}
		return y;
	}

	private int line(GuiGraphics graphics, String text, int y, int colour) {
		if (y > 20 && y < this.height - 12) {
			graphics.drawString(this.font, text, TEXT_X, y, colour, false);
		}
		return y + LINE;
	}

	private List<String> wrap(String text, int width) {
		ArrayList<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = current.isEmpty() ? word : current + " " + word;
			if (this.font.width(next) > width && !current.isEmpty()) {
				lines.add(current.toString());
				current = new StringBuilder(word);
			} else {
				current = new StringBuilder(next);
			}
		}
		if (!current.isEmpty()) {
			lines.add(current.toString());
		}
		return lines;
	}
}
