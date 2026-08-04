package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ClientTabConfigManager;
import com.creativemenu.client.tabs.CustomTabDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Erstellen/Bearbeiten eines Custom-Tabs. Linkes Drittel: Name/Icon + Icon-Kachel-Liste. Bei
 * ITEMS/TABS: mittleres Drittel = aktuelle Liste (Drag zum Umsortieren INNERHALB der Liste, Drag
 * raus oder Klick = entfernen), rechtes Drittel = alle Kandidaten (Drag rein oder Klick =
 * hinzufügen) - Suchfeld+Button bleiben als exakter Text-Pfad erhalten. Bei TAG: eigene Liste aus
 * ODER-verknüpften UND-Gruppen (siehe Klassenkommentar in {@link CustomTabDefinition}).
 */
public class CustomTabEditScreen extends FixedScaleScreen {

    private enum DragSource { NONE, CURRENT, CANDIDATES }

    private static final int TAG_ROW_H = 16;
    private static final long IMPORT_CONFIRM_WINDOW_MS = 4000;

    private final Screen parent;
    private final CustomTabDefinition def;
    private final boolean isNew;
    private final Runnable onSaved;

    private Button importButton;
    private long importArmedUntil = 0;

    private EditBox nameBox;
    private EditBox iconBox;
    private IconGridWidget iconGrid;

    private EditBox removeBox;
    private IconGridWidget removeGrid;
    private EditBox addBox;
    private IconGridWidget addGrid;

    private DragSource dragSource = DragSource.NONE;
    private String dragId;
    private boolean dragMoved;

    private AutocompleteEditBox tagBox;
    private int tagListX, tagListWidth, tagListTop;
    private int tagListScroll = 0;

    public CustomTabEditScreen(Screen parent, CustomTabDefinition existing, Runnable onSaved) {
        super(existing == null
            ? Component.translatable("gui.creativemenu.editor.newtab")
            : Component.translatable("gui.creativemenu.editor.edittab"));
        this.parent = parent;
        this.isNew = existing == null;
        this.def = existing == null ? new CustomTabDefinition() : existing;
        this.onSaved = onSaved;
    }

