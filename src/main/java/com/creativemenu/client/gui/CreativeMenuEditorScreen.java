package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.CategoryBrowseState;
import com.creativemenu.client.tabs.ClientTabConfigManager;
import com.creativemenu.client.tabs.LocalTabLayout;
import com.creativemenu.client.tabs.ServerPrescriptionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bearbeitungsmenü für die lokale Kreativmenü-Konfiguration: links Tabs-Liste, rechts Kategorien-
 * Liste, in der Mitte eine ECHTE eingebettete Vorschau des Kreativmenüs (siehe
 * {@link EmbeddedCreativeMenuPreview}) - aktualisiert sich nur gezielt (Speichern/Zurücksetzen),
 * nicht bei jedem Frame. Reihenfolge ändern passiert nicht mehr hier, sondern im separaten
 * {@link SortScreen}. Serverseitige Vorgaben werden über den eigenen
 * {@link ServerPrescriptionEditorScreen} bearbeitet (siehe {@link com.creativemenu.client.ClientEditorHandler}).
 */
public class CreativeMenuEditorScreen extends FixedScaleScreen {

    private static final int ROW_HEIGHT = 34;
    private static final int BASE_LIST_WIDTH = 150;
    private static final int MARGIN = 12;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final long RESET_CONFIRM_WINDOW_MS = 4000;

    private final EmbeddedCreativeMenuPreview preview = new EmbeddedCreativeMenuPreview();

    private List<TabRow> tabRows = new ArrayList<>();
    private List<TabRow> categoryRows = new ArrayList<>();
    private int tabScroll = 0;
    private int categoryScroll = 0;

    private int listTop;
    private int listBottom;
    private int listWidth;
    private int leftListX;
    private int rightListX;
    private int previewX;
    private int previewWidth;

    private Button resetButton;
    private long resetArmedUntil = 0;
    private Button categoryDesignButton;
    private Button importButton;
    private long importArmedUntil = 0;

    // Löschen einzelner Custom-Tabs/Kategorien (Nutzer-Vorgabe: nicht mehr nur komplett
    // zurücksetzen können) - gleiches Bestätigen-durch-zweiten-Klick-Muster wie der Reset-Button,
    // nur pro Zeile statt global (per Slot-ID gemerkt, da die Liste hier keine echten Button-
    // Widgets sind, sondern von Hand gezeichnet werden).
    private String deleteArmedSlotId;
    private long deleteArmedUntil = 0;

    // Die eingebettete Vorschau ist jetzt interaktiv navigierbar (Tab-Wechsel/Seiten-Buttons) und
    // benutzt dabei dasselbe statische selectedTab-Feld wie das echte Kreativmenü - Snapshot beim
    // Öffnen, Restore beim Schließen, damit Herumklicken in der Vorschau nicht merkt, welchen Tab
    // der Spieler beim nächsten echten Öffnen des Kreativmenüs zu sehen bekommt. Der gemerkte
    // Slot (SelectedSlotTracker) muss aus demselben Grund ebenfalls gesichert/wiederhergestellt
    // werden, sonst würde ein Klick in der Vorschau die für den ECHTEN Menü-Aufruf gemerkte Auswahl
    // überschreiben (siehe CreativeModeTabRegistryMixin).
    private CreativeModeTab savedSelectedTab;
    private String savedSelectedSlotId;
    private boolean canAddRemove = true;

    public CreativeMenuEditorScreen() {
        super(Component.translatable("gui.creativemenu.editor.title.local"));
        savedSelectedTab = SelectedTabSnapshot.get();
        savedSelectedSlotId = SelectedSlotTracker.get();
    }

    @Override
    public void onClose() {
        SelectedTabSnapshot.set(savedSelectedTab);
        SelectedSlotTracker.set(savedSelectedSlotId);
        super.onClose();
    }

