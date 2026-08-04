package com.creativemenu.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;

/**
 * Export/Import über die System-Zwischenablage (Nutzer-Vorgabe) - Minecraft hat keine native
 * Datei-Dialog-API, die Zwischenablage ist der übliche Weg für sowas in Mods. Format ist dasselbe
 * Gson-JSON, das intern schon für die Persistenz genutzt wird.
 */
public class ClipboardUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void copyAsJson(Object value) {
        Minecraft.getInstance().keyboardHandler.setClipboard(GSON.toJson(value));
    }

    /** Liefert null bei leerer/ungültiger Zwischenablage, statt zu werfen. */
    public static <T> T pasteAsJson(Class<T> type) {
        try {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard == null || clipboard.isBlank()) return null;
            return GSON.fromJson(clipboard, type);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
