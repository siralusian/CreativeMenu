package com.creativemenu.client.tabs;

import net.minecraft.world.item.CreativeModeTab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verknüpft die von {@link TabLayoutBuilder} pro Aufruf frisch gebauten Kategorie-Tab-Instanzen mit
 * ihren Mitgliedern, plus den (nicht persistierten, nur pro Sitzung gültigen) Aufklapp-Zustand.
 * Wird von {@code CreativeModeInventoryScreenMixin} gelesen. Absichtlich per Objekt-Identität
 * (CreativeModeTab hat kein eigenes equals/hashCode) statt über Slot-IDs, da die Mixin-Seite nur
 * die tatsächlichen Tab-Instanzen aus {@code currentPage.getVisibleTabs()} kennt.
 */
public class CategoryRegistry {

    private static final Map<CreativeModeTab, List<CreativeModeTab>> MEMBERS = new HashMap<>();
    private static final Set<CreativeModeTab> EXPANDED = new HashSet<>();

    public static void reset() {
        MEMBERS.clear();
    }

    public static void register(CreativeModeTab categoryTab, List<CreativeModeTab> members) {
        MEMBERS.put(categoryTab, members);
    }

    public static boolean isCategory(CreativeModeTab tab) {
        return MEMBERS.containsKey(tab);
    }

    public static List<CreativeModeTab> membersOf(CreativeModeTab tab) {
        return MEMBERS.get(tab);
    }

    public static boolean isExpanded(CreativeModeTab tab) {
        return EXPANDED.contains(tab);
    }

    /** Nur eine Kategorie gleichzeitig aufgeklappt, um Overlay-Überlappungen zu vermeiden. */
    public static void toggle(CreativeModeTab tab) {
        boolean wasExpanded = EXPANDED.contains(tab);
        EXPANDED.clear();
        if (!wasExpanded) EXPANDED.add(tab);
    }

    public static void collapseAll() {
        EXPANDED.clear();
    }
}
