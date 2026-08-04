package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ClientTabConfigManager;
import com.creativemenu.client.tabs.LocalTabLayout;
import com.creativemenu.client.tabs.ServerPrescriptionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sortieren-Menü: Seiten-Blöcke (5x2 Kacheln, wie das echte Kreativmenü) mit Text-Reflow-Drag&Drop,
 * plus links/rechts je eine Liste ausgeblendeter Tabs/Kategorien - reinziehen aktiviert (einblenden
 * an der Drop-Position), rausziehen in eine der beiden Listen deaktiviert (ausblenden) unabhängig
 * davon, in welche der beiden Listen man fallen lässt.
 *
 * Kern-Invariante: {@link #activeOrder} ist die einzige Quelle der Wahrheit während der Bearbeitung -
 * jeder Drag-Frame entfernt die gezogene Kachel (falls vorhanden) und fügt sie ggf. an der aktuell
 * überfahrenen Ziel-Position wieder ein ("Text-Reflow"). Persistiert wird erst bei Loslassen.
 */
public class SortScreen extends FixedScaleScreen {

    private static final int TILE = 24;
    private static final int TILE_GAP = 2;
    private static final int BLOCK_HEADER_H = 12;
    private static final int BLOCK_GAP = 10;
    private static final int ROW_H = 20;

    private final Screen parent;
    private final Runnable onSaved;

    private Map<String, TabRow> allRowsById = new HashMap<>();
    private List<String> activeOrder = new ArrayList<>();

    private EditBox searchTabs;
    private EditBox searchCategories;
    private int tabScroll = 0;
    private int categoryScroll = 0;
    private int pageScrollPx = 0;

    private String draggingSlotId;

    private int listTop;
    private int listBottom;

    // Nutzer-Vorgabe: globale Berechtigungsschwellen (siehe CreativeMenuPermissionConfig) - reine
    // Client-Komfort-Sperre, blockiert das AUFNEHMEN einer Kachel je nach Herkunft (Ausblenden-
    // Liste = Einblenden-Absicht, Raster = Umsortieren-Absicht).
    private final boolean canShowHide = Minecraft.getInstance().player.hasPermissions(ServerPrescriptionCache.minOpLevelShowHide());
    private final boolean canSort = Minecraft.getInstance().player.hasPermissions(ServerPrescriptionCache.minOpLevelSort());

    public SortScreen(Screen parent, Runnable onSaved) {
        super(Component.translatable("gui.creativemenu.editor.sort"));
        this.parent = parent;
        this.onSaved = onSaved;
    }

    @Override
    protected void initScaled() {
        listTop = 48;
        listBottom = height - 30;

        loadState();

        int q1x = q1X();
        int q4x = q4X();
        int qw = quarterWidth();

        Component searchLabel = Component.translatable("gui.creativemenu.editor.search");
        searchTabs = new EditBox(font, q1x + 6, 24, qw - 12, 18, searchLabel);
        searchTabs.setHint(searchLabel);
        addRenderableWidget(searchTabs);

        searchCategories = new EditBox(font, q4x + 6, 24, qw - 12, 18, searchLabel);
        searchCategories.setHint(searchLabel);
        addRenderableWidget(searchCategories);

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.close"), b -> onClose())
            .bounds(width / 2 - 75, height - 26, 150, 20).build());
    }

    private void loadState() {
        List<TabRow> rows = EditorTabRowBuilder.buildRows();
        allRowsById = new HashMap<>();
        activeOrder = new ArrayList<>();
        for (TabRow row : rows) {
            allRowsById.put(row.slotId(), row);
            if (!row.hidden()) activeOrder.add(row.slotId());
        }
    }

    private int quarterWidth() {
        return width / 4;
    }

    private int q1X() {
        return 0;
    }

    private int q2X() {
        return quarterWidth();
    }

    private int q3X() {
        return quarterWidth() * 2;
    }

    private int q4X() {
        return quarterWidth() * 3;
    }

    private int totalPages() {
        int filled = (activeOrder.size() + 9) / 10;
        return Math.max(1, filled) + 1;
    }

    private int[] blockRect(int pageIndex) {
        int stackCol = pageIndex % 2;
        int stackRow = pageIndex / 2;
        int blockWidth = 5 * TILE + 4 * TILE_GAP;
        int colX = stackCol == 0 ? q2X() : q3X();
        int blockX = colX + (quarterWidth() - blockWidth) / 2;
        int blockHeight = BLOCK_HEADER_H + 4 + 2 * TILE + TILE_GAP;
        // Nutzer-Fund: hier stand vorher eine von "listTop" losgelöste Konstante (26 statt 48) -
        // Block 0/1 ("Seite 1"/"Seite 2") begannen dadurch OBERHALB des Scissor-Clips von
        // renderPages/renderHiddenList (der bei listTop beginnt) und wurden größtenteils
        // weggeschnitten. Jetzt konsistent von listTop aus.
        int blockY = listTop + 2 + stackRow * (blockHeight + BLOCK_GAP) - pageScrollPx;
        return new int[] {blockX, blockY, blockWidth, blockHeight};
    }

    private int[] tileRect(int pageIndex, int row, int col) {
        int[] b = blockRect(pageIndex);
        int tx = b[0] + col * (TILE + TILE_GAP);
        int ty = b[1] + BLOCK_HEADER_H + 4 + row * (TILE + TILE_GAP);
        return new int[] {tx, ty, TILE, TILE};
    }

    private int flatIndex(int pageIndex, int row, int col) {
        return pageIndex * 10 + row * 5 + col;
    }

    private int[] hitTestTile(double mouseX, double mouseY) {
        if (mouseX < q2X() || mouseX >= q4X() || mouseY < listTop || mouseY >= listBottom) return null;
        for (int p = 0; p < totalPages(); p++) {
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 5; col++) {
                    int[] r = tileRect(p, row, col);
                    if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                        return new int[] {p, row, col};
                    }
                }
            }
        }
        return null;
    }

    private List<TabRow> hiddenRows(boolean category) {
        String query = (category ? searchCategories : searchTabs).getValue().toLowerCase(Locale.ROOT);
        List<TabRow> result = new ArrayList<>();
        for (TabRow row : allRowsById.values()) {
            if (activeOrder.contains(row.slotId())) continue;
            if (row.isCategory() != category) continue;
            if (!query.isBlank() && !row.name().getString().toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(row);
        }
        result.sort((a, b) -> a.name().getString().compareToIgnoreCase(b.name().getString()));
        return result;
    }

    @Override
    protected void renderScaled(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);

        renderHiddenList(guiGraphics, mouseX, mouseY, false);
        renderHiddenList(guiGraphics, mouseX, mouseY, true);
        renderPages(guiGraphics, mouseX, mouseY);

        if (draggingSlotId != null) {
            TabRow row = allRowsById.get(draggingSlotId);
            if (row != null) {
                guiGraphics.fill(mouseX - 12, mouseY - 12, mouseX + 12, mouseY + 12, 0xC0222222);
                guiGraphics.renderItem(row.icon(), mouseX - 8, mouseY - 8);
            }
        }
    }

    private void renderHiddenList(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean category) {
        int listX = category ? q4X() : q1X();
        int qw = quarterWidth();
        int scroll = category ? categoryScroll : tabScroll;
        List<TabRow> rows = hiddenRows(category);

        guiGraphics.enableScissor(toRealX(listX), toRealY(listTop), toRealX(listX + qw), toRealY(listBottom));
        int visibleRows = (listBottom - listTop) / ROW_H;
        for (int i = 0; i < visibleRows && i + scroll < rows.size(); i++) {
            TabRow row = rows.get(i + scroll);
            int rowY = listTop + i * ROW_H;
            int bg = (i + scroll) % 2 == 0 ? 0x40000000 : 0x20000000;
            guiGraphics.fill(listX + 2, rowY, listX + qw - 2, rowY + ROW_H - 2, bg);
            guiGraphics.renderItem(row.icon(), listX + 4, rowY + 2);
            guiGraphics.drawString(font, clipName(row.name().getString(), qw - 26), listX + 22, rowY + 5, 0xFFFFFF, false);
        }
        guiGraphics.disableScissor();
    }

    private String clipName(String name, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(name) <= maxWidth) return name;
        while (!name.isEmpty() && font.width(name + "..") > maxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        return name + "..";
    }

    private void renderPages(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.enableScissor(toRealX(q2X()), toRealY(listTop), toRealX(q4X()), toRealY(listBottom));
        for (int p = 0; p < totalPages(); p++) {
            int[] b = blockRect(p);
            if (b[1] + b[3] < listTop || b[1] > listBottom) continue;

            guiGraphics.drawString(font, Component.translatable("gui.creativemenu.editor.page", p + 1),
                b[0], b[1], 0xFFFFFF, false);

            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 5; col++) {
                    int[] r = tileRect(p, row, col);
                    int idx = flatIndex(p, row, col);
                    guiGraphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x50FFFFFF);
                    if (idx < activeOrder.size()) {
                        TabRow tabRow = allRowsById.get(activeOrder.get(idx));
                        if (tabRow != null) {
                            guiGraphics.renderItem(tabRow.icon(), r[0] + 4, r[1] + 4);
                        }
                    }
                }
            }
        }
        guiGraphics.disableScissor();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (canShowHide) {
                String hit = hitTestHiddenRow(mouseX, mouseY, false);
                if (hit == null) hit = hitTestHiddenRow(mouseX, mouseY, true);
                if (hit != null) {
                    draggingSlotId = hit;
                    return true;
                }
            }

            if (canSort) {
                int[] tile = hitTestTile(mouseX, mouseY);
                if (tile != null) {
                    int idx = flatIndex(tile[0], tile[1], tile[2]);
                    if (idx < activeOrder.size()) {
                        draggingSlotId = activeOrder.remove(idx);
                        return true;
                    }
                }
            }
        }
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    private String hitTestHiddenRow(double mouseX, double mouseY, boolean category) {
        int listX = category ? q4X() : q1X();
        int qw = quarterWidth();
        if (mouseX < listX || mouseX >= listX + qw || mouseY < listTop || mouseY >= listBottom) return null;
        int scroll = category ? categoryScroll : tabScroll;
        List<TabRow> rows = hiddenRows(category);
        int i = (int) ((mouseY - listTop) / ROW_H);
        int idx = i + scroll;
        if (idx < 0 || idx >= rows.size()) return null;
        return rows.get(idx).slotId();
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlotId != null) {
            activeOrder.remove(draggingSlotId);
            int[] tile = hitTestTile(mouseX, mouseY);
            if (tile != null) {
                int idx = Math.max(0, Math.min(activeOrder.size(), flatIndex(tile[0], tile[1], tile[2])));
                activeOrder.add(idx, draggingSlotId);
            }
            return true;
        }
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (draggingSlotId != null) {
            draggingSlotId = null;
            commit();
            return true;
        }
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    private void commit() {
        LocalTabLayout layout = ClientTabConfigManager.get();
        layout.order = new ArrayList<>(activeOrder);
        Set<String> hidden = new HashSet<>();
        for (String id : allRowsById.keySet()) {
            if (!activeOrder.contains(id)) hidden.add(id);
        }
        layout.hiddenIds = new ArrayList<>(hidden);
        ClientTabConfigManager.save();
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= q1X() && mouseX < q2X()) {
            int visibleRows = (listBottom - listTop) / ROW_H;
            int maxScroll = Math.max(0, hiddenRows(false).size() - visibleRows);
            tabScroll = Math.max(0, Math.min(maxScroll, tabScroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (mouseX >= q4X() && mouseX < q4X() + quarterWidth()) {
            int visibleRows = (listBottom - listTop) / ROW_H;
            int maxScroll = Math.max(0, hiddenRows(true).size() - visibleRows);
            categoryScroll = Math.max(0, Math.min(maxScroll, categoryScroll - (int) Math.signum(scrollY)));
            return true;
        }
        if (mouseX >= q2X() && mouseX < q4X()) {
            int stackRows = (totalPages() + 1) / 2;
            int blockHeight = BLOCK_HEADER_H + 4 + 2 * TILE + TILE_GAP;
            int totalPx = stackRows * (blockHeight + BLOCK_GAP);
            int maxScrollPx = Math.max(0, totalPx - (listBottom - listTop));
            pageScrollPx = Math.max(0, Math.min(maxScrollPx, pageScrollPx - (int) Math.signum(scrollY) * 20));
            return true;
        }
        return super.mouseScrolledScaled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (onSaved != null) onSaved.run();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
