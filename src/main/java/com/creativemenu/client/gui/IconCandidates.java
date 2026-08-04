package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ClientTabConfigManager;
import com.creativemenu.client.tabs.CustomTabDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Icon-Kachel-Kandidatenlisten für {@link IconGridWidget} - Anzeigenamen in der Client-Sprache. */
public class IconCandidates {

    private static List<IconGridWidget.Entry> allItems;
    private static List<IconGridWidget.Entry> allTabsOnly;
    private static List<IconGridWidget.Entry> allTabsAndCustom;

    public static List<IconGridWidget.Entry> allItems() {
        if (allItems == null) {
            List<IconGridWidget.Entry> list = new ArrayList<>();
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null || item == Items.AIR) continue;
                ItemStack stack = new ItemStack(item);
                list.add(new IconGridWidget.Entry(id.toString(), stack, stack.getHoverName().getString()));
            }
            allItems = list;
        }
        return allItems;
    }

    public static List<IconGridWidget.Entry> allTabsOnly() {
        if (allTabsOnly == null) {
            allTabsOnly = buildTabEntries();
        }
        return allTabsOnly;
    }

    /** Wie {@link #allTabsOnly()}, plus die aktuell existierenden Custom-Tabs des Spielers. */
    public static List<IconGridWidget.Entry> allTabsAndCustom() {
        List<IconGridWidget.Entry> list = new ArrayList<>(allTabsOnly());
        for (CustomTabDefinition def : ClientTabConfigManager.get().customTabs) {
            String label = (def.name == null || def.name.isBlank()) ? def.slotId() : def.name;
            list.add(new IconGridWidget.Entry(def.slotId(), resolveIcon(def.iconItemId), label));
        }
        return list;
    }

    private static List<IconGridWidget.Entry> buildTabEntries() {
        Set<CreativeModeTab> defaults = new HashSet<>(CreativeModeTabRegistry.getDefaultTabs());
        List<IconGridWidget.Entry> list = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (defaults.contains(tab)) continue;
            ResourceLocation name = CreativeModeTabRegistry.getName(tab);
            if (name == null) continue;
            list.add(new IconGridWidget.Entry(name.toString(), tab.getIconItem(), tab.getDisplayName().getString()));
        }
        return list;
    }

    private static ItemStack resolveIcon(String id) {
        if (id != null && !id.isBlank()) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc != null) {
                Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
                if (item != null && item != Items.AIR) return new ItemStack(item);
            }
        }
        return new ItemStack(Items.CHEST);
    }
}
