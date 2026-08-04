package com.creativemenu.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Zeigt eine nach Text gefilterte Kachel-Liste von Icons (Items oder Tabs) - ersetzt die
 * Autocomplete-Textliste für Icon-Auswahl (Nutzer-Vorgabe: "keine Vervollständigung, stattdessen
 * wird die Liste der Icons gefiltert"). Klick auf eine Kachel ruft {@link #onPick} auf; Tooltip mit
 * Anzeigename wird separat über {@link #renderTooltip} gezeichnet (muss von der Screen GANZ AM ENDE
 * ihres render() aufgerufen werden, gleiches Prinzip wie {@link AutocompleteEditBox}).
 */
public class IconGridWidget {

    public record Entry(String id, net.minecraft.world.item.ItemStack icon, String displayName) {}

    private static final int CELL = 18;

    private final int x, y, width, height;
    private final Supplier<List<Entry>> allEntries;
    private final Supplier<String> queryProvider;
    private final Consumer<String> onPick;

    private int scroll = 0;
    private List<Entry> filtered = List.of();
    private Entry hoveredEntry;

    public IconGridWidget(int x, int y, int width, int height, Supplier<List<Entry>> allEntries,
            Supplier<String> queryProvider, Consumer<String> onPick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.allEntries = allEntries;
        this.queryProvider = queryProvider;
        this.onPick = onPick;
    }

    private int columns() {
        return Math.max(1, width / CELL);
    }

    private int visibleRows() {
        return Math.max(1, height / CELL);
    }

    private void refresh() {
        String q = queryProvider.get();
        String ql = q == null ? "" : q.toLowerCase(Locale.ROOT);
        filtered = allEntries.get().stream()
            .filter(e -> ql.isBlank() || e.id().toLowerCase(Locale.ROOT).contains(ql)
                || e.displayName().toLowerCase(Locale.ROOT).contains(ql))
            .toList();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, filtered.size() - columns() * visibleRows())));
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        refresh();
        int cols = columns();
        int rows = visibleRows();
        int visible = cols * rows;

        guiGraphics.enableScissor(x, y, x + width, y + height);
        hoveredEntry = null;
        for (int i = 0; i < visible && i + scroll < filtered.size(); i++) {
            Entry entry = filtered.get(i + scroll);
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * CELL;
            int cy = y + row * CELL;

            boolean hovered = mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL;
            if (hovered) {
                guiGraphics.fill(cx, cy, cx + CELL, cy + CELL, 0x60FFFFFF);
                hoveredEntry = entry;
            }
            guiGraphics.renderItem(entry.icon(), cx + 1, cy + 1);
        }
        guiGraphics.disableScissor();
    }

    /** Von der Screen ganz am Ende ihres render() aufzurufen, damit der Tooltip über allem liegt. */
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredEntry != null) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                net.minecraft.network.chat.Component.literal(hoveredEntry.displayName()), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false;
        String id = tileAt(mouseX, mouseY);
        if (id != null && onPick != null) onPick.accept(id);
        return true;
    }

    /** Liefert die ID der Kachel unter der Maus, oder {@code null} - für Screens, die Klicks/Drag&Drop selbst steuern. */
    public String tileAt(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return null;
        int cols = columns();
        int col = (int) ((mouseX - x) / CELL);
        int row = (int) ((mouseY - y) / CELL);
        int idx = row * cols + col + scroll;
        if (col >= cols || idx < 0 || idx >= filtered.size()) return null;
        return filtered.get(idx).id();
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Ziel-Flach-Index für Drag&Drop-Umsortierung - anders als {@link #tileAt} auch für leere/außerhalb liegende Positionen (geclampt). */
    public int insertionIndexAt(double mouseX, double mouseY) {
        int cols = columns();
        int col = Math.max(0, Math.min(cols - 1, (int) ((mouseX - x) / CELL)));
        int row = Math.max(0, (int) ((mouseY - y) / CELL));
        return row * cols + col + scroll;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return false;
        int maxScroll = Math.max(0, filtered.size() - columns() * visibleRows());
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY) * columns()));
        return true;
    }
}