    @Override
    protected void initScaled() {
        int thirdWidth = width / 3;
        int pad = 8;

        buildLeftThird(thirdWidth, pad);
        buildTypeButtons(thirdWidth);

        boolean isTag = def.sourceType == CustomTabDefinition.SourceType.TAG;
        if (isTag) {
            buildTagPanel(thirdWidth, pad);
        } else {
            buildRemovePanel(thirdWidth, pad);
            buildAddPanel(thirdWidth, pad);
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.save"), b -> save())
            .bounds(width / 2 - 100, height - 26, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.cancel"), b -> onClose())
            .bounds(width / 2 + 10, height - 26, 90, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.export"), b -> exportDef())
            .bounds(8, height - 26, 70, 20).build());
        importButton = Button.builder(Component.translatable("gui.creativemenu.editor.import"), b -> onImportClicked())
            .bounds(86, height - 26, 70, 20).build();
        addRenderableWidget(importButton);
    }

    private void exportDef() {
        ClipboardUtil.copyAsJson(def);
    }

    // Nutzer-Vorgabe: Export/Import auch pro einzelnem Tab - überschreibt nur die Inhalts-Felder
    // (nicht id/slotId), damit ein importierter Tab weiterhin derselbe bleibt statt ein neuer zu werden.
    private void onImportClicked() {
        long now = System.currentTimeMillis();
        if (importArmedUntil != 0 && now < importArmedUntil) {
            CustomTabDefinition imported = ClipboardUtil.pasteAsJson(CustomTabDefinition.class);
            if (imported != null) {
                def.name = imported.name;
                def.iconItemId = imported.iconItemId;
                def.sourceType = imported.sourceType;
                def.itemIds = imported.itemIds;
                def.sourceTabIds = imported.sourceTabIds;
                def.tagGroups = imported.tagGroups;
                clearWidgets();
                init();
            }
            importArmedUntil = 0;
        } else {
            importArmedUntil = now + IMPORT_CONFIRM_WINDOW_MS;
            importButton.setMessage(Component.translatable("gui.creativemenu.editor.import.confirm"));
        }
    }

    private void buildLeftThird(int thirdWidth, int pad) {
        int fieldWidth = thirdWidth - 2 * pad;

        Component nameLabel = Component.translatable("gui.creativemenu.editor.name");
        nameBox = new EditBox(font, pad, 20, fieldWidth, 18, nameLabel);
        nameBox.setValue(def.name);
        nameBox.setHint(nameLabel);
        nameBox.setResponder(s -> def.name = s);
        addRenderableWidget(nameBox);

        Component iconLabel = Component.translatable("gui.creativemenu.editor.icon");
        iconBox = new EditBox(font, pad, 44, fieldWidth, 18, iconLabel);
        iconBox.setValue(def.iconItemId);
        iconBox.setHint(iconLabel);
        iconBox.setResponder(s -> def.iconItemId = s);
        addRenderableWidget(iconBox);

        int gridTop = 68;
        iconGrid = new IconGridWidget(pad, gridTop, fieldWidth, height - gridTop - 30,
            IconCandidates::allItems, iconBox::getValue, id -> iconBox.setValue(id));
    }

    private void buildTypeButtons(int thirdWidth) {
        int cx = thirdWidth + (width - thirdWidth) / 2;
        int typeY = 16;
        addRenderableWidget(Button.builder(typeLabel(CustomTabDefinition.SourceType.ITEMS), b -> setType(CustomTabDefinition.SourceType.ITEMS))
            .bounds(cx - 100, typeY, 65, 20).build());
        addRenderableWidget(Button.builder(typeLabel(CustomTabDefinition.SourceType.TAG), b -> setType(CustomTabDefinition.SourceType.TAG))
            .bounds(cx - 33, typeY, 65, 20).build());
        addRenderableWidget(Button.builder(typeLabel(CustomTabDefinition.SourceType.TABS), b -> setType(CustomTabDefinition.SourceType.TABS))
            .bounds(cx + 34, typeY, 66, 20).build());
    }

    private void buildTagPanel(int thirdWidth, int pad) {
        int x = thirdWidth + pad;
        int w = width - thirdWidth - 2 * pad;

        Component label = Component.translatable("gui.creativemenu.editor.tag");
        tagBox = new AutocompleteEditBox(font, x, 50, w - 60, 18, label, TabCandidates::tags);
        tagBox.setHint(label);
        addRenderableWidget(tagBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.add"), b -> addTagRow())
            .bounds(x + w - 55, 50, 55, 18).build());

        tagListX = x;
        tagListWidth = w;
        tagListTop = 76;

        // Nutzer-Fund: die Vorschlagsliste rendert direkt unterhalb der Box (y=68+) und überlappt
        // damit die Tag-Zeilen-Liste (ab tagListTop=76) - deren Klick-Handling schluckt JEDEN Klick
        // in seinem Bereich (auch ohne Treffer auf +/x), noch bevor er die Box erreicht. Nur der
        // oberste, knapp außerhalb liegende Rand des ersten Vorschlags war deshalb erreichbar.
        // Fix: Tag-Liste als geschützten Bereich eintragen, damit die Vorschlagsliste automatisch
        // nach oben ausweicht statt sie zu überlappen (gleicher Mechanismus wie bei den anderen
        // Autocomplete-Feldern).
        List<int[]> protectedRects = new ArrayList<>();
        protectedRects.add(new int[] {width / 2 - 100, height - 26, 90, 20});
        protectedRects.add(new int[] {width / 2 + 10, height - 26, 90, 20});
        protectedRects.add(new int[] {tagListX, tagListTop, tagListWidth, height - 30 - tagListTop});
        tagBox.setProtectedRects(protectedRects);
    }

    private void buildRemovePanel(int thirdWidth, int pad) {
        int x = thirdWidth + pad;
        int fieldWidth = thirdWidth - 2 * pad - 55;

        Component label = Component.translatable("gui.creativemenu.editor.removeitem");
        removeBox = new EditBox(font, x, 50, fieldWidth, 18, label);
        removeBox.setHint(label);
        addRenderableWidget(removeBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.remove"), b -> removeEntry())
            .bounds(x + fieldWidth + 5, 50, 50, 18).build());

        int gridTop = 76;
        removeGrid = new IconGridWidget(x, gridTop, thirdWidth - 2 * pad, height - gridTop - 30,
            this::currentListEntries, removeBox::getValue, null);
    }

    private void buildAddPanel(int thirdWidth, int pad) {
        int x = 2 * thirdWidth + pad;
        int fieldWidth = thirdWidth - 2 * pad - 55;

        Component label = Component.translatable("gui.creativemenu.editor.additem");
        addBox = new EditBox(font, x, 50, fieldWidth, 18, label);
        addBox.setHint(label);
        addRenderableWidget(addBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.add"), b -> addEntry())
            .bounds(x + fieldWidth + 5, 50, 50, 18).build());

        int gridTop = 76;
        addGrid = new IconGridWidget(x, gridTop, thirdWidth - 2 * pad, height - gridTop - 30,
            this::addCandidates, addBox::getValue, null);
    }

    // Nutzer-Fund: bereits in der "Enthalten"-Liste vorhandene Items/Tabs erschienen weiterhin
    // in dieser Kandidatenliste - hier explizit rausfiltern.
    private List<IconGridWidget.Entry> addCandidates() {
        List<String> current = currentList();
        List<IconGridWidget.Entry> source = isItemsType() ? IconCandidates.allItems() : IconCandidates.allTabsOnly();
        return source.stream().filter(e -> !current.contains(e.id())).toList();
    }

    private boolean isItemsType() {
        return def.sourceType == CustomTabDefinition.SourceType.ITEMS;
    }

    private List<String> currentList() {
        return isItemsType() ? def.itemIds : def.sourceTabIds;
    }

    private List<IconGridWidget.Entry> currentListEntries() {
        List<IconGridWidget.Entry> source = isItemsType() ? IconCandidates.allItems() : IconCandidates.allTabsOnly();
        Map<String, IconGridWidget.Entry> byId = source.stream()
            .collect(Collectors.toMap(IconGridWidget.Entry::id, e -> e, (a, b) -> a));
        List<IconGridWidget.Entry> result = new ArrayList<>();
        for (String id : currentList()) {
            IconGridWidget.Entry entry = byId.get(id);
            if (entry != null) result.add(entry);
        }
        return result;
    }

    private Component typeLabel(CustomTabDefinition.SourceType type) {
        String key = switch (type) {
            case ITEMS -> "gui.creativemenu.editor.type.items";
            case TAG -> "gui.creativemenu.editor.type.tag";
            case TABS -> "gui.creativemenu.editor.type.wholetab";
        };
        String prefix = def.sourceType == type ? "> " : "";
        return Component.literal(prefix).append(Component.translatable(key));
    }

    private void setType(CustomTabDefinition.SourceType type) {
        def.sourceType = type;
        clearWidgets();
        init();
    }

    private void addEntry() {
        String value = addBox.getValue().trim();
        if (value.isEmpty() || ResourceLocation.tryParse(value) == null) return;
        List<String> list = currentList();
        if (!list.contains(value)) list.add(value);
        addBox.setValue("");
    }

    private void removeEntry() {
        String value = removeBox.getValue().trim();
        currentList().remove(value);
        removeBox.setValue("");
    }

    // --- Tag-Gruppen (ODER von UND-Gruppen) ---

    private void addTagRow() {
        String tag = tagBox.getValue().trim();
        if (tag.isEmpty()) return;
        List<String> group = new ArrayList<>();
        group.add(tag);
        def.tagGroups.add(group);
        tagBox.setValue("");
    }

    private void andIntoRow(int rowIndex) {
        String tag = tagBox.getValue().trim();
        if (tag.isEmpty() || rowIndex < 0 || rowIndex >= def.tagGroups.size()) return;
        List<String> group = def.tagGroups.get(rowIndex);
        if (!group.contains(tag)) group.add(tag);
        tagBox.setValue("");
    }

    private void removeTagRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < def.tagGroups.size()) def.tagGroups.remove(rowIndex);
    }

