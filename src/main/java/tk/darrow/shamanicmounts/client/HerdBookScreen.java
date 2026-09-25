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
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import tk.darrow.shamanicmounts.book.Codex;
import tk.darrow.shamanicmounts.book.Genotype;
import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.book.Reading;
import tk.darrow.shamanicmounts.book.SpoilerPref;
import tk.darrow.shamanicmounts.book.TamePage;
import tk.darrow.shamanicmounts.entity.MountEntities;
import tk.darrow.shamanicmounts.entity.ShamanicMount;
import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.MountSize;

/**
 * The herd book. Basics; a gallery of the lines, each opening onto its three pelts and founder genes;
 * your tames, each with a portrait, family links, and every gene with both copies; the key to the
 * notation; and, with spoilers on, the breeding chapter. The page scrolls inside its own frame.
 */
public final class HerdBookScreen extends Screen {
	private static final int LEFT = 8;
	private static final int LEFT_W = 110;
	private static final int TEXT_X = 132;
	private static final int RIGHT = 8;
	private static final int TOP = 24;
	private static final int LINE = 11;
	private static final int ROW = 14;
	private static final int INK = 0xFFE6D7B8;
	private static final int PALE = 0xFFA99B80;
	private static final int GOLD = 0xFFE8C16A;
	private static final int LINK = 0xFF9FD3C7;
	private static final int PAGE = 0xF01C160F;
	private static final int WELL = 0xFF2A2218;
	private static final int WELL_EDGE = 0xFF5A4A34;
	private static final int HOVER = 0x30E8C16A;
	private static final int PORTRAIT_W = 96;
	private static final int PORTRAIT_H = 104;
	private static final int GALLERY_W = 108;
	private static final int GALLERY_H = 72;

	/** A clickable area drawn this frame. */
	private record Hit(int x0, int y0, int x1, int y1, Runnable action) {
		boolean contains(double x, double y) {
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}

	private final UUID player;
	private final HerdBook book;
	private final SpoilerPref spoilers;
	private final Map<UUID, ShamanicMount> previews = new HashMap<>();
	private final Map<String, ShamanicMount> linePreviews = new HashMap<>();
	private final List<Hit> hits = new ArrayList<>();
	private Codex.Page page = Codex.Page.BASICS;
	@Nullable private UUID selected;
	@Nullable private String selectedLine;
	private double scroll;
	private int maxScroll;
	private boolean confirmRelease;
	@Nullable private EditBox nameBox;
	@Nullable private List<FormattedCharSequence> tooltip;
	/** Work the page would otherwise redo every frame: wrapped lines, tame summaries, gene rows, line bodies. */
	private final Map<String, List<String>> wraps = new HashMap<>();
	private final Map<UUID, String> summaries = new HashMap<>();
	private final Map<Genome, List<TamePage.Row>> geneRows = new java.util.IdentityHashMap<>();
	private final Map<String, MountSize.Form> lineForms = new HashMap<>();
	/** On a very narrow window Rename moves to the left column with Release. */
	private boolean renameLeft;

	public HerdBookScreen(UUID player, HerdBook book, SpoilerPref spoilers) {
		super(Component.translatable("item.shamanicmounts.herd_book"));
		this.player = player;
		this.book = book;
		this.spoilers = spoilers;
	}

	private boolean spoiled() {
		return spoilers.shown(player);
	}

	@Nullable
	private HerdBook.Entry selectedEntry() {
		return selected == null ? null : book.get(selected);
	}

	private boolean ownsSelected() {
		HerdBook.Entry tame = selectedEntry();
		return tame != null && tame.tame() && player.equals(tame.owner());
	}

	private void open(Codex.Page chapter) {
		nameBox = null;
		page = chapter;
		selected = null;
		selectedLine = null;
		confirmRelease = false;
		scroll = 0;
		rebuildWidgets();
	}

	private void openTame(@Nullable UUID id) {
		nameBox = null;
		page = Codex.Page.TAMES;
		selected = id;
		selectedLine = null;
		confirmRelease = false;
		scroll = 0;
		rebuildWidgets();
	}

	private void clearCaches() {
		wraps.clear();
		summaries.clear();
		geneRows.clear();
		lineForms.clear();
	}

