package com.creativemenu.client.tabs;

import java.util.ArrayList;
import java.util.List;

/**
 * Die komplette lokale (clientseitige) Kreativmenü-Konfiguration eines Spielers. Persistiert als
 * Gson-JSON im Client-Config-Ordner (siehe {@link ClientTabConfigManager}).
 */
public class LocalTabLayout {

    /** Slot-IDs (echte Tab-ID oder "custom:&lt;uuid&gt;") die ausgeblendet sind. */
    public List<String> hiddenIds = new ArrayList<>();

    public List<CustomTabDefinition> customTabs = new ArrayList<>();

    public List<CategoryDefinition> categories = new ArrayList<>();

    /**
     * Gewünschte Gesamt-Reihenfolge aller Slots (echte Tab-IDs, "custom:&lt;uuid&gt;" und/oder
     * "category:&lt;uuid&gt;"). Slots, die hier nicht auftauchen (z.B. ein neu installierter Mod),
     * werden am Ende in ihrer ursprünglichen Reihenfolge angehängt - siehe {@link TabLayoutBuilder}.
     */
    public List<String> order = new ArrayList<>();

    /**
     * Wie Kategorie-Tabs beim Anklicken dargestellt werden (Nutzer-Vorgabe: mehrere Designs zur
     * Auswahl statt sich auf eins festzulegen) - 0 = Seitenpanel-Overlay (erste Version), 1 =
     * Mitglieder als Items im Grid mit Zurück-Knopf, 2 = Kategorie übernimmt Slot 1, restliche
     * Slots werden zu Mitglieds-Tabs (Default, siehe CategoryBrowseState).
     */
    public int categoryDisplayMode = 2;

    void ensureNonNull() {
        if (hiddenIds == null) hiddenIds = new ArrayList<>();
        if (customTabs == null) customTabs = new ArrayList<>();
        if (categories == null) categories = new ArrayList<>();
        if (order == null) order = new ArrayList<>();
    }
}
