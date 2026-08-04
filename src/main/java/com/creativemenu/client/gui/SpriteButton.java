package com.creativemenu.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Zeichnet ein Button-Aussehen (exakt dieselben Sprites wie {@code AbstractButton}) an einer
 * beliebigen Stelle, ohne ein echtes Widget zu sein - für Zeilen in eigenen, scissor-geclippten
 * Listen, wo echte {@code Button}-Widgets (Scroll-Offset, Klick-Routing außerhalb des sichtbaren
 * Bereichs) zu viel Verwaltungsaufwand wären.
 */
public class SpriteButton {

    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    public static void draw(GuiGraphics guiGraphics, Font font, int x, int y, int w, int h, String text, boolean hovered) {
        guiGraphics.blitSprite(hovered ? BUTTON_HIGHLIGHTED : BUTTON, x, y, w, h);
        int color = hovered ? 0xFFFFFFA0 : 0xFFE0E0E0;
        guiGraphics.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, color);
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
