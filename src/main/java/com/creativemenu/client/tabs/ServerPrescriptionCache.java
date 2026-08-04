package com.creativemenu.client.tabs;

import com.creativemenu.data.PrescriptionSet;
import com.creativemenu.data.ServerCategoryPrescription;
import com.creativemenu.data.ServerCustomTabPrescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Clientseitiger Cache der vom Server empfangenen Vorschriften - {@link #filtered} ist der für
 * DIESEN Spieler geltende (nach OP-Level bereits gefilterte) Satz, den {@code TabLayoutBuilder}
 * beim Aufbau des echten Kreativmenüs mit einbezieht. {@link #adminFull} ist der VOLLE,
 * ungefilterte Satz, nur befüllt während der Admin-Editor offen ist.
 */
public class ServerPrescriptionCache {

    private static PrescriptionSet filtered = new PrescriptionSet();
    private static PrescriptionSet adminFull = new PrescriptionSet();

    public static void setFiltered(PrescriptionSet set) {
        filtered = set != null ? set : new PrescriptionSet();
        ensureNonNull(filtered);
    }

    public static void setAdminFull(PrescriptionSet set) {
        adminFull = set != null ? set : new PrescriptionSet();
        ensureNonNull(adminFull);
    }

    public static List<ServerCustomTabPrescription> filteredCustomTabs() {
        return filtered.customTabs;
    }

    public static List<ServerCategoryPrescription> filteredCategories() {
        return filtered.categories;
    }

    /** Vom Admin vorgegebene Positions-Reihenfolge für GENAU diese (des Spielers eigene) OP-Stufe. */
    public static List<String> filteredOrder() {
        return filtered.order;
    }

    public static int minOpLevelAddRemove() {
        return filtered.minOpLevelAddRemove;
    }

    public static int minOpLevelShowHide() {
        return filtered.minOpLevelShowHide;
    }

    public static int minOpLevelSort() {
        return filtered.minOpLevelSort;
    }

    public static PrescriptionSet adminFull() {
        return adminFull;
    }

    private static void ensureNonNull(PrescriptionSet set) {
        if (set.customTabs == null) set.customTabs = new ArrayList<>();
        if (set.categories == null) set.categories = new ArrayList<>();
        if (set.order == null) set.order = new ArrayList<>();
        if (set.orderByLevel == null) set.orderByLevel = new java.util.HashMap<>();
    }
}