	private String summary(HerdBook.Entry entry) {
		return summaries.computeIfAbsent(entry.id(), id -> TamePage.summary(entry.genome(), entry.male()));
	}

	private List<TamePage.Row> rows(Genome genome) {
		return geneRows.computeIfAbsent(genome, key -> TamePage.genes(key, spoiled()));
	}

	@Override
	protected void init() {
		clearCaches();
		if (page == Codex.Page.BREEDING && !spoiled()) {
			page = Codex.Page.BASICS;
		}
		// A spoiler line does not stay open once spoilers are off.
		if (selectedLine != null && !spoiled()) {
			Codex.Line open = line(selectedLine);
			if (open == null || open.spoiler()) {
				selectedLine = null;
			}
		}
		// Keep whatever was typed in the name box across a rebuild.
		String typed = nameBox != null && selectedEntry() != null ? nameBox.getValue() : null;
		int y = 28;
		int chapters = Codex.open(spoiled()).size();
		for (Codex.Page chapter : Codex.open(spoiled())) {
			Codex.Page choice = chapter;
			addRenderableWidget(Button.builder(Component.translatable("book.shamanicmounts." + chapter.name().toLowerCase()),
					button -> open(choice)).bounds(LEFT, y, LEFT_W, 20).build());
			y += 22;
		}
		addRenderableWidget(Button.builder(Component.translatable(spoiled()
				? "book.shamanicmounts.spoilers_on" : "book.shamanicmounts.spoilers_off"), button -> {
					spoilers.set(player, !spoiled());
					scroll = 0;
					rebuildWidgets();
				}).bounds(LEFT, this.height - 28, LEFT_W, 20).build());

		nameBox = null;
		boolean detail = (page == Codex.Page.TAMES && selectedEntry() != null)
				|| (page == Codex.Page.LINES && selectedLine != null);
		if (detail) {
			addRenderableWidget(Button.builder(Component.translatable("book.shamanicmounts.back"), button -> {
				if (page == Codex.Page.LINES) {
					open(Codex.Page.LINES);
				} else {
					openTame(null);
				}
			}).bounds(TEXT_X, 28, 60, 20).build());
		}
		if (page == Codex.Page.TAMES && ownsSelected()) {
			HerdBook.Entry tame = selectedEntry();
			// Back, the name, and Rename share the top row; Release waits in the left column, away from Rename.
			int room = this.width - RIGHT - 8 - TEXT_X;
			renameLeft = room - 66 - 60 < 60;
			int boxW = renameLeft ? Math.max(40, Math.min(140, room - 66)) : Math.min(140, room - 66 - 60);
			nameBox = new EditBox(this.font, TEXT_X + 66, 29, boxW, 18, Component.translatable("book.shamanicmounts.rename"));
			nameBox.setMaxLength(24);
			nameBox.setValue(typed != null ? typed : tame.name());
			addRenderableWidget(nameBox);
			addRenderableWidget(Button.builder(Component.translatable("book.shamanicmounts.rename"), button -> {
				rename();
			}).bounds(renameLeft ? LEFT : TEXT_X + 70 + boxW, renameLeft ? 28 + chapters * 22 + 34 : 29, renameLeft ? LEFT_W : 56,
					renameLeft ? 20 : 18).build());
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
						openTame(null);
					}).bounds(LEFT, 28 + chapters * 22 + 10, LEFT_W, 20).build());
		}
	}

	/** Enter in the name box renames the tame, the same as the Rename button. */
	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (nameBox != null && nameBox.isFocused()
				&& (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
			rename();
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	private void rename() {
		if (nameBox != null && selected != null && book.rename(player, selected, nameBox.getValue())) {
			confirmRelease = false;
			ClientBook.tellServer(0, selected, book.get(selected).name());
			nameBox = null;
			rebuildWidgets();
		}
	}

	/** Where the scrolling page starts: under the Back row on a detail page, else under the top edge. */
	private int viewTop() {
		boolean detail = (page == Codex.Page.TAMES && selectedEntry() != null)
				|| (page == Codex.Page.LINES && selectedLine != null);
		return detail ? 54 : 32;
	}

	private int viewBottom() {
		return this.height - 12;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = Math.max(0, Math.min(maxScroll, scroll - scrollY * 16));
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (button == 0 && mouseY >= viewTop() - 4 && mouseY < viewBottom()) {
			for (Hit hit : List.copyOf(hits)) {
				if (hit.contains(mouseX, mouseY)) {
					hit.action().run();
					return true;
				}
			}
		}
		return false;
	}

	/** The page fill is drawn in the background pass, under the widgets. */
	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(TEXT_X - 8, TOP, this.width - RIGHT, this.height - 8, PAGE);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		hits.clear();
		tooltip = null;
		int top = viewTop();
		boolean inView = mouseY >= top - 4 && mouseY < viewBottom();
		int mx = inView ? mouseX : -1;
		int my = inView ? mouseY : -1;
		graphics.enableScissor(TEXT_X - 8, top - 4, this.width - RIGHT, viewBottom());
		int end = drawPage(graphics, mx, my, top - (int) scroll);
		graphics.disableScissor();
		maxScroll = Math.max(0, end + (int) scroll - viewBottom() + 8);
		if (scroll > maxScroll) {
			scroll = maxScroll;
		}
		if (maxScroll > 0) {
			scrollbar(graphics, top);
		}
		if (tooltip != null) {
			graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
		}
	}

	private void scrollbar(GuiGraphics graphics, int top) {
		int x = this.width - RIGHT - 4;
		int track = viewBottom() - top;
		int view = track;
		int total = view + maxScroll;
		int thumb = Math.max(16, track * view / total);
		int at = top + (int) ((track - thumb) * (scroll / maxScroll));
		graphics.fill(x, top, x + 2, top + track, WELL);
		graphics.fill(x, at, x + 2, at + thumb, WELL_EDGE);
	}

	/** Draws the open page from {@code y} and returns where it ended. */
	private int drawPage(GuiGraphics graphics, int mouseX, int mouseY, int y) {
		int width = this.width - TEXT_X - RIGHT - 14;
		return switch (page) {
			case BASICS -> sections(graphics, "Basics", Codex.basics(), y, width);
			case BREEDING -> sections(graphics, "Breeding", Codex.breeding(), y, width);
			case KEY -> key(graphics, y, width);
			case LINES -> selectedLine == null ? gallery(graphics, mouseX, mouseY, y, width)
					: lineDetail(graphics, mouseX, mouseY, y, width);
			case TAMES -> selectedEntry() == null ? tameList(graphics, mouseX, mouseY, y, width)
					: tamePage(graphics, mouseX, mouseY, y, width);
		};
	}

	private int sections(GuiGraphics graphics, String title, List<Codex.Section> sections, int y, int width) {
		y = heading(graphics, title, y);
		for (Codex.Section section : sections) {
			y = subheading(graphics, section.heading(), y);
			y = paragraph(graphics, section.paragraphs(), y, width) + 4;
		}
		return y;
	}

	private int key(GuiGraphics graphics, int y, int width) {
		y = heading(graphics, "Key", y);
		y = paragraph(graphics, Codex.keyIntro(), y, width) + 2;
		y = subheading(graphics, "Body genes", y);
		for (Genotype.KeyEntry entry : Genotype.bodyKey()) {
			y = keyEntry(graphics, entry, y, width);
		}
		if (spoiled()) {
			y = subheading(graphics, "Ridden gifts", y + 2);
			for (Genotype.KeyEntry entry : Genotype.giftKey()) {
				y = keyEntry(graphics, entry, y, width);
			}
		}
		return y;
	}

	private int keyEntry(GuiGraphics graphics, Genotype.KeyEntry entry, int y, int width) {
		graphics.drawString(this.font, entry.symbol(), TEXT_X, y, GOLD, false);
		graphics.drawString(this.font, entry.name(), TEXT_X + 18, y, INK, false);
		int x = TEXT_X + 70;
		for (String wrapped : wrap(entry.letters(), width - 70)) {
			graphics.drawString(this.font, wrapped, x, y, INK, false);
			y += LINE;
		}
		for (String wrapped : wrap(entry.rule(), width - 70)) {
			graphics.drawString(this.font, wrapped, x, y, PALE, false);
			y += LINE;
		}
		return y + 4;
	}

	/** The founders in a grid, each standing in its own well over its name and a line about it. */
	private int gallery(GuiGraphics graphics, int mouseX, int mouseY, int y, int width) {
		y = heading(graphics, "Lines", y);
		y = paragraph(graphics, List.of("Click a line for its three pelts and its founder's genes."), y, width) + 2;
		int columns = Math.max(1, (width + 8) / (GALLERY_W + 8));
		List<Codex.Line> lines = Codex.lines();
		boolean spoiled = spoiled();
		int rowTop = y;
		for (int start = 0; start < lines.size(); start += columns) {
			int rowHeight = 0;
			for (int column = 0; column < columns && start + column < lines.size(); column++) {
				Codex.Line entry = lines.get(start + column);
				boolean hidden = entry.spoiler() && !spoiled;
				int x = TEXT_X + column * (GALLERY_W + 8);
				boolean hover = !hidden && mouseX >= x && mouseX < x + GALLERY_W && mouseY >= rowTop && mouseY < rowTop + GALLERY_H;
				well(graphics, x, rowTop, GALLERY_W, GALLERY_H, hover);
				if (!hidden) {
					ShamanicMount preview = linePreview(entry.name(), entry.genome(), -1);
					if (preview != null) {
						portrait(graphics, preview, x, rowTop, GALLERY_W, GALLERY_H, -1, -1);
					}
					String name = entry.name();
					hits.add(new Hit(x, rowTop, x + GALLERY_W, rowTop + GALLERY_H, () -> openLine(name)));
				}
				int textY = rowTop + GALLERY_H + 3;
				graphics.drawString(this.font, hidden ? "?" : entry.name(), x + 2, textY, GOLD, false);
				int blurbY = textY + LINE;
				for (String wrapped : wrap(hidden ? Codex.HIDDEN_LINE : entry.blurb(), GALLERY_W - 2)) {
					graphics.drawString(this.font, wrapped, x + 2, blurbY, PALE, false);
					blurbY += 10;
				}
				rowHeight = Math.max(rowHeight, blurbY - rowTop);
			}
			rowTop += rowHeight + 10;
		}
		return rowTop;
	}

	private void openLine(String name) {
		selectedLine = name;
		scroll = 0;
		rebuildWidgets();
	}

	@Nullable
	private Codex.Line line(String name) {
		for (Codex.Line entry : Codex.lines()) {
			if (entry.name().equals(name)) {
				return entry;
			}
		}
		return null;
	}

	/** One founder line: a large portrait, its three pelts, and the founder's genes. */
	private int lineDetail(GuiGraphics graphics, int mouseX, int mouseY, int y, int width) {
		Codex.Line entry = line(selectedLine);
		if (entry == null) {
			return y;
		}
		y = heading(graphics, entry.name(), y);
		y = paragraph(graphics, List.of(entry.blurb()), y, width) + 2;
		MountSize.Form form = lineForms.computeIfAbsent(entry.name(), name -> MountSize.form(Expression.express(entry.genome())));
		List<String> pelts = TamePage.pelts(form);
		y = subheading(graphics, "Pelts: A over B over C", y);
		int wellW = Math.max(60, Math.min(120, (width - 16) / 3));
		int wellH = wellW * 5 / 6;
		for (int pelt = 0; pelt < 3; pelt++) {
			int x = TEXT_X + pelt * (wellW + 8);
			boolean hover = mouseX >= x && mouseX < x + wellW && mouseY >= y && mouseY < y + wellH;
			well(graphics, x, y, wellW, wellH, false);
			ShamanicMount preview = linePreview(entry.name(), entry.genome(), pelt);
			if (preview != null) {
				portrait(graphics, preview, x, y, wellW, wellH, hover ? mouseX : -1, hover ? mouseY : -1);
			}
			String label = (char) ('A' + pelt) + "  " + pelts.get(pelt);
			graphics.drawString(this.font, label, x + 2, y + wellH + 3, pelt == 0 ? GOLD : INK, false);
			String need = pelt == 0 ? "one copy" : pelt == 1 ? "no A copy" : "two C copies";
			graphics.drawString(this.font, need, x + 2, y + wellH + 3 + LINE, PALE, false);
		}
		y += wellH + 3 + LINE * 2 + 6;
		y = subheading(graphics, "Founder genes", y);
		return geneTable(graphics, entry.genome(), mouseX, mouseY, y, width);
	}

	/** Every tame this player owns, one row each, with what it is. */
	private int tameList(GuiGraphics graphics, int mouseX, int mouseY, int y, int width) {
		y = heading(graphics, "Tames", y);
		List<HerdBook.Entry> tames = book.tames(player);
		if (tames.isEmpty()) {
			return paragraph(graphics, List.of("No tames in the book yet. Tame a wild adult with a Shamanic Saddle."), y, width);
		}
		int nameW = 0;
		for (HerdBook.Entry entry : tames) {
			nameW = Math.max(nameW, this.font.width(entry.name()));
		}
		nameW = Math.min(nameW + 10, width / 3);
		for (HerdBook.Entry entry : tames) {
			boolean hover = mouseY >= y - 2 && mouseY < y + ROW - 2 && mouseX >= TEXT_X && mouseX < TEXT_X + width;
			if (hover) {
				graphics.fill(TEXT_X - 4, y - 2, TEXT_X + width, y + ROW - 2, HOVER);
			}
			graphics.drawString(this.font, this.font.plainSubstrByWidth(entry.name(), nameW - 6), TEXT_X, y, GOLD, false);
			String summary = summary(entry);
			graphics.drawString(this.font, this.font.plainSubstrByWidth(summary, width - nameW), TEXT_X + nameW, y, PALE, false);
			UUID id = entry.id();
			hits.add(new Hit(TEXT_X - 4, y - 2, TEXT_X + width, y + ROW - 2, () -> openTame(id)));
			y += ROW;
		}
		return y;
	}

	/** One tame: its name and summary beside its portrait, family links, then every gene. */
	private int tamePage(GuiGraphics graphics, int mouseX, int mouseY, int y, int width) {
		HerdBook.Entry entry = selectedEntry();
		if (entry == null) {
			return y;
		}
		int wellW = Math.min(PORTRAIT_W, width / 3);
		int wellX = TEXT_X + width - wellW;
		int textW = width - wellW - 10;
		int top = y;
		if (wellW >= 40) {
			well(graphics, wellX, y, wellW, PORTRAIT_H, false);
			ShamanicMount preview = preview(entry);
			boolean hover = mouseX >= wellX && mouseX < wellX + wellW && mouseY >= y && mouseY < y + PORTRAIT_H;
			if (preview != null) {
				portrait(graphics, preview, wellX, y, wellW, PORTRAIT_H, hover ? mouseX : -1, hover ? mouseY : -1);
			}
		}
		y = heading(graphics, entry.name(), y);
		for (String wrapped : wrap(summary(entry), textW)) {
			graphics.drawString(this.font, wrapped, TEXT_X, y, INK, false);
			y += LINE;
		}
		String wings = TamePage.wings(entry.genome());
		if (!wings.isEmpty()) {
			graphics.drawString(this.font, wings, TEXT_X, y, INK, false);
			y += LINE;
		}
		if (!entry.tame() || !player.equals(entry.owner())) {
			graphics.drawString(this.font, "Not your tame: read only.", TEXT_X, y, PALE, false);
			y += LINE;
		}
		y += 4;
		y = family(graphics, entry, mouseX, mouseY, y, textW);
		y = Math.max(y, top + PORTRAIT_H + 6);
		y = subheading(graphics, "Genes", y);
		return geneTable(graphics, entry.genome(), mouseX, mouseY, y, width);
	}

	/** Dam, sire, and foals; every relative in the book is a link to its page. */
	private int family(GuiGraphics graphics, HerdBook.Entry entry, int mouseX, int mouseY, int y, int width) {
		List<TamePage.Kin> kin = TamePage.kin(book, entry);
		int x = TEXT_X;
		String relation = "";
		for (TamePage.Kin one : kin) {
			if (!one.relation().equals(relation)) {
				if (!relation.isEmpty()) {
					y += LINE;
				}
				relation = one.relation();
				String label = "Foal".equals(relation) ? "Foals: " : relation + ": ";
				graphics.drawString(this.font, label, TEXT_X, y, INK, false);
				x = TEXT_X + this.font.width(label);
			} else {
				graphics.drawString(this.font, ", ", x, y, INK, false);
				x += this.font.width(", ");
			}
			int w = this.font.width(one.name());
			if (x + w > TEXT_X + width && x > TEXT_X + 40) {
				y += LINE;
				x = TEXT_X + 12;
			}
			boolean link = one.id() != null && book.get(one.id()) != null;
			boolean hover = link && mouseX >= x && mouseX < x + w && mouseY >= y - 1 && mouseY < y + 9;
			graphics.drawString(this.font, one.name(), x, y, link ? LINK : PALE, false);
			if (hover) {
				graphics.fill(x, y + 9, x + w, y + 10, LINK);
			}
			if (link) {
				UUID id = one.id();
				hits.add(new Hit(x, y - 1, x + w, y + 9, () -> openTame(id)));
			}
			x += w;
		}
		if (kin.stream().noneMatch(one -> "Foal".equals(one.relation()))) {
			y += LINE;
			graphics.drawString(this.font, "Foals: none", TEXT_X, y, INK, false);
		}
		return y + LINE + 4;
	}

	/**
	 * Every gene, grouped, with columns for the notation, what shows, and each parent's copy. The
	 * note column appears when there is room and spoilers are on. Hovering a row explains the gene.
	 */
	private int geneTable(GuiGraphics graphics, Genome genome, int mouseX, int mouseY, int y, int width) {
		boolean showNotes = spoiled();
		// Columns share the page's width, so a narrow window keeps every column on the page.
		int colShown = Math.min(64, width / 4);
		int room = Math.max(60, width - colShown);
		boolean noteFits = room >= 300;
		int colDam = colShown + room * (noteFits ? 30 : 38) / 100;
		int colSire = colDam + room * (noteFits ? 22 : 31) / 100;
		int colNote = colSire + room * 22 / 100;
		graphics.drawString(this.font, "gene", TEXT_X, y, PALE, false);
		graphics.drawString(this.font, "shows", TEXT_X + colShown, y, PALE, false);
		graphics.drawString(this.font, "dam", TEXT_X + colDam, y, PALE, false);
		graphics.drawString(this.font, "sire", TEXT_X + colSire, y, PALE, false);
		if (showNotes && noteFits) {
			graphics.drawString(this.font, "note", TEXT_X + colNote, y, PALE, false);
		}
		y += LINE + 2;
		Reading.Group group = null;
		for (TamePage.Row row : rows(genome)) {
			if (row.group() != group) {
				group = row.group();
				graphics.drawString(this.font, group.title, TEXT_X, y + 2, GOLD, false);
				graphics.fill(TEXT_X, y + 12, TEXT_X + width, y + 13, WELL_EDGE);
				y += LINE + 6;
			}
			boolean hover = mouseY >= y - 1 && mouseY < y + LINE - 1 && mouseX >= TEXT_X && mouseX < TEXT_X + width;
			if (hover) {
				graphics.fill(TEXT_X - 4, y - 1, TEXT_X + width, y + LINE - 1, HOVER);
				ArrayList<FormattedCharSequence> tip = new ArrayList<>();
				tip.addAll(this.font.split(Component.literal(row.symbol() + " " + row.locus()), 220));
				tip.addAll(this.font.split(Component.literal(row.rule()), 220));
				if (!row.note().isEmpty()) {
					tip.addAll(this.font.split(Component.literal("Here: " + row.note()), 220));
				}
				tooltip = tip;
			}
			graphics.drawString(this.font, row.symbol() + " " + row.notation(), TEXT_X, y, GOLD, false);
			graphics.drawString(this.font, clip(row.shown(), colDam - colShown - 4), TEXT_X + colShown, y, INK, false);
			graphics.drawString(this.font, clip(row.dam(), colSire - colDam - 4), TEXT_X + colDam, y, PALE, false);
			graphics.drawString(this.font, clip(row.sire(), (noteFits ? colNote : width) - colSire - 4), TEXT_X + colSire, y, PALE, false);
			if (showNotes && noteFits && !row.note().isEmpty()) {
				graphics.drawString(this.font, clip(row.note(), width - colNote), TEXT_X + colNote, y, LINK, false);
			}
			y += LINE;
		}
		return y;
	}

	private String clip(String text, int width) {
		return this.font.plainSubstrByWidth(text, Math.max(8, width));
	}

	private void well(GuiGraphics graphics, int x, int y, int w, int h, boolean hover) {
		graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, hover ? GOLD : WELL_EDGE);
		graphics.fill(x, y, x + w, y + h, WELL);
	}

	private static void portrait(GuiGraphics graphics, ShamanicMount preview, int x, int y, int w, int h, int mouseX,
			int mouseY) {
		// Antlers, necks, and frills stand above the hitbox, so the portrait leaves them a fifth more room.
		int scale = Math.max(8, Math.round((h - 12) * 0.8f / Math.max(1f, preview.getBbHeight() / preview.getScale())));
		scale = Math.min(scale, Math.round((w - 8) * 0.8f / Math.max(1f, preview.getBbWidth() * 1.6f)));
		// A three-quarter view by default, so eyes set in the side of the head face the reader. Hovering
		// lets it follow the mouse instead.
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		int lookX = hover ? mouseX : x + w / 2 - 60;
		int lookY = hover ? mouseY : y + h / 2 + 10;
		InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, x + w, y + h, Math.max(8, scale), 0.0625f,
				lookX, lookY, preview);
	}

	/** Closing the book drops every preview, so nothing of it stays behind. */
	@Override
	public void removed() {
		super.removed();
		clearCaches();
		previews.clear();
		linePreviews.clear();
	}

	/** A client-side copy of the tame, built from its genome and never added to the world. */
	@Nullable
	private ShamanicMount preview(HerdBook.Entry entry) {
		return previews.computeIfAbsent(entry.id(), id -> build(entry.genome()));
	}

	/** A founder, in its own pelt ({@code -1}) or with both pelt copies set to one pelt. */
	@Nullable
	private ShamanicMount linePreview(String name, Genome genome, int pelt) {
		return linePreviews.computeIfAbsent(name + "#" + pelt, key -> build(pelt < 0 ? genome : withPelt(genome, pelt)));
	}

	private static Genome withPelt(Genome genome, int pelt) {
		Marks.Pelt allele = Marks.Pelt.values()[pelt];
		return new Genome(genome.maternal.with(allele), genome.paternal.with(allele), genome.headFromMaternal,
				genome.footFromMaternal, genome.tailFromMaternal, genome.chimera);
	}

	@Nullable
	private static ShamanicMount build(Genome genome) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return null;
		}
		ShamanicMount mount = MountEntities.MOUNT.get().create(minecraft.level);
		if (mount == null) {
			return null;
		}
		mount.setGenome(genome, true);
		return mount;
	}

	/** The open page as plain lines, in the same words it draws, for the live harness. */
	private List<String> pageLines() {
		ArrayList<String> out = new ArrayList<>();
		switch (page) {
			case BASICS -> Codex.basics().forEach(section -> out.addAll(section.paragraphs()));
			case BREEDING -> Codex.breeding().forEach(section -> out.addAll(section.paragraphs()));
			case KEY -> {
				out.addAll(Codex.keyIntro());
				for (Genotype.KeyEntry entry : Genotype.bodyKey()) {
					out.add(entry.symbol() + " " + entry.name() + ": " + entry.letters() + ". " + entry.rule());
				}
				if (spoiled()) {
					for (Genotype.KeyEntry entry : Genotype.giftKey()) {
						out.add(entry.symbol() + " " + entry.name() + ": " + entry.letters() + ". " + entry.rule());
					}
				}
			}
			case LINES -> {
				if (selectedLine == null) {
					for (Codex.Line entry : Codex.lines()) {
						out.add(entry.name() + ": " + (entry.spoiler() && !spoiled() ? Codex.HIDDEN_LINE : entry.blurb()));
					}
				} else {
					Codex.Line entry = line(selectedLine);
					if (entry != null) {
						out.add(entry.name() + ": " + entry.blurb());
						out.add("Pelts: " + String.join(", ", TamePage.pelts(MountSize.form(Expression.express(entry.genome())))));
						out.addAll(geneLines(entry.genome()));
					}
				}
			}
			case TAMES -> {
				HerdBook.Entry entry = selectedEntry();
				if (entry == null) {
					List<HerdBook.Entry> tames = book.tames(player);
					if (tames.isEmpty()) {
						out.add("No tames in the book yet.");
					}
					for (HerdBook.Entry tame : tames) {
						out.add(tame.name() + ": " + TamePage.summary(tame.genome(), tame.male()));
					}
				} else {
					out.add(TamePage.summary(entry.genome(), entry.male()));
					out.addAll(TamePage.family(book, entry));
					out.add("Pelt: " + TamePage.pelt(entry.genome()));
					out.addAll(geneLines(entry.genome()));
				}
			}
		}
		return out;
	}

	private List<String> geneLines(Genome genome) {
		ArrayList<String> out = new ArrayList<>();
		boolean showNotes = spoiled();
		for (TamePage.Row row : TamePage.genes(genome, showNotes)) {
			String text = row.symbol() + " " + row.notation() + "  " + row.shown() + "  dam " + row.dam() + " sire " + row.sire();
			if (showNotes && !row.note().isEmpty()) {
				text = text + "  " + row.note();
			}
			out.add(text);
		}
		return out;
	}

	/** One line for the live harness: page, spoilers, tame names, and the lines on the page. */
	String harnessReport() {
		StringBuilder out = new StringBuilder();
		out.append("page=").append(page.name());
		out.append("|spoilers=").append(spoiled());
		out.append("|tames=");
		boolean first = true;
		for (HerdBook.Entry entry : book.tames(player)) {
			if (!first) {
				out.append(',');
			}
			first = false;
			out.append(entry.name());
		}
		HerdBook.Entry selectedEntry = selectedEntry();
		out.append("|selected=").append(selectedEntry == null ? "" : selectedEntry.name());
		out.append("|line=").append(selectedLine == null ? "" : selectedLine);
		out.append("|confirm=").append(confirmRelease);
		out.append("|scroll=").append((int) scroll).append('/').append(maxScroll);
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

	/**
	 * The harness picks row {@code index}: a tame on the tame list, a line in the gallery, or, on a
	 * tame's page, a relative (0 dam, 1 sire, then foals).
	 */
	boolean harnessPick(int index) {
		if (page == Codex.Page.TAMES && selectedEntry() == null) {
			List<HerdBook.Entry> tames = book.tames(player);
			if (index >= 0 && index < tames.size()) {
				openTame(tames.get(index).id());
				return true;
			}
			return false;
		}
		if (page == Codex.Page.TAMES) {
			List<TamePage.Kin> kin = TamePage.kin(book, selectedEntry());
			if (index >= 0 && index < kin.size() && kin.get(index).id() != null && book.get(kin.get(index).id()) != null) {
				openTame(kin.get(index).id());
				return true;
			}
			return false;
		}
		if (page == Codex.Page.LINES && selectedLine == null) {
			List<Codex.Line> lines = Codex.lines();
			if (index >= 0 && index < lines.size() && (!lines.get(index).spoiler() || spoiled())) {
				openLine(lines.get(index).name());
				return true;
			}
		}
		return false;
	}

	/** The harness scrolls the page to its end, or back to the top. */
	void harnessScroll(boolean end) {
		scroll = end ? maxScroll : 0;
	}

	/** The harness presses Enter in the name box, as a player would after typing. */
	String harnessEnter() {
		if (nameBox == null) {
			return "error no name box";
		}
		setFocused(nameBox);
		nameBox.setFocused(true);
		keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0);
		return "ok";
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
		graphics.drawString(this.font, text, TEXT_X, y, GOLD, false);
		graphics.fill(TEXT_X, y + 10, TEXT_X + Math.max(60, this.font.width(text) + 8), y + 11, GOLD);
		return y + 18;
	}

	private int subheading(GuiGraphics graphics, String text, int y) {
		graphics.drawString(this.font, text, TEXT_X, y, GOLD, false);
		return y + LINE + 3;
	}

	private int paragraph(GuiGraphics graphics, List<String> sentences, int y, int width) {
		for (String sentence : sentences) {
			for (String wrapped : wrap(sentence, width)) {
				graphics.drawString(this.font, wrapped, TEXT_X, y, INK, false);
				y += LINE;
			}
			y += 4;
		}
		return y;
	}

	private List<String> wrap(String text, int width) {
		return wraps.computeIfAbsent(width + "|" + text, key -> wrapNow(text, width));
	}

	private List<String> wrapNow(String text, int width) {
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