    private void save() {
        if (isNew) ClientTabConfigManager.get().customTabs.add(def);
        if (isNew) ClientTabConfigManager.get().order.add(def.slotId());
        ClientTabConfigManager.save();
        if (onSaved != null) onSaved.run();
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    protected void renderScaled(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);
        if (importArmedUntil != 0 && System.currentTimeMillis() > importArmedUntil) {
            importArmedUntil = 0;
            importButton.setMessage(Component.translatable("gui.creativemenu.editor.import"));
        }
        guiGraphics.drawCenteredString(font, title, width / 2, 4, 0xFFFFFF);

        iconGrid.render(guiGraphics, mouseX, mouseY);
        if (def.sourceType == CustomTabDefinition.SourceType.TAG) {
            renderTagList(guiGraphics, mouseX, mouseY);
        } else {
            removeGrid.render(guiGraphics, mouseX, mouseY);
            addGrid.render(guiGraphics, mouseX, mouseY);
        }

        iconGrid.renderTooltip(guiGraphics, mouseX, mouseY);
        if (def.sourceType == CustomTabDefinition.SourceType.TAG) {
            tagBox.renderDropdown(guiGraphics, mouseX, mouseY);
        } else {
            removeGrid.renderTooltip(guiGraphics, mouseX, mouseY);
            addGrid.renderTooltip(guiGraphics, mouseX, mouseY);
        }

        if (dragSource != DragSource.NONE && dragId != null) {
            var entry = (isItemsType() ? IconCandidates.allItems() : IconCandidates.allTabsOnly()).stream()
                .filter(e -> e.id().equals(dragId)).findFirst().orElse(null);
            if (entry != null) {
                guiGraphics.fill(mouseX - 10, mouseY - 10, mouseX + 10, mouseY + 10, 0xC0222222);
                guiGraphics.renderItem(entry.icon(), mouseX - 8, mouseY - 8);
            }
        }
    }