    @Override
    protected void initScaled() {
        int headerY = 26;
        int buttonRowY = headerY + 14;
        listTop = buttonRowY + 24;
        listBottom = height - 40;

        // Nutzer-Fund: die Vorschau war mit voller Restbreite viel zu breit ("jede Menge Platz
        // über") - hier auf die Hälfte der ursprünglichen Restbreite gekürzt, die andere Hälfte
        // geht zu gleichen Teilen an beide Listen.
        int baselinePreviewWidth = width - 4 * MARGIN - 2 * BASE_LIST_WIDTH;
        listWidth = BASE_LIST_WIDTH + baselinePreviewWidth / 4;

        leftListX = MARGIN;
        rightListX = width - MARGIN - listWidth;
        previewX = leftListX + listWidth + MARGIN;
        previewWidth = rightListX - MARGIN - previewX;

        refreshRows();

        // Nutzer-Vorgabe: globale Berechtigungsschwelle, ab welcher OP-Stufe Spieler ihr eigenes
        // Kreativmenü überhaupt verändern dürfen (siehe CreativeMenuPermissionConfig) - reine
        // Client-Komfort-Sperre (die lokale Konfigurationsdatei ist kein sicherheitskritischer
        // Server-Zustand), daher genügt ein einfaches Deaktivieren der Buttons.
        boolean canAddRemove = Minecraft.getInstance().player.hasPermissions(ServerPrescriptionCache.minOpLevelAddRemove());
        this.canAddRemove = canAddRemove;

        Button newTabButton = Button.builder(Component.translatable("gui.creativemenu.editor.newtab"), b ->
                minecraft.setScreen(new CustomTabEditScreen(this, null, this::onChildSaved)))
            .bounds(leftListX, buttonRowY, listWidth, 20).build();
        newTabButton.active = canAddRemove;
        addRenderableWidget(newTabButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.sort"), b ->
                minecraft.setScreen(new SortScreen(this, this::onChildSaved)))
            .bounds(previewX, buttonRowY, previewWidth, 20).build());

        Button newCategoryButton = Button.builder(Component.translatable("gui.creativemenu.editor.newcategory"), b ->
                minecraft.setScreen(new CategoryEditScreen(this, null, this::onChildSaved)))
            .bounds(rightListX, buttonRowY, listWidth, 20).build();
        newCategoryButton.active = canAddRemove;
        addRenderableWidget(newCategoryButton);

        // Nutzer-Vorgabe: nicht auf ein Kategorie-Darstellungsdesign festlegen, sondern zur Wahl
        // stellen - Klick zyklet durch die 3 Optionen, sofort gespeichert. Nutzer-Fund: oben rechts
        // überlappte der Button mit der "Kategorien"-Listenüberschrift - jetzt unten links.
        categoryDesignButton = Button.builder(categoryDesignLabel(), b -> cycleCategoryDesign())
            .bounds(8, height - 24, 200, 16).build();
        addRenderableWidget(categoryDesignButton);

