package com.creativemenu.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * EditBox mit Vorschlagsliste. Nutzer-Vorgabe: Vorschläge verschwinden, sobald sie einen
 * klickbaren Bereich (siehe {@link #setProtectedRects(List)}) ganz oder teilweise verdecken würden.
 *
 * Nutzer-Fund: eine feste "immer unterhalb der Box" Platzierung sorgte dafür, dass die Liste bei
 * eng benachbarten geschützten Bereichen (z.B. Buttons direkt unter dem Textfeld) so gut wie NIE
 * angezeigt wurde, selbst mit reichlich freiem Platz OBERHALB der Box. Jetzt wird zuerst unterhalb
 * versucht, bei Überlappung stattdessen oberhalb, und nur wenn BEIDE Richtungen überlappen wird die
 * Liste tatsächlich ausgeblendet.
 *
 * Die Vorschlagsliste wird bewusst NICHT in {@link #renderWidget} gezeichnet (Nutzer-Fund: dadurch
 * landet sie im Zeichen-Stapel unter allen NACH dieser Box hinzugefügten Widgets/Buttons und wirkt
 * "verdeckt"/"aktualisiert sich nicht") - stattdessen ruft die jeweilige Screen
 * {@link #renderDropdown(GuiGraphics, int, int)} explizit ganz am Ende ihrer eigenen render()-Methode
 * auf, damit die Liste garantiert über allem anderen liegt.
 */
public class AutocompleteEditBox extends EditBox {

    private static final int MAX_VISIBLE = 6;
    private static final int ROW_HEIGHT = 12;

    private final Supplier<List<String>> candidateSource;
    private Consumer<String> onPick = s -> {};
    private List<int[]> protectedRects = new ArrayList<>();
    private List<String> currentMatches = List.of();
    private int hoveredIndex = -1;

    public AutocompleteEditBox(Font font, int x, int y, int width, int height, Component message,
            Supplier<List<String>> candidateSource) {
        super(font, x, y, width, height, message);
        this.candidateSource = candidateSource;
        setResponder(s -> refreshMatches());
    }

    public void setOnPick(Consumer<String> onPick) {
        this.onPick = onPick;
    }

    /** Rechtecke (x, y, width, height) die niemals von der Vorschlagsliste verdeckt werden dürfen. */
    public void setProtectedRects(List<int[]> rects) {
        this.protectedRects = rects;
    }

    private void refreshMatches() {
        String text = getValue().toLowerCase(java.util.Locale.ROOT);
        if (text.isBlank()) {
            currentMatches = List.of();
            return;
        }
        List<String> matches = new ArrayList<>();
        for (String candidate : candidateSource.get()) {
            if (candidate.toLowerCase(java.util.Locale.ROOT).contains(text)) {
                matches.add(candidate);
                if (matches.size() >= MAX_VISIBLE) break;
            }
        }
        currentMatches = matches;
        hoveredIndex = -1;
    }

    private boolean isExactMatch() {
        String text = getValue();
        return candidateSource.get().stream().anyMatch(c -> c.equalsIgnoreCase(text));
    }

    private int[] rectBelow() {
        return new int[] {getX(), getY() + getHeight(), getWidth(), currentMatches.size() * ROW_HEIGHT};
    }

    private int[] rectAbove() {
        int h = currentMatches.size() * ROW_HEIGHT;
        return new int[] {getX(), getY() - h, getWidth(), h};
    }

    private boolean overlapsProtected(int[] rect) {
        for (int[] p : protectedRects) {
            if (rectsIntersect(rect, p)) return true;
        }
        return false;
    }

    private static boolean rectsIntersect(int[] a, int[] b) {
        return a[0] < b[0] + b[2] && a[0] + a[2] > b[0] && a[1] < b[1] + b[3] && a[1] + a[3] > b[1];
    }

    /** Bevorzugt unterhalb der Box; weicht bei Überlappung nach oben aus; {@code null} = kein Platz in beiden Richtungen. */
    private int[] bestRect() {
        int[] below = rectBelow();
        if (!overlapsProtected(below)) return below;
        int[] above = rectAbove();
        if (!overlapsProtected(above)) return above;
        return null;
    }

    private boolean isFocusedWithMatches() {
        return isFocused() && !currentMatches.isEmpty() && !isExactMatch();
    }

    /** Von der Screen ganz am Ende ihres render() aufzurufen, damit die Liste über allem anderen liegt. */
    public void renderDropdown(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isFocusedWithMatches()) {
            hoveredIndex = -1;
            return;
        }
        int[] rect = bestRect();
        if (rect == null) {
            hoveredIndex = -1;
            return;
        }

        guiGraphics.fill(rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3], 0xF0101010);
        hoveredIndex = -1;
        for (int i = 0; i < currentMatches.size(); i++) {
            int rowY = rect[1] + i * ROW_HEIGHT;
            boolean hovered = mouseX >= rect[0] && mouseX <= rect[0] + rect[2] && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) hoveredIndex = i;
            int color = hovered ? 0xFFFFFF00 : 0xFFE0E0E0;
            guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, currentMatches.get(i),
                rect[0] + 2, rowY + 2, color, false);
        }
    }

    private boolean dropdownClickable() {
        return isFocusedWithMatches() && bestRect() != null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dropdownClickable() && hoveredIndex >= 0 && hoveredIndex < currentMatches.size()) {
            String picked = currentMatches.get(hoveredIndex);
            setValue(picked);
            currentMatches = List.of();
            onPick.accept(picked);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (dropdownClickable() && keyCode == 257 /* Enter */ && hoveredIndex >= 0) {
            String picked = currentMatches.get(hoveredIndex);
            setValue(picked);
            currentMatches = List.of();
            onPick.accept(picked);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
