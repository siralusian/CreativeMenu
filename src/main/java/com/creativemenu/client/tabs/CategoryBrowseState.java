package com.creativemenu.client.tabs;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;

import java.util.List;

/**
 * Zustand für "Kategorie zeigt gerade ein bestimmtes Mitglied" - von allen drei Kategorie-Designs
 * gemeinsam genutzt (Design 0 "Seitenpanel", Design 1 "Item-Auswahl", Design 2 "Slot-Übernahme"),
 * siehe {@code CreativeModeInventoryScreenMixin}. Rein sitzungsbezogen, nicht persistiert.
 * {@code backButtonTab()}/{@code originalPages()} werden NUR von Design 2 genutzt (dort wird
 * {@code this.pages} selbst umgebaut); Design 0/1 lassen die Seitenstruktur unangetastet und
 * tauschen nur den Item-Inhalt der Kategorie aus. {@link #originalItems()} sind die ursprünglichen
 * (Vereinigungs-)Items der Kategorie VOR dem Wechsel - werden beim Verlassen unverändert
 * zurückgeschrieben (siehe {@link com.creativemenu.client.mixin.vanilla.CreativeModeTabAccessor}),
 * damit die Kategorie danach wieder ihre normale "alle Mitglieder vereint"-Ansicht zeigt.
 * {@link #currentMemberIndex()} == -1 ist ein Design-1-Sentinel für "zeigt die Picker-Übersicht,
 * kein Mitglied gewählt".
 */
public class CategoryBrowseState {

    private static CreativeModeTab category;
    // Slot-ID der Kategorie (stabil über Rebuilds hinweg, siehe TabIdentityRegistry) - NICHT dasselbe
    // wie category selbst: die Objektinstanz wechselt bei jedem TabLayoutBuilder.build()-Aufruf,
    // die Slot-ID bleibt gleich. Wird gebraucht, um in CreativeModeInventoryScreenMixin#
    // creativemenu$onSelectTab zu erkennen, ob ein selectTab(...)-Aufruf WIRKLICH zu einem anderen
    // Tab wechselt oder nur dieselbe Kategorie in frischer Instanz erneut auswählt (Live-Test-Fund:
    // Vanillas eigener init()-Code ruft mitten im Wiederaufbau selbst schon selectTab(frischeKategorie)
    // auf, NOCH BEVOR refreshInstances() unten läuft - ein reiner Objektidentitäts-Vergleich hielt
    // das faelschlich für einen Tab-Wechsel und löschte den Browse-Zustand, bevor er wiederhergestellt
    // werden konnte).
    private static String categorySlotId;
    private static List<CreativeModeTab> members;
    private static List<ItemStack> originalItems;
    private static int currentMemberIndex;
    // Eigener, frisch gebauter Zurück-Tab (Nutzer-Vorgabe: statt den echten Operator-Werkzeuge-Tab
    // umzufunktionieren, der sich als hartnäckig gegen Icon-Überzeichnung erwies) - pro Betreten
    // des Browse-Modus neu erzeugt, siehe CreativeModeInventoryScreenMixin#creativemenu$rebuildBrowsePage.
    private static CreativeModeTab backButtonTab;
    // Die ECHTE Seitenliste von vor dem Betreten des Browse-Modus (Vanillas "pages"-Feld ist final,
    // wir mutieren dessen Inhalt beim Betreten/Verlassen statt es zu ersetzen - siehe
    // CreativeModeInventoryScreenMixin#creativemenu$enterBrowseMode/exitBrowseMode) - muss beim
    // Verlassen 1:1 zurueckgeschrieben werden, sonst gehen reale Mehrseiten-Setups (>10 Top-Level-Tabs) kaputt.
    private static List<CreativeTabsScreenPage> originalPages;

    public static boolean isActive() {
        return category != null;
    }

    public static CreativeModeTab category() {
        return category;
    }

    public static String categorySlotId() {
        return categorySlotId;
    }

    public static List<CreativeModeTab> members() {
        return members;
    }

    public static int currentMemberIndex() {
        return currentMemberIndex;
    }

    public static List<ItemStack> originalItems() {
        return originalItems;
    }

    public static void enter(CreativeModeTab categoryTab, List<CreativeModeTab> memberList, List<ItemStack> unionItems) {
        category = categoryTab;
        categorySlotId = TabIdentityRegistry.slotIdOf(categoryTab);
        members = memberList;
        originalItems = unionItems;
        currentMemberIndex = 0;
    }

    public static void setCurrentMemberIndex(int index) {
        currentMemberIndex = index;
    }

    /**
     * Nutzer-Fund: beim Schließen/Wiederöffnen des Kreativmenüs blieb {@code isActive()} zwar
     * korrekt true (nichts löscht {@link #exit()} beim reinen Schließen), aber {@link #category()}/
     * {@link #members()} zeigten noch auf die ALTEN Tab-Instanzen von vor dem Rebuild (synthetische
     * Tabs werden bei jedem {@code TabLayoutBuilder.build()} neu instanziiert) - man landete deshalb
     * wieder im normalen Kategorie-Tab statt in der Browse-Ansicht. Anders als {@link #enter}, das
     * für einen ECHTEN Neueinstieg gedacht ist und {@code currentMemberIndex} bewusst auf 0
     * zurücksetzt, aktualisiert diese Methode nur die (jetzt frischen) Objektreferenzen und behält
     * den zuletzt aktiven Mitglieds-Index bei (auf gültigen Bereich geklemmt, falls sich die
     * Mitgliederzahl geändert hat) - außer er ist -1 (Design 1/Item-Auswahl: "zeigt gerade die
     * Picker-Übersicht, kein Mitglied gewählt"), das bleibt als gültiger Sentinelwert erhalten.
     */
    public static void refreshInstances(CreativeModeTab categoryTab, List<CreativeModeTab> memberList, List<ItemStack> unionItems) {
        category = categoryTab;
        categorySlotId = TabIdentityRegistry.slotIdOf(categoryTab);
        members = memberList;
        originalItems = unionItems;
        if (currentMemberIndex != -1 && (currentMemberIndex < 0 || currentMemberIndex >= memberList.size())) currentMemberIndex = 0;
    }

    public static CreativeModeTab backButtonTab() {
        return backButtonTab;
    }

    public static void setBackButtonTab(CreativeModeTab tab) {
        backButtonTab = tab;
    }

    public static List<CreativeTabsScreenPage> originalPages() {
        return originalPages;
    }

    public static void setOriginalPages(List<CreativeTabsScreenPage> pages) {
        originalPages = pages;
    }

    public static void exit() {
        category = null;
        categorySlotId = null;
        members = null;
        originalItems = null;
        currentMemberIndex = 0;
        backButtonTab = null;
        originalPages = null;
    }
}
