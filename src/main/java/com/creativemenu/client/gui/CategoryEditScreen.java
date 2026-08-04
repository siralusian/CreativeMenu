package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.CategoryDefinition;
import com.creativemenu.client.tabs.ClientTabConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Erstellen/Bearbeiten einer Kategorie - gleicher Aufbau wie {@link CustomTabEditScreen} (linkes
 * Drittel Name/Icon + Icon-Liste, mittleres/rechtes Drittel Mitglieder entfernen/hinzufügen als
 * Kachel-Liste mit Drag&Drop statt Klick-in-Suchleiste), nur ohne Items/Tag/Ganzer-Tab-Auswahl.
 */
public class CategoryEditScreen extends FixedScaleScreen {

    private enum DragSource { NONE, CURRENT, CANDIDATES }

    private static final long IMPORT_CONFIRM_WINDOW_MS = 4000;

    private final Screen parent;
    private final CategoryDefinition def;
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

    public CategoryEditScreen(Screen parent, CategoryDefinition existing, Runnable onSaved) {
        super(existing == null
            ? Component.translatable("gui.creativemenu.editor.newcategory")
            : Component.translatable("gui.creativemenu.editor.editcategory"));
        this.parent = parent;
        this.isNew = existing == null;
        this.def = existing == null ? new CategoryDefinition() : existing;
        this.onSaved = onSaved;
    }

    @Override
    protected void initScaled() {
        int thirdWidth = width / 3;
        int pad = 8;

        buildLeftThird(thirdWidth, pad);
        buildRemovePanel(thirdWidth, pad);
        buildAddPanel(thirdWidth, pad);

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

    private void onImportClicked() {
        long now = System.currentTimeMillis();
        if (importArmedUntil != 0 && now < importArmedUntil) {
            CategoryDefinition imported = ClipboardUtil.pasteAsJson(CategoryDefinition.class);
            if (imported != null) {
                def.name = imported.name;
                def.iconItemId = imported.iconItemId;
                def.memberSlotIds = imported.memberSlotIds;
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

    private void buildRemovePanel(int thirdWidth, int pad) {
        int x = thirdWidth + pad;
        int fieldWidth = thirdWidth - 2 * pad - 55;

        Component label = Component.translatable("gui.creativemenu.editor.removemember");
        removeBox = new EditBox(font, x, 20, fieldWidth, 18, label);
        removeBox.setHint(label);
        addRenderableWidget(removeBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.remove"), b -> removeMember())
            .bounds(x + fieldWidth + 5, 20, 50, 18).build());

        int gridTop = 46;
        removeGrid = new IconGridWidget(x, gridTop, thirdWidth - 2 * pad, height - gridTop - 30,
            this::currentMemberEntries, removeBox::getValue, null);
    }

    private void buildAddPanel(int thirdWidth, int pad) {
        int x = 2 * thirdWidth + pad;
        int fieldWidth = thirdWidth - 2 * pad - 55;

        Component label = Component.translatable("gui.creativemenu.editor.addmember");
        addBox = new EditBox(font, x, 20, fieldWidth, 18, label);
        addBox.setHint(label);
        addRenderableWidget(addBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.creativemenu.editor.add"), b -> addMember())
            .bounds(x + fieldWidth + 5, 20, 50, 18).build());

        int gridTop = 46;
        addGrid = new IconGridWidget(x, gridTop, thirdWidth - 2 * pad, height - gridTop - 30,
            this::addCandidates, addBox::getValue, null);
    }

    private List<IconGridWidget.Entry> addCandidates() {
        return IconCandidates.allTabsAndCustom().stream()
            .filter(e -> !def.memberSlotIds.contains(e.id())).toList();
    }

    private List<IconGridWidget.Entry> currentMemberEntries() {
        List<IconGridWidget.Entry> source = IconCandidates.allTabsAndCustom();
        Map<String, IconGridWidget.Entry> byId = source.stream()
            .collect(Collectors.toMap(IconGridWidget.Entry::id, e -> e, (a, b) -> a));
        List<IconGridWidget.Entry> result = new java.util.ArrayList<>();
        for (String id : def.memberSlotIds) {
            IconGridWidget.Entry entry = byId.get(id);
            if (entry != null) result.add(entry);
        }
        return result;
    }

    private void addMember() {
        String text = addBox.getValue().trim();
        if (text.isEmpty()) return;
        if (!def.memberSlotIds.contains(text)) def.memberSlotIds.add(text);
        addBox.setValue("");
    }

    private void removeMember() {
        String text = removeBox.getValue().trim();
        def.memberSlotIds.remove(text);
        removeBox.setValue("");
    }

    private void save() {
        if (isNew) ClientTabConfigManager.get().categories.add(def);
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
        removeGrid.render(guiGraphics, mouseX, mouseY);
        addGrid.render(guiGraphics, mouseX, mouseY);

        iconGrid.renderTooltip(guiGraphics, mouseX, mouseY);
        removeGrid.renderTooltip(guiGraphics, mouseX, mouseY);
        addGrid.renderTooltip(guiGraphics, mouseX, mouseY);

        if (dragSource != DragSource.NONE && dragId != null) {
            IconCandidates.allTabsAndCustom().stream().filter(e -> e.id().equals(dragId)).findFirst()
                .ifPresent(entry -> {
                    guiGraphics.fill(mouseX - 10, mouseY - 10, mouseX + 10, mouseY + 10, 0xC0222222);
                    guiGraphics.renderItem(entry.icon(), mouseX - 8, mouseY - 8);
                });
        }
    }

    @Override
    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (iconGrid.mouseClicked(mouseX, mouseY)) return true;

            String removeId = removeGrid.tileAt(mouseX, mouseY);
            if (removeId != null) {
                dragSource = DragSource.CURRENT;
                dragId = removeId;
                dragMoved = false;
                def.memberSlotIds.remove(removeId);
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
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    @Override
    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragSource != DragSource.NONE) {
            dragMoved = true;
            def.memberSlotIds.remove(dragId);
            if (removeGrid.contains(mouseX, mouseY)) {
                int idx = Math.max(0, Math.min(def.memberSlotIds.size(), removeGrid.insertionIndexAt(mouseX, mouseY)));
                def.memberSlotIds.add(idx, dragId);
            }
            return true;
        }
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (dragSource != DragSource.NONE) {
            if (!dragMoved && dragSource == DragSource.CANDIDATES && !def.memberSlotIds.contains(dragId)) {
                def.memberSlotIds.add(dragId);
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
        if (removeGrid.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (addGrid.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        return super.mouseScrolledScaled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
