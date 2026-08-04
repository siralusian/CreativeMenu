package com.creativemenu.client.tabs;

import net.minecraft.world.item.CreativeModeTab;

import java.util.HashMap;
import java.util.Map;

/**
 * Verknüpft die von {@link TabLayoutBuilder} pro Aufruf frisch aufgebaute Tab-Menge (real UND
 * synthetisch) mit ihrer stabilen Slot-ID, in beide Richtungen. Grund: {@code CreativeModeTab} hat
 * kein eigenes {@code equals()}/{@code hashCode()}, und synthetische Tabs (Custom/Kategorie/Server)
 * werden bei JEDEM Aufbau neu instanziiert - Vanillas eigener, identitätsbasierter Abgleich des
 * statischen {@code selectedTab}-Felds würde eine gemerkte Auswahl über einen Rebuild hinweg sonst
 * nie wiederfinden. Siehe {@code CreativeModeTabRegistryMixin}/{@code CreativeModeInventoryScreenMixin}.
 */
public class TabIdentityRegistry {

    private static final Map<String, CreativeModeTab> BY_SLOT = new HashMap<>();
    private static final Map<CreativeModeTab, String> SLOT_OF = new HashMap<>();

    public static void reset() {
        BY_SLOT.clear();
        SLOT_OF.clear();
    }

    public static void register(String slotId, CreativeModeTab tab) {
        BY_SLOT.put(slotId, tab);
        SLOT_OF.put(tab, slotId);
    }

    public static CreativeModeTab bySlotId(String slotId) {
        return slotId == null ? null : BY_SLOT.get(slotId);
    }

    public static String slotIdOf(CreativeModeTab tab) {
        return tab == null ? null : SLOT_OF.get(tab);
    }
}
