package com.creativemenu.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reines Übertragungsobjekt für Netzwerkpakete - wird per Gson zu/von JSON serialisiert (siehe
 * die drei Pakete in {@code com.creativemenu.network}, die alle nur ein einzelnes JSON-String-Feld
 * transportieren statt eigener StreamCodecs für diese verschachtelte Struktur).
 *
 * {@link #order} ist die Positions-Vorschrift für GENAU EINE OP-Stufe (befüllt im gefilterten
 * Sync an einzelne Spieler - siehe {@code ServerPrescriptionSyncPacket}). {@link #orderByLevel}
 * ist das Gegenstück für den ungefilterten Admin-Sync (alle Stufen gleichzeitig, siehe
 * {@code ServerPrescriptionAdminSyncPacket}) - jeweils nur eines der beiden ist befüllt, das
 * andere bleibt leer/ungenutzt. Die drei minOpLevel-Felder sind die globalen
 * Berechtigungsschwellen (siehe {@code CreativeMenuPermissionConfig}), an jeden Spieler mit
 * seinem gefilterten Sync mitgeschickt.
 */
public class PrescriptionSet {

    public List<ServerCustomTabPrescription> customTabs = new ArrayList<>();
    public List<ServerCategoryPrescription> categories = new ArrayList<>();
    public List<String> order = new ArrayList<>();
    public Map<Integer, List<String>> orderByLevel = new HashMap<>();

    public int minOpLevelAddRemove = 0;
    public int minOpLevelShowHide = 0;
    public int minOpLevelSort = 0;

    public PrescriptionSet() {}

    public PrescriptionSet(List<ServerCustomTabPrescription> customTabs, List<ServerCategoryPrescription> categories) {
        this.customTabs = customTabs;
        this.categories = categories;
    }
}
