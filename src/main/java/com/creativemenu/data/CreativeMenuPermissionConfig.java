package com.creativemenu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Globale Berechtigungsschwellen: ab welcher OP-Stufe dürfen Spieler ihr eigenes lokales
 * Kreativmenü selbst verändern (Nutzer-Vorgabe: getrennt nach Hinzufügen/Entfernen,
 * Ein-/Ausblenden, Sortieren). Default 0 = jeder darf (aktuelles Verhalten unverändert), bis der
 * Admin es einschränkt. Persistiert im Weltordner, unabhängig von den eigentlichen
 * Server-Vorschriften ({@link ServerMenuConfigManager}) - reine Rechte-Schwellen, keine
 * Zusammensetzungs-Vorschrift.
 */
public class CreativeMenuPermissionConfig {

    private static class Data {
        int minOpLevelAddRemove = 0;
        int minOpLevelShowHide = 0;
        int minOpLevelSort = 0;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();
    private static Path dataFile;

    public static void init(MinecraftServer server) {
        dataFile = server.getWorldPath(LevelResource.ROOT).resolve("creativemenu_permissions.json");
        load();
    }

    public static int minOpLevelAddRemove() {
        return data.minOpLevelAddRemove;
    }

    public static int minOpLevelShowHide() {
        return data.minOpLevelShowHide;
    }

    public static int minOpLevelSort() {
        return data.minOpLevelSort;
    }

    public static void setAddRemove(int level) {
        data.minOpLevelAddRemove = level;
        save();
    }

    public static void setShowHide(int level) {
        data.minOpLevelShowHide = level;
        save();
    }

    public static void setSort(int level) {
        data.minOpLevelSort = level;
        save();
    }

    private static void load() {
        data = new Data();
        if (dataFile == null || !Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) data = loaded;
        } catch (IOException ignored) {}
    }

    private static void save() {
        if (dataFile == null) return;
        try (Writer writer = Files.newBufferedWriter(dataFile)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {}
    }
}
