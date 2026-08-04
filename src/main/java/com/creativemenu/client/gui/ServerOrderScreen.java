package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ServerPrescriptionCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sortieren-Screen für den Server-Admin-Editor (Nutzer-Vorgabe: Vorschau + Sperren/Sortieren im
 * Server-Menü nachrüsten) - analog zu {@link SortScreen}, aber ohne Ein-/Ausblenden-Listen (der
 * Server blendet nichts aus, das bleibt Sache des einzelnen Spielers): eine einzige Seiten-Block-
 * Fläche aus echten Tabs + den Server-Vorschriften der aktuell im Editor gewählten OP-Stufe.
 * Kacheln gesperrter Vorschriften ({@code allowSort=false}) sind abgedunkelt und nicht greifbar -
 * ihre Position ist fix, echte Tabs sind immer frei verschiebbar. Schreibt beim Loslassen/Schließen
 * in {@link ServerPrescriptionCache#adminFull()}'s {@code orderByLevel} für die gewählte Stufe -
 * persistiert erst, wenn der Admin im Hauptbildschirm "Speichern" drückt.
 */
public class ServerOrderScreen extends FixedScaleScreen {

    private static final int TILE = 24;
    private static final int TILE_GAP = 2;
    private static final int BLOCK_HEADER_H = 12;
    private static final int BLOCK_GAP = 10;

    private final Screen parent;
    private final int opLevel;
    private final Runnable onSaved;

    private Map<String, ServerAdminRow> allRowsById = new HashMap<>();
    private List<String> activeOrder = new ArrayList<>();

    private int pageScrollPx = 0;
    private String draggingSlotId;

    private int listTop;
    private int listBottom;

    public ServerOrderScreen(Screen parent, int opLevel, Runnable onSaved) {
        super(Component.translatable("gui.creativemenu.editor.sort"));
        this.parent = parent;
        this.opLevel = opLevel;
        this.onSaved = onSaved;
    }

    @Override
    protected void initScaled() {
        listTop = 32;
        listBottom = height - 30;
        loadState();

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.close"), b -> onClose())
            .bounds(width / 2 - 75, height - 26, 150, 20).build());
    }

    private void loadState() {
        allRowsById = new HashMap<>();
        for (ServerAdminRow row : ServerAdminRowBuilder.realRows()) allRowsById.put(row.slotId(), row);
        for (ServerAdminRow row : ServerAdminRowBuilder.customRows(opLevel)) allRowsById.put(row.slotId(), row);
        for (ServerAdminRow row : ServerAdminRowBuilder.categoryRows(opLevel)) allRowsById.put(row.slotId(), row);

        activeOrder = new ArrayList<>();
        List<String> stored = ServerPrescriptionCache.adminFull().orderByLevel.getOrDefault(opLevel, List.of());
        for (String slot : stored) {
            if (allRowsById.containsKey(slot) && !activeOrder.contains(slot)) activeOrder.add(slot);
        }
        for (String slot : allRowsById.keySet()) {
            if (!activeOrder.contains(slot)) activeOrder.add(slot);
        }
    }

    private int totalPages() {
        int filled = (activeOrder.size() + 9) / 10;
        return Math.max(1, filled);
    }

    private int[] blockRect(int pageIndex) {
        int stackCol = pageIndex % 2;
        int stackRow = pageIndex / 2;
        int blockWidth = 5 * TILE + 4 * TILE_GAP;
        int colWidth = width / 2;
        int colX = stackCol == 0 ? 0 : colWidth;
        int blockX = colX + (colWidth - blockWidth) / 2;
        int blockHeight = BLOCK_HEADER_H + 4 + 2 * TILE + TILE_GAP;
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
        if (mouseY < listTop || mouseY >= listBottom) return null;
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

    @Override
    protected void renderScaled(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, net.minecraft.client.resources.language.I18n.get("gui.creativemenu.editor.editinglevel", opLevel),
            width / 2, 20, 0xFFAAAAAA);

        renderPages(guiGraphics, mouseX, mouseY);

        if (draggingSlotId != null) {
            ServerAdminRow row = allRowsById.get(draggingSlotId);
            if (row != null) {
                guiGraphics.fill(mouseX - 12, mouseY - 12, mouseX + 12, mouseY + 12, 0xC0222222);
                guiGraphics.renderItem(row.icon(), mouseX - 8, mouseY - 8);
            }
        }
    }

    private void renderPages(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.enableScissor(toRealX(0), toRealY(listTop), toRealX(width), toRealY(listBottom));
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
                        ServerAdminRow tileRow = allRowsById.get(activeOrder.get(idx));
                        if (tileRow != null) {
                            guiGraphics.renderItem(tileRow.icon(), r[0] + 4, r[1] + 4);
                            if (!tileRow.allowSort()) {
                                guiGraphics.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x90000000);
                            }
                            if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
                                guiGraphics.renderTooltip(font, tileRow.name(), mouseX, mouseY);
                            }
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
            int[] tile = hitTestTile(mouseX, mouseY);
            if (tile != null) {
                int idx = flatIndex(tile[0], tile[1], tile[2]);
                if (idx < activeOrder.size()) {
                    ServerAdminRow row = allRowsById.get(activeOrder.get(idx));
                    if (row != null && row.allowSort()) {
                        draggingSlotId = activeOrder.remove(idx);
                        return true;
                    }
                }
            }
        }
        return super.mouseClickedScaled(mouseX, mouseY, button);
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
            commitOrder();
            return true;
        }
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    private void commitOrder() {
        ServerPrescriptionCache.adminFull().orderByLevel.put(opLevel, new ArrayList<>(activeOrder));
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int stackRows = (totalPages() + 1) / 2;
        int blockHeight = BLOCK_HEADER_H + 4 + 2 * TILE + TILE_GAP;
        int totalPx = stackRows * (blockHeight + BLOCK_GAP);
        int maxScrollPx = Math.max(0, totalPx - (listBottom - listTop));
        pageScrollPx = Math.max(0, Math.min(maxScrollPx, pageScrollPx - (int) Math.signum(scrollY) * 20));
        return true;
    }

    @Override
    public void onClose() {
        commitOrder();
        if (onSaved != null) onSaved.run();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
