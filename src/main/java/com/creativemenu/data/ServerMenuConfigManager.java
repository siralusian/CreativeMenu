package com.creativemenu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serverseitig vorgeschriebene Kreativmenü-Einstellungen, getrennt von der lokalen
 * Client-Konfiguration (siehe {@code com.creativemenu.client.tabs.ClientTabConfigManager}).
 * Persistiert im Weltordner - jede Instanz eines Servers hat ihre eigenen Vorschriften.
 */
public class ServerMenuConfigManager {

    private static class Data {
        List<ServerCustomTabPrescription> customTabs = new ArrayList<>();
        List<ServerCategoryPrescription> categories = new ArrayList<>();
        Map<Integer, List<String>> orderByLevel = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();
    private static Path dataFile;

    public static void init(MinecraftServer server) {
        dataFile = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("creativemenu_server_config.json");
        load();
    }

    public static List<ServerCustomTabPrescription> allCustomTabs() {
        return data.customTabs;
    }

    public static List<ServerCategoryPrescription> allCategories() {
        return data.categories;
    }

    /** 0-4, unabhängig davon ob der Spieler tatsächlich "geoppt" ist (0 = ganz normaler Spieler). */
    public static int opLevelOf(ServerPlayer player) {
        return player.getServer().getProfilePermissions(player.getGameProfile());
    }

    public static List<ServerCustomTabPrescription> customTabsFor(int opLevel) {
        return data.customTabs.stream().filter(p -> p.opLevel == opLevel).toList();
    }

    public static List<ServerCategoryPrescription> categoriesFor(int opLevel) {
        return data.categories.stream().filter(p -> p.opLevel == opLevel).toList();
    }

    public static List<String> orderFor(int opLevel) {
        return data.orderByLevel.getOrDefault(opLevel, List.of());
    }

    public static Map<Integer, List<String>> allOrderByLevel() {
        return data.orderByLevel;
    }

    /** Gefilterter Satz für einen einzelnen Spieler (S2C-Sync), inklusive Positions-Vorschrift für genau seine Stufe. */
    public static PrescriptionSet filteredSetFor(int opLevel) {
        PrescriptionSet set = new PrescriptionSet(customTabsFor(opLevel), categoriesFor(opLevel));
        set.order = new ArrayList<>(orderFor(opLevel));
        return set;
    }

    /** Voller, ungefilterter Satz für den Admin-Editor (alle Stufen gemischt, siehe PrescriptionSet). */
    public static PrescriptionSet fullSet() {
        PrescriptionSet set = new PrescriptionSet(allCustomTabs(), allCategories());
        set.orderByLevel = allOrderByLevel();
        return set;
    }

    /** Ersetzt den kompletten Datensatz (Admin-Editor sendet die volle, bearbeitete Liste). */
    public static void replaceAll(List<ServerCustomTabPrescription> customTabs, List<ServerCategoryPrescription> categories,
            Map<Integer, List<String>> orderByLevel) {
        data.customTabs = new ArrayList<>(customTabs);
        data.categories = new ArrayList<>(categories);
        data.orderByLevel = orderByLevel != null ? new HashMap<>(orderByLevel) : new HashMap<>();
        save();
    }

    public static void resetAll() {
        data = new Data();
        save();
    }

    private static void load() {
        data = new Data();
        if (dataFile == null || !Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) data = loaded;
        } catch (IOException ignored) {}
        if (data.customTabs == null) data.customTabs = new ArrayList<>();
        if (data.categories == null) data.categories = new ArrayList<>();
        if (data.orderByLevel == null) data.orderByLevel = new HashMap<>();
    }

    private static void save() {
        if (dataFile == null) return;
        try (Writer writer = Files.newBufferedWriter(dataFile)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {}
    }
}
