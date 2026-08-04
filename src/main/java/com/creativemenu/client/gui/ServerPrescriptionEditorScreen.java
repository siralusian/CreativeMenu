package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ServerPrescriptionCache;
import com.creativemenu.network.ServerPrescriptionSaveRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin-Bearbeitungsmenü für serverseitig vorgeschriebene Custom-Tabs/Kategorien - geöffnet über
 * {@code /creativemenu server} (siehe {@link com.creativemenu.commands.CreativeMenuCommands}),
 * arbeitet auf dem vollen (ungefilterten) {@link ServerPrescriptionCache#adminFull()}-Satz.
 * Nutzer-Vorgabe: die Sitzung arbeitet je Sitzung im Kontext EINER OP-Stufe (Wähler oben, siehe
 * {@link OpLevelWidget}), Auswahl filtert beide Listen; "Neuer Server-Tab"/"Neue Server-Kategorie"
 * stempelt neue Vorschriften mit der gewählten Stufe. "Sortieren" öffnet {@link ServerOrderScreen}
 * (Positions-Vorschrift + Sperren pro Stufe). "Speichern" schickt den kompletten bearbeiteten Satz
 * zum Server, der ihn erneut serverseitig gegen Admin-Rechte prüft (siehe
 * {@link ServerPrescriptionSaveRequestPacket}) und an alle Online-Spieler (nach ihrem eigenen
 * OP-Level gefiltert) verteilt. "Schließen" verwirft unge­speicherte Änderungen einfach im Speicher -
 * beim nächsten Öffnen sendet der Server ohnehin einen frischen Satz.
 */
public class ServerPrescriptionEditorScreen extends FixedScaleScreen {

    private static final int ROW_HEIGHT = 46;
    private static final int MARGIN = 12;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final long DELETE_CONFIRM_WINDOW_MS = 4000;

    private List<ServerAdminRow> tabRows = new ArrayList<>();
    private List<ServerAdminRow> categoryRows = new ArrayList<>();
    private int tabScroll = 0;
    private int categoryScroll = 0;
    private int selectedOpLevel = 0;
    private OpLevelWidget levelSwitcher;

    private int listTop;
    private int listBottom;
    private int listWidth;
    private int leftListX;
    private int rightListX;

    private String deleteArmedSlotId;
    private long deleteArmedUntil = 0;

    public ServerPrescriptionEditorScreen() {
        super(Component.translatable("gui.creativemenu.editor.title.server"));
    }

    @Override
    protected void initScaled() {
        int headerY = 26;
        int levelRowY = headerY + 14;
        int buttonRowY = levelRowY + 20;
        listTop = buttonRowY + 24;
        listBottom = height - 40;

        listWidth = (width - 3 * MARGIN) / 2;
        leftListX = MARGIN;
        rightListX = MARGIN * 2 + listWidth;

        levelSwitcher = new OpLevelWidget(width / 2 - OpLevelWidget.width() / 2, levelRowY,
            () -> selectedOpLevel, level -> { selectedOpLevel = level; refreshRows(); });

        refreshRows();

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.newservertab"), b ->
                minecraft.setScreen(new ServerCustomTabEditScreen(this, null, selectedOpLevel, this::onChildSaved)))
            .bounds(leftListX, buttonRowY, listWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.newservercategory"), b ->
                minecraft.setScreen(new ServerCategoryEditScreen(this, null, selectedOpLevel, this::onChildSaved)))
            .bounds(rightListX, buttonRowY, listWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.sort"), b ->
                minecraft.setScreen(new ServerOrderScreen(this, selectedOpLevel, this::onChildSaved)))
            .bounds(width / 2 - 75, height - 50, 150, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.save"), b -> saveToServer())
            .bounds(width / 2 - 105, height - 28, 100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.close"), b -> onClose())
            .bounds(width / 2 + 5, height - 28, 100, 20).build());
    }

    private void onChildSaved() {
        refreshRows();
    }

    private void refreshRows() {
        tabRows = ServerAdminRowBuilder.customRows(selectedOpLevel);
        categoryRows = ServerAdminRowBuilder.categoryRows(selectedOpLevel);
    }

    private void saveToServer() {
        var set = ServerPrescriptionCache.adminFull();
        PacketDistributor.sendToServer(ServerPrescriptionSaveRequestPacket.of(set));
    }

    private List<ServerAdminRow> rowsFor(boolean category) {
        return category ? categoryRows : tabRows;
    }

    private int scrollFor(boolean category) {
        return category ? categoryScroll : tabScroll;
    }

    private void setScrollFor(boolean category, int value) {
        if (category) categoryScroll = value; else tabScroll = value;
    }

    private int listXFor(boolean category) {
        return category ? rightListX : leftListX;
    }

    @Override
    protected void renderScaled(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        levelSwitcher.render(guiGraphics, font, mouseX, mouseY);

        guiGraphics.drawCenteredString(font, Component.translatable("gui.creativemenu.editor.tabsheader"),
            leftListX + listWidth / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.creativemenu.editor.categoriesheader"),
            rightListX + listWidth / 2, 12, 0xFFFFFF);

        renderList(guiGraphics, mouseX, mouseY, false);
        renderList(guiGraphics, mouseX, mouseY, true);
    }

    private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean category) {
        List<ServerAdminRow> rows = rowsFor(category);
        int listX = listXFor(category);
        int scroll = scrollFor(category);
        int contentWidth = listWidth - SCROLLBAR_WIDTH - 2;

        guiGraphics.enableScissor(toRealX(listX), toRealY(listTop), toRealX(listX + listWidth), toRealY(listBottom));
        int visibleRows = (listBottom - listTop) / ROW_HEIGHT;
        for (int i = 0; i < visibleRows && i + scroll < rows.size(); i++) {
            int idx = i + scroll;
            ServerAdminRow row = rows.get(idx);
            int rowY = listTop + i * ROW_HEIGHT;

            int bg = idx % 2 == 0 ? 0x40000000 : 0x20000000;
            guiGraphics.fill(listX, rowY, listX + contentWidth, rowY + ROW_HEIGHT - 3, bg);
            guiGraphics.renderItem(row.icon(), listX + 2, rowY + 2);

            guiGraphics.drawString(font, clipName(row.name().getString(), contentWidth - 22),
                listX + 20, rowY + 4, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, permissionSummary(row), listX + 20, rowY + 16, 0xFFAAAAAA, false);

            int btnW = (contentWidth - 12) / 2;
            int editX = listX + 2;
            int deleteX = editX + btnW + 8;
            int actionY = rowY + 30;

            boolean hoveredEdit = SpriteButton.isHovered(mouseX, mouseY, editX, actionY, btnW, 14);
            SpriteButton.draw(guiGraphics, font, editX, actionY, btnW, 14, I18n.get("gui.creativemenu.editor.edit"), hoveredEdit);

            boolean armed = row.slotId().equals(deleteArmedSlotId) && System.currentTimeMillis() < deleteArmedUntil;
            boolean hoveredDelete = SpriteButton.isHovered(mouseX, mouseY, deleteX, actionY, btnW, 14);
            String deleteLabel = armed ? I18n.get("gui.creativemenu.editor.delete.confirm") : I18n.get("gui.creativemenu.editor.delete");
            SpriteButton.draw(guiGraphics, font, deleteX, actionY, btnW, 14, deleteLabel, hoveredDelete);
        }
        guiGraphics.disableScissor();

        int viewHeight = listBottom - listTop;
        int totalHeight = rows.size() * ROW_HEIGHT;
        if (totalHeight > viewHeight) {
            int barX = listX + contentWidth + 2;
            guiGraphics.fill(barX, listTop, barX + SCROLLBAR_WIDTH, listBottom, 0x40FFFFFF);
            int maxScrollRows = Math.max(1, rows.size() - visibleRows);
            int thumbH = Math.max(10, viewHeight * viewHeight / totalHeight);
            int thumbY = listTop + (viewHeight - thumbH) * scroll / maxScrollRows;
            guiGraphics.fill(barX, thumbY, barX + SCROLLBAR_WIDTH, thumbY + thumbH, 0xA0FFFFFF);
        }
    }

    private String permissionSummary(ServerAdminRow row) {
        String hide = I18n.get(row.allowHide() ? "gui.creativemenu.editor.yes" : "gui.creativemenu.editor.no");
        String sort = I18n.get(row.allowSort() ? "gui.creativemenu.editor.yes" : "gui.creativemenu.editor.no");
        return I18n.get("gui.creativemenu.editor.permsummary", hide, sort);
    }

    private String clipName(String name, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(name) <= maxWidth) return name;
        while (!name.isEmpty() && font.width(name + "..") > maxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        return name + "..";
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (levelSwitcher.mouseClicked(mouseX, mouseY)) return true;
            if (handleListClick(mouseX, mouseY, false)) return true;
            if (handleListClick(mouseX, mouseY, true)) return true;
        }
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    private boolean handleListClick(double mouseX, double mouseY, boolean category) {
        int listX = listXFor(category);
        if (mouseX < listX || mouseX > listX + listWidth || mouseY < listTop || mouseY >= listBottom) return false;

        List<ServerAdminRow> rows = rowsFor(category);
        int scroll = scrollFor(category);
        int visibleRows = (listBottom - listTop) / ROW_HEIGHT;
        int i = (int) ((mouseY - listTop) / ROW_HEIGHT);
        int idx = i + scroll;
        if (i >= visibleRows || idx >= rows.size()) return false;

        int contentWidth = listWidth - SCROLLBAR_WIDTH - 2;
        int rowY = listTop + i * ROW_HEIGHT;
        ServerAdminRow row = rows.get(idx);
        int btnW = (contentWidth - 12) / 2;
        int editX = listX + 2;
        int deleteX = editX + btnW + 8;
        int actionY = rowY + 30;

        if (SpriteButton.isHovered((int) mouseX, (int) mouseY, editX, actionY, btnW, 14)) {
            if (row.isCategory()) {
                minecraft.setScreen(new ServerCategoryEditScreen(this, row.categoryDef(), selectedOpLevel, this::onChildSaved));
            } else {
                minecraft.setScreen(new ServerCustomTabEditScreen(this, row.customDef(), selectedOpLevel, this::onChildSaved));
            }
            return true;
        }
        if (SpriteButton.isHovered((int) mouseX, (int) mouseY, deleteX, actionY, btnW, 14)) {
            handleDeleteClick(row);
            return true;
        }
        return true;
    }

    private void handleDeleteClick(ServerAdminRow row) {
        long now = System.currentTimeMillis();
        if (row.slotId().equals(deleteArmedSlotId) && now < deleteArmedUntil) {
            deleteEntry(row);
            deleteArmedSlotId = null;
        } else {
            deleteArmedSlotId = row.slotId();
            deleteArmedUntil = now + DELETE_CONFIRM_WINDOW_MS;
        }
    }

    private void deleteEntry(ServerAdminRow row) {
        var set = ServerPrescriptionCache.adminFull();
        if (row.isCategory()) {
            set.categories.removeIf(d -> d.slotId().equals(row.slotId()));
        } else {
            set.customTabs.removeIf(d -> d.slotId().equals(row.slotId()));
        }
        onChildSaved();
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftListX && mouseX <= leftListX + listWidth) {
            scrollList(false, scrollY);
            return true;
        }
        if (mouseX >= rightListX && mouseX <= rightListX + listWidth) {
            scrollList(true, scrollY);
            return true;
        }
        return super.mouseScrolledScaled(mouseX, mouseY, scrollX, scrollY);
    }

    private void scrollList(boolean category, double scrollY) {
        List<ServerAdminRow> rows = rowsFor(category);
        int visibleRows = (listBottom - listTop) / ROW_HEIGHT;
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        int current = scrollFor(category);
        setScrollFor(category, Math.max(0, Math.min(maxScroll, current - (int) Math.signum(scrollY))));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
