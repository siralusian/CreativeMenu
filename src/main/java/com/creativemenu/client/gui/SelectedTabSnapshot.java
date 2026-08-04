package com.creativemenu.client.gui;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;

import java.lang.reflect.Field;

/**
 * Liest/setzt das private STATISCHE Feld {@code CreativeModeInventoryScreen.selectedTab} per
 * Reflection (gleiches Muster wie {@code GameModeSwitcherScreenMixin} in CobbleCompanion - simpler
 * als ein Mixin-Accessor für ein statisches Feld, und für einen einmaligen Snapshot/Restore beim
 * Öffnen/Schließen des Editors völlig ausreichend). Grund: die eingebettete Vorschau (siehe
 * {@link EmbeddedCreativeMenuPreview}) ist jetzt interaktiv navigierbar und benutzt denselben
 * statischen Zustand wie das ECHTE, später vom Spieler geöffnete Kreativmenü - ohne Snapshot/Restore
 * würde jedes Herumklicken in der Vorschau merken, welchen Tab der Spieler beim nächsten "e" zu
 * sehen bekommt.
 */
public class SelectedTabSnapshot {

    private static Field field;

    private static Field field() {
        if (field == null) {
            try {
                field = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
                field.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return field;
    }

    public static CreativeModeTab get() {
        try {
            return (CreativeModeTab) field().get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static void set(CreativeModeTab tab) {
        if (tab == null) return;
        try {
            field().set(null, tab);
        } catch (ReflectiveOperationException ignored) {}
    }
}
