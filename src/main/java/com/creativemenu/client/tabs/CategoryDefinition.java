package com.creativemenu.client.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Eine Kategorie-Tab: belegt selbst einen Platz in der Tab-Leiste, zeigt beim Anklicken aber keinen
 * Item-Inhalt an, sondern klappt eine Liste ihrer Mitglieder-Tabs auf (siehe
 * {@code com.creativemenu.client.mixin.vanilla.CreativeModeInventoryScreenMixin} +
 * {@link CategoryRegistry}). Mitglieder werden durch Zuweisung NICHT automatisch im restlichen Menü
 * ausgeblendet (Nutzer-Vorgabe) - Sichtbarkeit bleibt unabhängig über {@link LocalTabLayout#hiddenIds}
 * gesteuert.
 */
public class CategoryDefinition {

    public String id = UUID.randomUUID().toString();
    public String name = "";
    /** Leer = kein fest gewähltes Icon, Fallback ist dann das erste Mitglieds-Icon. */
    public String iconItemId = "";

    /** Slot-IDs (echte Tab-IDs oder "custom:&lt;uuid&gt;") die dieser Kategorie zugewiesen sind. */
    public List<String> memberSlotIds = new ArrayList<>();

    public String slotId() {
        return "category:" + id;
    }
}
