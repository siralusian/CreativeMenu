package com.creativemenu.client.tabs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lädt/speichert die lokale Kreativmenü-Konfiguration des Spielers. Getrennt von der (noch nicht
 * gebauten) serverseitig vorgeschriebenen Konfiguration - siehe
 * {@code com.creativemenu.data.ServerMenuConfigManager}.
 */
public class ClientTabConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LocalTabLayout layout;

    public static LocalTabLayout get() {
        if (layout == null) load();
        return layout;
    }

    public static void save() {
        if (layout == null) return;
        try (Writer writer = Files.newBufferedWriter(configFile())) {
            GSON.toJson(layout, writer);
        } catch (IOException ignored) {}
    }

    public static void resetAll() {
        layout = new LocalTabLayout();
        save();
    }

    /** Für Import (Zwischenablage) - ersetzt die komplette Konfiguration. */
    public static void replaceAll(LocalTabLayout newLayout) {
        layout = newLayout != null ? newLayout : new LocalTabLayout();
        layout.ensureNonNull();
        save();
    }

    private static void load() {
        Path file = configFile();
        if (!Files.exists(file)) {
            layout = new LocalTabLayout();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            LocalTabLayout parsed = GSON.fromJson(reader, LocalTabLayout.class);
            layout = parsed != null ? parsed : new LocalTabLayout();
        } catch (IOException | JsonSyntaxException e) {
            layout = new LocalTabLayout();
        }
        layout.ensureNonNull();
    }

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve("creativemenu_client.json");
    }
}
