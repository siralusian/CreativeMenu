package com.creativemenu.client.gui;

/**
 * Merkt sich die Slot-ID (nicht die Tab-Objektinstanz - siehe {@link
 * com.creativemenu.client.tabs.TabIdentityRegistry}) des zuletzt ausgewählten Tabs, um eine
 * gemerkte Auswahl über einen Tab-Rebuild hinweg wiederzufinden (siehe
 * {@code CreativeModeInventoryScreenMixin}/{@code CreativeModeTabRegistryMixin}). Als reiner
 * String übersteht dieser Zustand einen Rebuild trivial, im Gegensatz zur Vanilla-Tab-Objektreferenz
 * selbst.
 */
public class SelectedSlotTracker {

    private static String lastSlotId;

    public static void record(String slotId) {
        if (slotId != null) lastSlotId = slotId;
    }

    public static String get() {
        return lastSlotId;
    }

    public static void set(String slotId) {
        lastSlotId = slotId;
    }
}
