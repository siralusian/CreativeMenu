package com.creativemenu.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serverseitig vorgeschriebene Kategorie - fasst NUR echte (Original-)Tabs zusammen, keine
 * Custom-Tabs (die sind privat pro Spieler, der Server kennt sie nicht). Die Zusammensetzung
 * (welche Tabs drin sind) ist die Vorschrift; ob/wie der Spieler den resultierenden Kategorie-Tab
 * selbst ein-/ausblenden bzw. verschieben darf, steuern {@link #allowHide}/{@link #allowSort}
 * (siehe TabLayoutBuilder-Merge-Logik). Gilt wie {@link ServerCustomTabPrescription} immer nur
 * für genau eine OP-Stufe.
 */
public class ServerCategoryPrescription {

    public String id = UUID.randomUUID().toString();
    public String name = "";
    public String iconItemId = "";
    public List<String> memberSlotIds = new ArrayList<>();

    public int opLevel = 0;

    public boolean allowHide = true;
    public boolean allowSort = true;

    public String slotId() {
        return "servercategory:" + id;
    }
}
