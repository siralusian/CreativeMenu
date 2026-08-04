package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.CategoryDefinition;
import com.creativemenu.client.tabs.ClientTabConfigManager;
import com.creativemenu.client.tabs.CustomTabDefinition;
import com.creativemenu.client.tabs.LocalTabLayout;
import com.creativemenu.client.tabs.TabLayoutBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Baut die vollständige Zeilenliste für den Editor - anders als {@link TabLayoutBuilder} (der die
 * echte Kreativmenü-Anzeige baut) werden hier ausgeblendete Tabs NICHT entfernt, sondern nur
 * markiert, damit der Spieler sie im Editor wieder einblenden kann. Nutzt bewusst NICHT
 * {@code CreativeModeTabRegistry.getSortedCreativeModeTabs()} direkt, da unser eigener Mixin dessen
 * Rückgabewert bereits nach der lokalen Konfiguration filtert/umsortiert - für die "Universum aller
 * echten Tabs"-Ansicht im Editor brauchen wir die ungefilterte Registry-Liste.
 */
public class EditorTabRowBuilder {

    public static List<TabRow> buildRows() {
        LocalTabLayout layout = ClientTabConfigManager.get();
        Set<CreativeModeTab> defaultTabs = new HashSet<>(CreativeModeTabRegistry.getDefaultTabs());

        Map<String, CreativeModeTab> realById = new LinkedHashMap<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (defaultTabs.contains(tab)) continue;
            ResourceLocation name = CreativeModeTabRegistry.getName(tab);
            if (name != null) realById.put(name.toString(), tab);
        }

        Set<String> hidden = new HashSet<>(layout.hiddenIds);
        Map<String, CustomTabDefinition> customBySlot = new LinkedHashMap<>();
        for (CustomTabDefinition def : layout.customTabs) {
            customBySlot.put(def.slotId(), def);
        }
        Map<String, CategoryDefinition> categoryBySlot = new LinkedHashMap<>();
        for (CategoryDefinition def : layout.categories) {
            categoryBySlot.put(def.slotId(), def);
        }

        List<TabRow> rows = new ArrayList<>();
        Set<String> placed = new HashSet<>();

        for (String slot : layout.order) {
            if (placed.contains(slot)) continue;
            TabRow row = toRow(slot, realById, customBySlot, categoryBySlot, hidden);
            if (row != null) {
                rows.add(row);
                placed.add(slot);
            }
        }
        for (String slot : realById.keySet()) {
            if (!placed.contains(slot)) {
                rows.add(toRow(slot, realById, customBySlot, categoryBySlot, hidden));
                placed.add(slot);
            }
        }
        for (String slot : customBySlot.keySet()) {
            if (!placed.contains(slot)) {
                rows.add(toRow(slot, realById, customBySlot, categoryBySlot, hidden));
                placed.add(slot);
            }
        }
        for (String slot : categoryBySlot.keySet()) {
            if (!placed.contains(slot)) {
                rows.add(toRow(slot, realById, customBySlot, categoryBySlot, hidden));
            }
        }
        return rows;
    }

    private static TabRow toRow(String slot, Map<String, CreativeModeTab> realById,
            Map<String, CustomTabDefinition> customBySlot, Map<String, CategoryDefinition> categoryBySlot,
            Set<String> hidden) {
        boolean isHidden = hidden.contains(slot);
        if (slot.startsWith("custom:")) {
            CustomTabDefinition def = customBySlot.get(slot);
            if (def == null) return null;
            ItemStack icon = resolveIcon(def.iconItemId);
            String name = (def.name == null || def.name.isBlank()) ? "Custom Tab" : def.name;
            return new TabRow(slot, Component.literal(name), icon, isHidden, def, null);
        } else if (slot.startsWith("category:")) {
            CategoryDefinition def = categoryBySlot.get(slot);
            if (def == null) return null;
            ItemStack icon = resolveIcon(def.iconItemId);
            String name = (def.name == null || def.name.isBlank()) ? "Category" : def.name;
            return new TabRow(slot, Component.literal(name), icon, isHidden, null, def);
        } else {
            CreativeModeTab tab = realById.get(slot);
            if (tab == null) return null;
            return new TabRow(slot, tab.getDisplayName(), tab.getIconItem(), isHidden, null, null);
        }
    }

    private static ItemStack resolveIcon(String id) {
        if (id != null && !id.isBlank()) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
                if (item != null && item != net.minecraft.world.item.Items.AIR) return new ItemStack(item);
            }
        }
        return new ItemStack(net.minecraft.world.item.Items.CHEST);
    }

    public static void persistOrder(List<TabRow> rows) {
        LocalTabLayout layout = ClientTabConfigManager.get();
        List<String> order = new ArrayList<>();
        for (TabRow row : rows) order.add(row.slotId());
        layout.order = order;
        ClientTabConfigManager.save();
    }
}
