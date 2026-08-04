package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ServerPrescriptionCache;
import com.creativemenu.data.ServerCategoryPrescription;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Server-Pendant zu {@link CategoryEditScreen}. Mitglieder-Kandidaten sind bewusst NUR echte
 * Original-Tabs ({@link IconCandidates#allTabsOnly()}), keine Custom-Tabs - der Server kennt die
 * privaten Custom-Tabs einzelner Spieler nicht (siehe Klassenkommentar von
 * {@link ServerCategoryPrescription}). Die OP-Stufe wird NICHT mehr hier gewählt (siehe
 * {@link ServerCustomTabEditScreen}-Klassenkommentar) - nur noch als Anzeige, plus die
 * Ein-/Ausblenden-/Verschieben-erlaubt-Umschalter.
 */
public class ServerCategoryEditScreen extends FixedScaleScreen {

    private enum DragSource { NONE, CURRENT, CANDIDATES }

    private static final long IMPORT_CONFIRM_WINDOW_MS = 4000;

    private final Screen parent;
    private final ServerCategoryPrescription def;
    private final boolean isNew;
    private final Runnable onSaved;

    private EditBox nameBox;
    private EditBox iconBox;
    private IconGridWidget iconGrid;
    private Button allowHideButton;
    private Button allowSortButton;
    private Button importButton;
    private long importArmedUntil = 0;

    private EditBox removeBox;
    private IconGridWidget removeGrid;
    private EditBox addBox;
    private IconGridWidget addGrid;

    private DragSource dragSource = DragSource.NONE;
    private String dragId;
    private boolean dragMoved;

    public ServerCategoryEditScreen(Screen parent, ServerCategoryPrescription existing, int contextOpLevel, Runnable onSaved) {
        super(existing == null
            ? Component.translatable("gui.creativemenu.editor.newservercategory")
            : Component.translatable("gui.creativemenu.editor.editservercategory"));
        this.parent = parent;
        this.isNew = existing == null;
        this.def = existing == null ? new ServerCategoryPrescription() : existing;
        if (isNew) this.def.opLevel = contextOpLevel;
        this.onSaved = onSaved;
    }

    @Override
    protected void initScaled() {
        int thirdWidth = width / 3;
        int pad = 8;

        buildLeftThird(thirdWidth, pad);
        buildRemovePanel(thirdWidth, pad);
        buildAddPanel(thirdWidth, pad);
        buildPermissionButtons();

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
            ServerCategoryPrescription imported = ClipboardUtil.pasteAsJson(ServerCategoryPrescription.class);
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

    private void buildPermissionButtons() {
        int w = 150;
        int x = width - 2 * w - 8 - 8;
        allowHideButton = Button.builder(permissionLabel("gui.creativemenu.editor.allowhide", def.allowHide),
                b -> { def.allowHide = !def.allowHide; allowHideButton.setMessage(permissionLabel("gui.creativemenu.editor.allowhide", def.allowHide)); })
            .bounds(x, 2, w, 16).build();
        addRenderableWidget(allowHideButton);

        allowSortButton = Button.builder(permissionLabel("gui.creativemenu.editor.allowsort", def.allowSort),
                b -> { def.allowSort = !def.allowSort; allowSortButton.setMessage(permissionLabel("gui.creativemenu.editor.allowsort", def.allowSort)); })
            .bounds(x + w + 8, 2, w, 16).build();
        addRenderableWidget(allowSortButton);
    }

    private Component permissionLabel(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "gui.creativemenu.editor.yes" : "gui.creativemenu.editor.no"));
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
        return IconCandidates.allTabsOnly().stream()
            .filter(e -> !def.memberSlotIds.contains(e.id())).toList();
    }

    private List<IconGridWidget.Entry> currentMemberEntries() {
        List<IconGridWidget.Entry> source = IconCandidates.allTabsOnly();
        Map<String, IconGridWidget.Entry> byId = source.stream()
            .collect(Collectors.toMap(IconGridWidget.Entry::id, e -> e, (a, b) -> a));
        List<IconGridWidget.Entry> result = new ArrayList<>();
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
        if (isNew) ServerPrescriptionCache.adminFull().categories.add(def);
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
        guiGraphics.drawString(font, net.minecraft.client.resources.language.I18n.get("gui.creativemenu.editor.editinglevel", def.opLevel), 8, 4, 0xFFAAAAAA, false);

        iconGrid.render(guiGraphics, mouseX, mouseY);
        removeGrid.render(guiGraphics, mouseX, mouseY);
        addGrid.render(guiGraphics, mouseX, mouseY);

        iconGrid.renderTooltip(guiGraphics, mouseX, mouseY);
        removeGrid.renderTooltip(guiGraphics, mouseX, mouseY);
        addGrid.renderTooltip(guiGraphics, mouseX, mouseY);

        if (dragSource != DragSource.NONE && dragId != null) {
            IconCandidates.allTabsOnly().stream().filter(e -> e.id().equals(dragId)).findFirst()
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
