package com.creativemenu.client.gui;

import com.creativemenu.client.tabs.CategoryDefinition;
import com.creativemenu.client.tabs.CustomTabDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Eine Zeile in der Editor-Tabliste - echter Tab, Custom-Tab, oder Kategorie-Tab. */
public record TabRow(String slotId, Component name, ItemStack icon, boolean hidden,
        CustomTabDefinition customDef, CategoryDefinition categoryDef) {

    public boolean isCustom() {
        return customDef != null;
    }

    public boolean isCategory() {
        return categoryDef != null;
    }
}
