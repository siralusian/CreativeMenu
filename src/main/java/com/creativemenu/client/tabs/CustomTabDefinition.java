package com.creativemenu.client.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Eine vom Spieler erstellte "eigene" Tab - entweder eine explizite Item-Liste (Reihenfolge bleibt
 * wie eingegeben), eine 1:1-Kopie eines vorhandenen Tabs, oder eine dynamische Tag-basierte Liste
 * (siehe {@link SourceType}).
 */
public class CustomTabDefinition {

    public enum SourceType { ITEMS, TAG, TABS }

    public String id = UUID.randomUUID().toString();
    public String name = "";
    /** Leer = kein fest gewähltes Icon, Fallback ist dann das erste Item/der erste Original-Tab. */
    public String iconItemId = "";
    public SourceType sourceType = SourceType.ITEMS;

    /** Nur für {@link SourceType#ITEMS}: Item-IDs in gewünschter Reihenfolge. */
    public List<String> itemIds = new ArrayList<>();

    /**
     * Nur für {@link SourceType#TAG}: ODER-verknüpfte Gruppen von UND-verknüpften Tag-IDs (ohne
     * führendes '#') - ein Item passt, wenn es ALLE Tags mindestens EINER Gruppe hat. Eine Gruppe
     * mit nur einem Tag ist einfach "dieser Tag ODER...".
     */
    public List<List<String>> tagGroups = new ArrayList<>();

    /**
     * Nur für {@link SourceType#TABS}: IDs der Original-Tabs (z.B. "minecraft:building_blocks"),
     * deren Items in dieser Reihenfolge zusammengeführt werden - jeder Original-Tab behält dabei
     * seine eigene interne Item-Reihenfolge.
     */
    public List<String> sourceTabIds = new ArrayList<>();

    public String slotId() {
        return "custom:" + id;
    }
}
