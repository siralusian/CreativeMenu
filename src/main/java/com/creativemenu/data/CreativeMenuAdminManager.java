package com.creativemenu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wer darf die serverseitig vorgeschriebenen Einstellungen (/creativemenu server) bearbeiten.
 * Komplett unabhängig von Vanilla-OP - ein OP hat NICHT automatisch Zugriff, muss erst per
 * "/creativemenu admin <Name>" freigeschaltet werden. Persistiert als Gson-JSON im Weltordner.
 */
public class CreativeMenuAdminManager {

    private static class Data {
        Set<String> admins = new HashSet<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = new Data();
    private static Path dataFile;

    public static void init(MinecraftServer server) {
        dataFile = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("creativemenu_admin.json");
        load();
    }

    public static boolean isAdmin(UUID uuid) {
        return data.admins.contains(uuid.toString());
    }

    /** Konsole (kein Spieler) oder ein bereits berechtigter Admin darf weitere Spieler berechtigen. */
    public static boolean canGrant(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null || isAdmin(player.getUUID());
    }

    public static void grant(UUID uuid) {
        data.admins.add(uuid.toString());
        save();
    }

    public static void revoke(UUID uuid) {
        data.admins.remove(uuid.toString());
        save();
    }

    private static void load() {
        data = new Data();
        if (dataFile == null || !Files.exists(dataFile)) return;
        try (Reader reader = Files.newBufferedReader(dataFile)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) data = loaded;
        } catch (IOException ignored) {}
        if (data.admins == null) data.admins = new HashSet<>();
    }

    private static void save() {
        if (dataFile == null) return;
        try (Writer writer = Files.newBufferedWriter(dataFile)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {}
    }
}
