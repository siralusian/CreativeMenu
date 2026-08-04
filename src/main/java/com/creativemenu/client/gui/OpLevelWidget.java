package com.creativemenu.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Fünf Kacheln (OP-Stufe 0-4), SINGLE-Select - der Admin-Editor arbeitet je Sitzung im Kontext
 * genau EINER Stufe (Nutzer-Vorgabe), keine Mehrfachauswahl mehr wie in einer früheren Version.
 * Ein Spieler kann auch ohne OP im Kreativmodus sein, daher zählt Stufe 0 als eigene, wählbare Stufe.
 */
public class OpLevelWidget {

    private static final int CELL_W = 22;
    private static final int CELL_H = 16;
    private static final int GAP = 2;

    private final int x, y;
    private final IntSupplier getter;
    private final IntConsumer setter;

    public OpLevelWidget(int x, int y, IntSupplier getter, IntConsumer setter) {
        this.x = x;
        this.y = y;
        this.getter = getter;
        this.setter = setter;
    }

    public static int width() {
        return 5 * CELL_W + 4 * GAP;
    }

    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        int current = getter.getAsInt();
        for (int level = 0; level <= 4; level++) {
            int cx = x + level * (CELL_W + GAP);
            boolean selected = current == level;
            boolean hovered = SpriteButton.isHovered(mouseX, mouseY, cx, y, CELL_W, CELL_H);
            SpriteButton.draw(guiGraphics, font, cx, y, CELL_W, CELL_H, String.valueOf(level), selected || hovered);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        for (int level = 0; level <= 4; level++) {
            int cx = x + level * (CELL_W + GAP);
            if (SpriteButton.isHovered((int) mouseX, (int) mouseY, cx, y, CELL_W, CELL_H)) {
                setter.accept(level);
                return true;
            }
        }
        return false;
    }
}