    private void renderTagList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int rowH = TAG_ROW_H;
        int visibleRows = Math.max(0, (height - 30 - tagListTop) / rowH);
        guiGraphics.enableScissor(toRealX(tagListX), toRealY(tagListTop), toRealX(tagListX + tagListWidth), toRealY(height - 30));
        for (int i = 0; i < visibleRows && i + tagListScroll < def.tagGroups.size(); i++) {
            int idx = i + tagListScroll;
            List<String> group = def.tagGroups.get(idx);
            int rowY = tagListTop + i * rowH;
            int bg = idx % 2 == 0 ? 0x40000000 : 0x20000000;
            guiGraphics.fill(tagListX, rowY, tagListX + tagListWidth, rowY + rowH, bg);
            guiGraphics.drawString(font, String.join(" + ", group), tagListX + 2, rowY + (rowH - 8) / 2, 0xFFFFFF, false);

            boolean hoveredPlus = SpriteButton.isHovered(mouseX, mouseY, tagListX + tagListWidth - 40, rowY, 18, rowH);
            SpriteButton.draw(guiGraphics, font, tagListX + tagListWidth - 40, rowY, 18, rowH, "+", hoveredPlus);
            boolean hoveredX = SpriteButton.isHovered(mouseX, mouseY, tagListX + tagListWidth - 20, rowY, 18, rowH);
            SpriteButton.draw(guiGraphics, font, tagListX + tagListWidth - 20, rowY, 18, rowH, "x", hoveredX);
        }
        guiGraphics.disableScissor();
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (iconGrid.mouseClicked(mouseX, mouseY)) return true;

