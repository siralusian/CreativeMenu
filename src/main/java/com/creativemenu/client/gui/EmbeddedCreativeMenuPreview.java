package com.creativemenu.client.gui;

import com.creativemenu.client.mixin.vanilla.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/**
 * Rendert eine ECHTE {@link CreativeModeInventoryScreen}-Instanz in einen Unterbereich der
 * Editor-Screen - läuft automatisch durch denselben Mixin-Hook wie das reale Kreativmenü, zeigt
 * also exakt die aktuelle lokale Konfiguration (Tabs, Reihenfolge, Kategorien). Nutzer-Vorgabe:
 * muss sich nicht bei jedem Frame/Drag NEU BAUEN - {@link #refresh} wird deshalb nur gezielt
 * aufgerufen (z.B. nach Speichern/Ausblenden), nicht in {@link #render}.
 *
 * Interaktiv (Tab-Wechsel, Seiten-Buttons, Scrollen), ABER: Klicks/Drags innerhalb des eigentlichen
 * Item-Slot-Bereichs (das Inventar-Rechteck selbst) werden blockiert, damit sich in der Vorschau
 * NIEMALS Items ins echte Spieler-Inventar nehmen lassen (Nutzer-Vorgabe). Mausrad-Scrollen wird
 * dagegen immer durchgereicht, da Scrollen allein nie Items bewegt.
 */
public class EmbeddedCreativeMenuPreview {

    private CreativeModeInventoryScreen screen;
    private int areaX, areaY, areaWidth, areaHeight;
    private int dx, dy;

    public void refresh(int areaX, int areaY, int areaWidth, int areaHeight) {
        this.areaX = areaX;
        this.areaY = areaY;
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            screen = null;
            return;
        }
        boolean opBlocks = mc.player.canUseGameMasterBlocks();
        // CreativeModeInventoryScreen's Konstruktor setzt player.containerMenu unconditionally auf
        // sein eigenes ItemPickerMenu (Zeile "player.containerMenu = this.menu;") - für eine reine
        // Vorschau innerhalb einer anderen Screen wollen wir den echten Menü-Zustand des Spielers
        // NICHT verändern, deshalb hier sofort wieder zurücksetzen.
        net.minecraft.world.inventory.AbstractContainerMenu previousMenu = mc.player.containerMenu;
        screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), opBlocks);
        mc.player.containerMenu = previousMenu;
        screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        updateOffsets();
    }

    /** Nur Position/Größe des Vorschaubereichs aktualisieren, ohne die Screen neu zu bauen (z.B. bei resize). */
    public void updateArea(int areaX, int areaY, int areaWidth, int areaHeight) {
        this.areaX = areaX;
        this.areaY = areaY;
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
        updateOffsets();
    }

    private void updateOffsets() {
        if (screen == null) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.creativemenu$getLeftPos();
        int topPos = accessor.creativemenu$getTopPos();
        int imageWidth = accessor.creativemenu$getImageWidth();
        int imageHeight = accessor.creativemenu$getImageHeight();
        dx = (areaX + areaWidth / 2) - (leftPos + imageWidth / 2);
        dy = (areaY + areaHeight / 2) - (topPos + imageHeight / 2);
    }

    private boolean containsScreenPoint(double mouseX, double mouseY) {
        return mouseX >= areaX && mouseX < areaX + areaWidth && mouseY >= areaY && mouseY < areaY + areaHeight;
    }

    /** Das eigentliche Item-Slot-Rechteck (Inventar-Box) in Bildschirm-Koordinaten - hier darf NIE geklickt werden. */
    private boolean withinSlotBox(double mouseX, double mouseY) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int leftPos = accessor.creativemenu$getLeftPos();
        int topPos = accessor.creativemenu$getTopPos();
        int imageWidth = accessor.creativemenu$getImageWidth();
        int imageHeight = accessor.creativemenu$getImageHeight();
        double localX = mouseX - dx;
        double localY = mouseY - dy;
        return localX >= leftPos && localX < leftPos + imageWidth && localY >= topPos && localY < topPos + imageHeight;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (screen == null) return;
        updateOffsets();

        boolean inside = containsScreenPoint(mouseX, mouseY);
        int localMouseX = inside ? mouseX - dx : -9999;
        int localMouseY = inside ? mouseY - dy : -9999;

        guiGraphics.enableScissor(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(dx, dy, 0);
        screen.render(guiGraphics, localMouseX, localMouseY, partialTick);
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (screen == null || !containsScreenPoint(mouseX, mouseY) || withinSlotBox(mouseX, mouseY)) return false;
        return screen.mouseClicked(mouseX - dx, mouseY - dy, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (screen == null || !containsScreenPoint(mouseX, mouseY) || withinSlotBox(mouseX, mouseY)) return false;
        return screen.mouseReleased(mouseX - dx, mouseY - dy, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (screen == null || !containsScreenPoint(mouseX, mouseY) || withinSlotBox(mouseX, mouseY)) return false;
        return screen.mouseDragged(mouseX - dx, mouseY - dy, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (screen == null || !containsScreenPoint(mouseX, mouseY)) return false;
        return screen.mouseScrolled(mouseX - dx, mouseY - dy, scrollX, scrollY);
    }
}
