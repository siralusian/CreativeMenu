package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.ServerPrescriptionCache;
import com.creativemenu.data.ServerCategoryPrescription;
import com.creativemenu.data.ServerCustomTabPrescription;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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

/** Baut die Zeilenlisten für {@link ServerPrescriptionEditorScreen}, gefiltert auf eine OP-Stufe. */
public class ServerAdminRowBuilder {

    public static List<ServerAdminRow> customRows(int opLevel) {
        List<ServerAdminRow> rows = new ArrayList<>();
        for (ServerCustomTabPrescription def : ServerPrescriptionCache.adminFull().customTabs) {
            if (def.opLevel != opLevel) continue;
            String name = (def.name == null || def.name.isBlank()) ? "Server Tab" : def.name;
            rows.add(new ServerAdminRow(def.slotId(), Component.literal(name), resolveIcon(def.iconItemId),
                def.allowHide, def.allowSort, def, null));
        }
        return rows;
    }

    public static List<ServerAdminRow> categoryRows(int opLevel) {
        List<ServerAdminRow> rows = new ArrayList<>();
        for (ServerCategoryPrescription def : ServerPrescriptionCache.adminFull().categories) {
            if (def.opLevel != opLevel) continue;
            String name = (def.name == null || def.name.isBlank()) ? "Server Category" : def.name;
            rows.add(new ServerAdminRow(def.slotId(), Component.literal(name), resolveIcon(def.iconItemId),
                def.allowHide, def.allowSort, null, def));
        }
        return rows;
    }

    /** Echte Tabs für {@link ServerOrderScreen} - nie gesperrt, der Server blendet reale Tabs nie aus. */
    public static List<ServerAdminRow> realRows() {
        List<ServerAdminRow> rows = new ArrayList<>();
        Set<CreativeModeTab> defaults = new HashSet<>(CreativeModeTabRegistry.getDefaultTabs());
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (defaults.contains(tab)) continue;
            ResourceLocation name = CreativeModeTabRegistry.getName(tab);
            if (name == null) continue;
            rows.add(new ServerAdminRow(name.toString(), tab.getDisplayName(), tab.getIconItem(), true, true, null, null));
        }
        return rows;
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