            if (def.sourceType == CustomTabDefinition.SourceType.TAG) {
                if (handleTagListClick(mouseX, mouseY)) return true;
            } else {
                String removeId = removeGrid.tileAt(mouseX, mouseY);
                if (removeId != null) {
                    dragSource = DragSource.CURRENT;
                    dragId = removeId;
                    dragMoved = false;
                    currentList().remove(removeId);
                    return true;
                }
                String addId = addGrid.tileAt(mouseX, mouseY);
                if (addId != null) {
                    dragSource = DragSource.CANDIDATES;
                    dragId = addId;
                    dragMoved = false;
                    return true;
                }
            }
        }
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    private boolean handleTagListClick(double mouseX, double mouseY) {
        if (mouseX < tagListX || mouseX >= tagListX + tagListWidth || mouseY < tagListTop || mouseY >= height - 30) return false;
        int i = (int) ((mouseY - tagListTop) / TAG_ROW_H);
        int idx = i + tagListScroll;
        // Nutzer-Fund: hier stand vorher ein unbedingtes "return true" für jeden Klick in der
        // Zeile, auch ohne +/x-Treffer - das schluckte Klicks, die eigentlich der (teils
        // überlappenden) Autocomplete-Vorschlagsliste gehörten. Jetzt nur bei echtem Button-Treffer
        // konsumieren, sonst durchreichen (return false) - der Bereich ist zusätzlich als
        // geschützter Bereich bei der Vorschlagsbox hinterlegt (siehe buildTagPanel), sodass sich
        // beide ohnehin kaum noch überlappen sollten.
        if (idx < 0 || idx >= def.tagGroups.size()) return false;
        int rowY = tagListTop + i * TAG_ROW_H;
        if (SpriteButton.isHovered((int) mouseX, (int) mouseY, tagListX + tagListWidth - 40, rowY, 18, TAG_ROW_H)) {
            andIntoRow(idx);
            return true;
        }
        if (SpriteButton.isHovered((int) mouseX, (int) mouseY, tagListX + tagListWidth - 20, rowY, 18, TAG_ROW_H)) {
            removeTagRow(idx);
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragSource != DragSource.NONE) {
            dragMoved = true;
            List<String> list = currentList();
            list.remove(dragId);
            if (removeGrid.contains(mouseX, mouseY)) {
                int idx = Math.max(0, Math.min(list.size(), removeGrid.insertionIndexAt(mouseX, mouseY)));
                list.add(idx, dragId);
            }
            return true;
        }
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (dragSource != DragSource.NONE) {
            if (!dragMoved && dragSource == DragSource.CANDIDATES) {
                List<String> list = currentList();
                if (!list.contains(dragId)) list.add(dragId);
            }
            dragSource = DragSource.NONE;
            dragId = null;
            return true;
        }
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (iconGrid.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (def.sourceType == CustomTabDefinition.SourceType.TAG) {
            if (mouseX >= tagListX && mouseX < tagListX + tagListWidth && mouseY >= tagListTop && mouseY < height - 30) {
                int visibleRows = Math.max(1, (height - 30 - tagListTop) / TAG_ROW_H);
                int maxScroll = Math.max(0, def.tagGroups.size() - visibleRows);
                tagListScroll = Math.max(0, Math.min(maxScroll, tagListScroll - (int) Math.signum(scrollY)));
                return true;
            }
        } else {
            if (removeGrid.mouseScrolled(mouseX, mouseY, scrollY)) return true;
            if (addGrid.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        }
        return super.mouseScrolledScaled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
