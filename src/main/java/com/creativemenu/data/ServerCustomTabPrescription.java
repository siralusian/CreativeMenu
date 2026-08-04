package com.creativemenu.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serverseitig vorgeschriebener Custom-Tab - bewusst eine EIGENE, vom clientseitigen
 * {@code CustomTabDefinition} unabhängige Klasse (kein gemeinsames Common-Package, um zu
 * vermeiden, dass server-seitiger Code jemals von {@code com.creativemenu.client.*} abhängt),
 * auch wenn sich die Felder größtenteils decken. {@link #opLevel} bestimmt, für welche
 * Spieler-OP-Stufe (0-4) diese Vorschrift gilt - ein Spieler kann auch ohne OP im Creative-Modus
 * sein, deshalb ist 0 eine eigene, wählbare Stufe. Nutzer-Vorgabe: jede Vorschrift gilt IMMER nur
 * für genau eine Stufe (der Admin-Editor arbeitet je Sitzung im Kontext einer einzigen Stufe,
 * siehe ServerPrescriptionEditorScreen), keine Mehrfachauswahl mehr.
 */
public class ServerCustomTabPrescription {

    public enum SourceType { ITEMS, TAG, TABS }

    public String id = UUID.randomUUID().toString();
    public String name = "";
    public String iconItemId = "";
    public SourceType sourceType = SourceType.ITEMS;

    public List<String> itemIds = new ArrayList<>();
    public List<String> sourceTabIds = new ArrayList<>();
    public List<List<String>> tagGroups = new ArrayList<>();

    public int opLevel = 0;

    /** Darf der Spieler diesen vorgeschriebenen Tab selbst ein-/ausblenden bzw. verschieben? */
    public boolean allowHide = true;
    public boolean allowSort = true;

    public String slotId() {
        return "servertab:" + id;
    }
}
