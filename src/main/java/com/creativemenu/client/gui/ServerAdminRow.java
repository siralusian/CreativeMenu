package com.creativemenu.client.gui;

import com.creativemenu.data.ServerCategoryPrescription;
import com.creativemenu.data.ServerCustomTabPrescription;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Eine Zeile in der Server-Admin-Bearbeitungsliste (für die aktuell gewählte OP-Stufe) - Server-Tab oder Server-Kategorie. */
public record ServerAdminRow(String slotId, Component name, ItemStack icon, boolean allowHide, boolean allowSort,
        ServerCustomTabPrescription customDef, ServerCategoryPrescription categoryDef) {

    public boolean isCategory() {
        return categoryDef != null;
    }
}