        // Nutzer-Vorgabe: Export/Import der GESAMTEN Konfiguration über die Zwischenablage - unten
        // rechts (aus demselben Grund wie oben nicht mehr oben links, wo sie die "Tabs"-Überschrift überlappten).
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.export"), b -> exportAll())
            .bounds(width - 158, height - 24, 70, 16).build());
        importButton = Button.builder(Component.translatable("gui.creativemenu.editor.import"), b -> onImportClicked())
            .bounds(width - 80, height - 24, 70, 16).build();
        addRenderableWidget(importButton);

        resetButton = Button.builder(Component.translatable("gui.creativemenu.editor.reset"), b -> onResetClicked())
            .bounds(width / 2 - 105, height - 28, 100, 20).build();
        addRenderableWidget(resetButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.close"), b -> onClose())
            .bounds(width / 2 + 5, height - 28, 100, 20).build());

        refreshPreview();
    }

    // Nutzer-Fund (GUI-Skalierung): die eingebettete Vorschau bettet einen ECHTEN zweiten
    // CreativeModeInventoryScreen ein und rendert ihn selbst per PoseStack-Push/enableScissor -
    // damit das bei GUI-Größe != 2 nicht doppelt/falsch skaliert, bekommt sie ECHTE (nicht
    // virtuelle) Koordinaten und wird beim Rendern über renderInRealSpace() aufgerufen (siehe
    // FixedScaleScreen-Klassenkommentar - exakt der Anwendungsfall, für den die Methode gedacht ist).
    private void refreshPreview() {
        preview.refresh(toRealX(previewX), toRealY(listTop), toRealX(previewWidth), toRealY(listBottom - listTop));
    }

    private void onChildSaved() {
        refreshRows();
        refreshPreview();
    }

    // Nutzer-Vorgabe: reale (Original-)Tabs verwalten sich nur noch über den Sortieren-Screen -
    // diese Liste hier zeigt daher nur noch Custom-Tabs/Kategorien.
    private void refreshRows() {
        List<TabRow> all = EditorTabRowBuilder.buildRows();
        tabRows = all.stream().filter(TabRow::isCustom).collect(Collectors.toCollection(ArrayList::new));
        categoryRows = all.stream().filter(TabRow::isCategory).collect(Collectors.toCollection(ArrayList::new));
    }

    private Component categoryDesignLabel() {
        int mode = ClientTabConfigManager.get().categoryDisplayMode;
        String key = switch (mode) {
            case 0 -> "gui.creativemenu.editor.categorydesign.overlay";
            case 1 -> "gui.creativemenu.editor.categorydesign.itemgrid";
            default -> "gui.creativemenu.editor.categorydesign.slottakeover";
        };
        return Component.translatable("gui.creativemenu.editor.categorydesign", Component.translatable(key));
    }

    private void cycleCategoryDesign() {
        var layout = ClientTabConfigManager.get();
        layout.categoryDisplayMode = (layout.categoryDisplayMode + 1) % 3;
        ClientTabConfigManager.save();
        categoryDesignButton.setMessage(categoryDesignLabel());
        // Nutzer-Fund (Crash): Browse-Zustand (z.B. Design 1s currentMemberIndex==-1-Sentinel für
        // "Picker-Ansicht") ist an das JEWEILIGE Design gebunden - überlebte er einen Design-Wechsel,
        // verstand das neue Design ihn nicht (führte zu IndexOutOfBoundsException beim nächsten
        // Öffnen des Kreativmenüs). Beim Wechseln daher immer zurücksetzen.
        CategoryBrowseState.exit();
    }

    private void exportAll() {
        ClipboardUtil.copyAsJson(ClientTabConfigManager.get());
    }

    private void onImportClicked() {
        long now = System.currentTimeMillis();
        if (importArmedUntil != 0 && now < importArmedUntil) {
            LocalTabLayout imported = ClipboardUtil.pasteAsJson(LocalTabLayout.class);
            if (imported != null) {
                ClientTabConfigManager.replaceAll(imported);
                onChildSaved();
            }
            importArmedUntil = 0;
            importButton.setMessage(Component.translatable("gui.creativemenu.editor.import"));
        } else {
            importArmedUntil = now + RESET_CONFIRM_WINDOW_MS;
            importButton.setMessage(Component.translatable("gui.creativemenu.editor.import.confirm"));
        }
    }

    private void onResetClicked() {
        long now = System.currentTimeMillis();
        if (resetArmedUntil != 0 && now < resetArmedUntil) {
            ClientTabConfigManager.resetAll();
            onChildSaved();
            resetArmedUntil = 0;
            resetButton.setMessage(Component.translatable("gui.creativemenu.editor.reset"));
        } else {
            resetArmedUntil = now + RESET_CONFIRM_WINDOW_MS;
            resetButton.setMessage(Component.translatable("gui.creativemenu.editor.reset.confirm"));
        }
    }

    private List<TabRow> rowsFor(boolean category) {
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

        if (resetArmedUntil != 0 && System.currentTimeMillis() > resetArmedUntil) {
            resetArmedUntil = 0;
            resetButton.setMessage(Component.translatable("gui.creativemenu.editor.reset"));
        }
        if (importArmedUntil != 0 && System.currentTimeMillis() > importArmedUntil) {
            importArmedUntil = 0;
            importButton.setMessage(Component.translatable("gui.creativemenu.editor.import"));
        }

        guiGraphics.drawCenteredString(font, Component.translatable("gui.creativemenu.editor.tabsheader"),
            leftListX + listWidth / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(font, Component.translatable("gui.creativemenu.editor.categoriesheader"),
            rightListX + listWidth / 2, 12, 0xFFFFFF);

        renderList(guiGraphics, mouseX, mouseY, false);
        renderList(guiGraphics, mouseX, mouseY, true);

        // preview.render() prüft selbst (in echten Koordinaten), ob die Maus über der Vorschau ist,
        // und setzt sonst intern -9999/-9999 (siehe EmbeddedCreativeMenuPreview) - hier reicht daher
        // eine einfache Umrechnung nach "echt", kein zusätzlicher Bereichs-Check nötig.
        int realMouseX = toRealX(mouseX);
        int realMouseY = toRealY(mouseY);
        renderInRealSpace(guiGraphics, () -> preview.render(guiGraphics, realMouseX, realMouseY, partialTick));
    }

    private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean category) {
        List<TabRow> rows = rowsFor(category);
        int listX = listXFor(category);
        int scroll = scrollFor(category);
        int contentWidth = listWidth - SCROLLBAR_WIDTH - 2;

        guiGraphics.enableScissor(toRealX(listX), toRealY(listTop), toRealX(listX + listWidth), toRealY(listBottom));
        int visibleRows = (listBottom - listTop) / ROW_HEIGHT;
        for (int i = 0; i < visibleRows && i + scroll < rows.size(); i++) {
            int idx = i + scroll;
            TabRow row = rows.get(idx);
            int rowY = listTop + i * ROW_HEIGHT;

            int bg = idx % 2 == 0 ? 0x40000000 : 0x20000000;
            guiGraphics.fill(listX, rowY, listX + contentWidth, rowY + ROW_HEIGHT - 3, bg);
            guiGraphics.renderItem(row.icon(), listX + 2, rowY + 2);

            int nameColor = row.hidden() ? 0xFF808080 : 0xFFFFFFFF;
            guiGraphics.drawString(font, clipName(row.name().getString(), contentWidth - 22),
                listX + 20, rowY + 5, nameColor, false);

            int btnW = (contentWidth - 12) / 2;
            int editX = listX + 2;
            int actionY = rowY + 18;
            int deleteX = editX + btnW + 8;

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
            if (handleListClick(mouseX, mouseY, false)) return true;
            if (handleListClick(mouseX, mouseY, true)) return true;
        }
        if (preview.mouseClicked(toRealX((int) mouseX), toRealY((int) mouseY), button)) return true;
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (preview.mouseReleased(toRealX((int) mouseX), toRealY((int) mouseY), button)) return true;
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (preview.mouseDragged(toRealX((int) mouseX), toRealY((int) mouseY), button, toRealX((int) dragX), toRealY((int) dragY))) return true;
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean handleListClick(double mouseX, double mouseY, boolean category) {
        int listX = listXFor(category);
        if (mouseX < listX || mouseX > listX + listWidth || mouseY < listTop || mouseY >= listBottom) return false;

        List<TabRow> rows = rowsFor(category);
        int scroll = scrollFor(category);
        int visibleRows = (listBottom - listTop) / ROW_HEIGHT;
        int i = (int) ((mouseY - listTop) / ROW_HEIGHT);
        int idx = i + scroll;
        if (i >= visibleRows || idx >= rows.size()) return false;

        int contentWidth = listWidth - SCROLLBAR_WIDTH - 2;
        int rowY = listTop + i * ROW_HEIGHT;
        TabRow row = rows.get(idx);
        int btnW = (contentWidth - 12) / 2;
        int editX = listX + 2;
        int actionY = rowY + 18;
        int deleteX = editX + btnW + 8;

        if (!canAddRemove) return true;

        if (SpriteButton.isHovered((int) mouseX, (int) mouseY, editX, actionY, btnW, 14)) {
            if (row.isCustom()) {
                minecraft.setScreen(new CustomTabEditScreen(this, row.customDef(), this::onChildSaved));
            } else {
                minecraft.setScreen(new CategoryEditScreen(this, row.categoryDef(), this::onChildSaved));
            }
            return true;
        }
        if (SpriteButton.isHovered((int) mouseX, (int) mouseY, deleteX, actionY, btnW, 14)) {
            handleDeleteClick(row);
            return true;
        }
        return true;
    }

    private void handleDeleteClick(TabRow row) {
        long now = System.currentTimeMillis();
        if (row.slotId().equals(deleteArmedSlotId) && now < deleteArmedUntil) {
            deleteEntry(row);
            deleteArmedSlotId = null;
        } else {
            deleteArmedSlotId = row.slotId();
            deleteArmedUntil = now + RESET_CONFIRM_WINDOW_MS;
        }
    }

    private void deleteEntry(TabRow row) {
        var layout = ClientTabConfigManager.get();
        layout.order.remove(row.slotId());
        layout.hiddenIds.remove(row.slotId());
        if (row.isCustom()) {
            layout.customTabs.removeIf(d -> d.slotId().equals(row.slotId()));
        } else if (row.isCategory()) {
            layout.categories.removeIf(d -> d.slotId().equals(row.slotId()));
        }
        ClientTabConfigManager.save();
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
        if (preview.mouseScrolled(toRealX((int) mouseX), toRealY((int) mouseY), scrollX, scrollY)) return true;
        return super.mouseScrolledScaled(mouseX, mouseY, scrollX, scrollY);
    }

    private void scrollList(boolean category, double scrollY) {
        List<TabRow> rows = rowsFor(category);
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
