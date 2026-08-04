# CreativeMenu – Roadmap

Eigenständige NeoForge-Mod (MC 1.21.1 / NeoForge 21.1.233) zum Sortieren, Gruppieren und
Aus-/Einblenden von Creative-Menü-Tabs. Keine Cobblemon-Abhängigkeit.

## Phase 0 – Fundament (erledigt)
- Eigenständiges Gradle/NeoForge-Projekt (build.gradle, gradle.properties, settings.gradle, Wrapper)
- Mod-Grundgerüst (`CreativeMenu`, `CreativeMenuClient`), `neoforge.mods.toml`
- Commands `/creativemenu` (Alias `/crm`): `admin <Name>`, `open`, `server`, `resett true`, `server resett true`
- Berechtigungsmodell: `CreativeMenuAdminManager` (Konsole oder bestehender Admin schaltet weitere Admins frei), unabhängig von Vanilla-OP
- Netzwerk-Grundgerüst: `OpenEditorPacket` (S2C, öffnet Platzhalter-Editor-Screen lokal/server), `ResetLocalConfigPacket` (S2C, löscht lokale Config-Datei)
- Getrennte Config-Dateien: `creativemenu_admin.json` + `creativemenu_server_config.json` (Weltordner), `creativemenu_client.json` (Client-Config-Ordner) – Datenmodell noch leer
- EN/DE-Lokalisierung von Anfang an (`en_us.json` / `de_de.json`)
- Platzhalter-Editor-Screen (Titel + Schließen-Button) – beweist den kompletten Pfad Command → Berechtigung → Paket → Screen

## Phase 1 – Eigene Tabs & Sichtbarkeit (lokal) - erledigt
- Einziger Eingriffspunkt: Mixin auf `CreativeModeTabRegistry.getSortedCreativeModeTabs()` (NeoForge) -
  liefert Rendering UND Klick-Handling im echten Creative-Screen automatisch konsistent aus (per
  Analyse der Vanilla/NeoForge-Quellen verifiziert, kein separates Rendering-/Klick-Mixin nötig)
- Ein-/Ausblenden beliebiger Original- UND Custom-Tabs (`hiddenIds`)
- Freie Reihenfolge aller Tabs (`order`), neu hinzugekommene Mods werden automatisch ans Ende angehängt
- Eigene Tabs mit 3 Quelltypen: explizite Item-Liste, Tag-basiert (dynamisch), oder Zusammenführung
  mehrerer ganzer Original-/Custom-Tabs (jeder behält seine eigene interne Reihenfolge)
- Editor-GUI (`/creativemenu open`): Drag&Drop-Liste (Maus, kein Buttons), Ein-/Ausblenden-Link pro
  Zeile, Live-Vorschau-Streifen (Icons in Original-Seitenaufteilung, mit Umblättern), Autocomplete-
  Textfelder (Item-/Tag-/Tab-IDs) die sich ausblenden, sobald sie einen klickbaren Bereich verdecken würden

## Phase 2 – Kategorie-Tabs & Tag-Tabs - Tag-Tabs erledigt, Kategorie-Dropdown gebaut aber ungetestet
- Tag-basierte Tabs: Teil von Phase 1 (siehe oben, SourceType.TAG)
- Kategorie-Tabs: eigener Slot in der Tab-Leiste, klappt beim Anklicken eine Liste ihrer Mitglieder
  (echte Tabs UND/ODER Custom-Tabs) direkt über/unter sich auf - oben, wenn der Tab zur oberen
  Zeile der Seite gehört, unten wenn zur unteren (zusätzlicher Mixin `CreativeModeInventoryScreenMixin`
  auf den echten Screen: render-TAIL zum Zeichnen, mouseReleased-HEAD fürs Klick-Handling). Bewusst so
  gebaut, dass ein Fehler hier NUR Kategorie-Tabs betrifft - alle anderen Tabs laufen unverändert über
  die normale Vanilla-Logik, selbst falls dieser Teil einen Bug hätte.
  **Wichtig: bisher nur durch Quellcode-Analyse geprüft, noch nicht in-game getestet** (siehe
  Klassenkommentar) - Richtung/Abstände/Sichtbarkeit bei vielen Mitgliedern müssen im Spiel verifiziert
  werden, bevor Phase 3 darauf aufbaut.

## Phase 2.5 – In-Game- und GUI-Feedback-Runde 2 (erledigt, teils ungetestet)
Zwei vom Nutzer im Spiel gefundene Bugs behoben:
- Autocomplete-Vorschlagsliste zeigte sich gar nicht mehr (Dropdown wurde immer nur unterhalb der Box
  versucht, blockierte praktisch immer an eng benachbarten Buttons) - jetzt Ausweich-Logik: unten
  versuchen, bei Überlappung automatisch oben versuchen, nur bei beidseitiger Blockade wirklich ausblenden.
- Kategorien verloren ausgeblendete Mitglieder (Custom-Tabs/Kategorien wurden beim Bauen übersprungen,
  sobald sie selbst versteckt waren) - Ausblenden steuert jetzt nur noch die Sichtbarkeit im Haupt-Balken,
  nicht mehr die Existenz als Kategorie-Mitglied.

Danach eine große Design-Überarbeitung (Nutzer-Vorgabe, komplett umgesetzt, siehe Klassenkommentare):
- Ingame: Kategorie-Dropdown kollidierte mit dem Seiten-Wechseln-Button (Klick landete zusätzlich auf
  dem dahinterliegenden Button, da Button-Presses in mouseCLICKED feuern, nicht erst in mouseReleased) -
  behoben durch zusätzlichen mouseClicked-HEAD-Abfang für den gesamten Overlay-Bereich. Kategorie-
  Mitglieder-Kacheln nutzen jetzt die echten Vanilla-Tab-Sprites statt einer Flächenfüllung.
- Editor-Startbildschirm: ECHTE eingebettete `CreativeModeInventoryScreen`-Instanz als Vorschau (nur
  gezielt neu gebaut, nicht pro Frame - siehe `EmbeddedCreativeMenuPreview`), feste (nicht mehr per Maus
  verschiebbare) Listen mit optionalem Scrollbalken, zwei getrennte Listen "Tabs"/"Kategorien" links/
  rechts der Vorschau, "Sortieren"-Button, zentrierte Zurücksetzen(+Bestätigung)/Schließen-Buttons,
  zweizeilige Listeneinträge mit echten Button-Texturen für Bearbeiten/Ein-Ausblenden.
- Neuer/Bearbeiten-Screens (Custom-Tab + Kategorie): Dreispalten-Layout, Icon-Textfeld ohne
  Autocomplete-Dropdown, stattdessen live gefilterte Icon-Kachel-Liste (Item-ID UND lokalisierter
  Name), gleiches Muster für Hinzufügen/Entfernen-Panels mit Tooltip bei Mouseover.
- Neuer Sortieren-Screen (`SortScreen`): Seiten-Blöcke (5×2-Kacheln wie im echten Menü) mit Text-
  Reflow-Drag&Drop, links/rechts Listen ausgeblendeter Tabs/Kategorien mit Suchleiste - Kachel rein
  ziehen aktiviert, Kachel raus in eine der beiden Listen ziehen deaktiviert (unabhängig welche Liste).

**Verifikationsstand:** Ingame-Klick-Kollisions-Fix + Textur-Wechsel wurden per `runClient` mit dem
kompletten Mod-Set (Curios/Cobblemon/Create/CustomNPCs/RCT) gegengetestet - kein Crash, Mixin greift
sauber. Die GUI-Screens selbst (eingebettete Vorschau, Icon-Kachel-Listen, Sortieren-Drag&Drop) sind
NUR durch Kompilieren + Code-Review geprüft - das öffnen der Screens braucht einen Ingame-Chat-Befehl,
den diese Session nicht selbst auslösen kann. Nutzer-Test vor Phase 3 nötig.

## Phase 2.6 – Feedback-Runde 4 nach erstem GUI-Test (erledigt, teils ungetestet)
- Vorschau/Sortieren-Spalte im Startbildschirm halbiert, Freiraum an die beiden Listen verteilt.
- Vorschau ist jetzt interaktiv (Tab-Wechsel, Seiten-Buttons, Scrollen durchgereicht an die echte
  eingebettete Screen), Klicks im Item-Slot-Bereich selbst bleiben aber blockiert (kein Item-Diebstahl
  möglich). `SelectedTabSnapshot` sichert/stellt das geteilte statische `selectedTab`-Feld beim
  Öffnen/Schließen des Editors wieder her.
- Standard-Icon "minecraft:chest" entfernt (leeres Feld = alle Icons sichtbar, Fallback bleibt intakt).
- Icon-Kachel-Listen für Hinzufügen/Entfernen komplett neu: Klick auf eine Kachel überträgt direkt in
  die jeweils andere Liste (oder per Drag&Drop), Umsortieren per Drag&Drop innerhalb der aktuellen
  Liste - die Suchleiste tippt nicht mehr automatisch den Namen rein, bleibt nur noch exakter
  Text-Pfad für den Hinzufügen/Entfernen-Button.
- Tag-Modus komplett neu: `tagGroups` (ODER-verknüpfte UND-Gruppen) statt einem einzelnen Tag, echte
  Vorschlagsfunktion, Liste der Tag-Gruppen mit "+"-Button pro Zeile (UND-Erweiterung) und
  Haupt-Hinzufügen-Button (neue ODER-Zeile).
- Kategorie-Kacheln: engerer Abstand + Umbruch in mehrere Reihen ab 4 Mitgliedern (Rate ohne
  Referenzbild, im Spiel gegenprüfen).
- Erneut per `runClient` (volles Mod-Set) gegengetestet: kein Crash. GUI-Interaktionen selbst wieder
  nur code-geprüft.

## Phase 3 – Server-Vorschriften (erledigt, ungetestet)
- Eigenes, vom lokalen Modell unabhängiges Datenmodell: `ServerCustomTabPrescription` /
  `ServerCategoryPrescription` (+ `PrescriptionSet`-DTO), persistiert über `ServerMenuConfigManager`
  in `creativemenu_server_config.json` (Weltordner). Server-Kategorien referenzieren bewusst nur
  echte Original-Tabs, keine privaten Custom-Tabs einzelner Spieler.
- Geltung nach OP-Level (0-4, Stufe 0 = kein OP aber trotzdem Kreativmodus möglich) pro Vorschrift
  einzeln wählbar (`opLevels`-Set), gefiltert über `ServerMenuConfigManager.customTabsFor`/`categoriesFor`.
- Netzwerk: `ServerPrescriptionSyncPacket` (S2C, gefiltert, bei Login + nach jedem Speichern an alle
  Online-Spieler), `ServerPrescriptionAdminSyncPacket` (S2C, ungefiltert, nur beim Öffnen des
  Admin-Editors), `ServerPrescriptionSaveRequestPacket` (C2S, serverseitig NOCHMAL gegen
  `CreativeMenuAdminManager.isAdmin` geprüft, unabhängig vom Command-Zugriffsschutz).
- Merge-Logik: `TabLayoutBuilder` baut Server-Vorschriften (gefiltert über `ServerPrescriptionCache`)
  in denselben synthetischen Tab-Topf wie lokale Custom-Tabs/Kategorien ein (`"servertab:"`/
  `"servercategory:"`-Slot-IDs) - dadurch bestimmt der Server nur die ZUSAMMENSETZUNG, die Position
  in der Reihenfolge bleibt (wie bei neu installierten Mod-Tabs) Sache des einzelnen Spielers.
- Admin-Editor (`/creativemenu server`): eigener `ServerPrescriptionEditorScreen` (kein Platzhalter
  mehr) mit getrennten Listen für Server-Tabs/-Kategorien, Bearbeiten/Löschen pro Zeile,
  `ServerCustomTabEditScreen`/`ServerCategoryEditScreen` (gleiches 3-Spalten-Layout wie die lokalen
  Pendants) inkl. OP-Stufen-Mehrfachauswahl (`OpLevelWidget`). "Speichern" schickt den kompletten
  bearbeiteten Satz zum Server; "Schließen" verwirft unge­speicherte Änderungen nur im Client-Speicher
  (nächstes Öffnen holt ohnehin einen frischen Satz vom Server).

**Verifikationsstand:** Nur durch Kompilieren + Code-Review geprüft (keine der Phase-3-Änderungen
betrifft Mixins, daher kein zusätzlicher `runClient`-Crashtest nötig). Der komplette Server-Editor-
Ablauf (Rechte-Check, Speichern/Verteilen, Merge im echten Kreativmenü) braucht einen Ingame-Test mit
mindestens zwei Spielern/Sitzungen (ein Admin + ein Nicht-Admin auf verschiedenen OP-Stufen), den
diese Session nicht selbst auslösen kann.

**Nachtrag Phase 4:** `opLevels`-Set + `OpLevelWidget`-Mehrfachauswahl aus diesem Abschnitt wurden in
Phase 4 durch ein Einzelstufen-Modell ersetzt (siehe unten) - dieser Abschnitt beschreibt den Stand
VOR Phase 4, nicht mehr den aktuellen Code.

## Phase 4 – Feedback-Runde nach Live-Server-Test (erledigt, größtenteils ungetestet)

Live-Test von Phase 1-3 deckte mehrere Bugs und einen großen Batch neuer Anforderungen auf, alle in
einem Rutsch durchgezogen (Nutzer-Vorgabe, kein Zwischen-Check-in):

**Bugfixes:**
- Versteckter Custom-Tab/Kategorie erschien trotzdem: `TabLayoutBuilder`s finale `synthById`-Fallback-
  Schleife hatte (anders als die `realById`-Schleife direkt davor) keinen `!hidden.contains(...)`-Guard.
- Items/Tags/ganze Tabs ließen sich nicht kombinieren: `resolveSourceItems` vereint jetzt immer alle
  drei Quellen (dedupliziert nach `Item`-Identität) statt exklusiv nach `sourceType` zu schalten - die
  3 Ansicht-Buttons in den Editoren steuern nur noch, welches Panel sichtbar ist, nicht mehr den Inhalt.
- Bereits enthaltene Items/Tabs/Mitglieder tauchten weiterhin in der Kandidatenliste auf: alle 4
  Editor-Screens filtern die Add-Kandidaten jetzt gegen die aktuelle Liste.
- Tab-Auswahl ging beim Schließen/Wiederöffnen verloren (nur bei Custom-Tabs/Kategorien, reale Tabs
  unbetroffen): `CreativeModeTab` hat kein `equals()`/`hashCode()`, synthetische Tabs werden bei jedem
  `TabLayoutBuilder.build()` neu instanziiert. Fix: `TabIdentityRegistry` (Slot-ID ↔ Tab-Instanz pro
  Build) + `SelectedSlotTracker` (merkt sich die zuletzt gewählte Slot-ID statt der Objektreferenz) -
  `CreativeModeTabRegistryMixin` schreibt Vanillas statisches `selectedTab`-Feld direkt nach dem Build
  auf die frische Instanz zurück, bevor Vanillas eigene (identitätsbasierte) Auflösung läuft.
- Ein-/Ausblenden-Button in der Startbildschirm-Liste entfernt (redundant seit `SortScreen`), reale
  Tabs sind dort komplett raus - werden nur noch über Sortieren verwaltet.

**Server-Vorschriften umgebaut:**
- `Set<Integer> opLevels` → einzelnes `int opLevel` pro Vorschrift (immer nur eine Stufe). Neue Felder
  `allowHide`/`allowSort` (statt der alten Mehrfachauswahl-Buttons).
- `ServerPrescriptionEditorScreen`: Einzelstufen-Wähler oben (`OpLevelWidget`, jetzt Single-Select statt
  Multi-Select), filtert beide Listen; neue Vorschriften werden mit der gewählten Stufe gestempelt.
- `ServerCustomTabEditScreen`/`ServerCategoryEditScreen`: Anzeige "Bearbeite für OP-Stufe: N" +
  `allowHide`/`allowsort`-Umschalter statt der alten Mehrfachauswahl.
- Neuer `ServerOrderScreen` (Vorschau+Sortieren fürs Server-Menü, war komplett unmöglich): Seiten-Block-
  Fläche aus echten Tabs + Vorschriften der gewählten Stufe, gesperrte (`allowSort=false`) Kacheln sind
  abgedunkelt/nicht greifbar. `TabLayoutBuilder` platziert gesperrte Slots zuerst (feste, vom Admin
  vorgegebene Reihenfolge), ignoriert lokales Verstecken bei `allowHide=false`.
- Neue globale Berechtigungsschwellen (`CreativeMenuPermissionConfig`, `creativemenu_permissions.json`):
  ab welcher OP-Stufe dürfen Spieler selbst Hinzufügen/Entfernen, Ein-/Ausblenden, Sortieren - neue
  Unterbefehle `/creativemenu permissions get`/`set <addremove|showhide|sort> <0-4>`. Durchsetzung
  clientseitig über `player.hasPermissions(...)` (kein eigener Sync nötig, Vanilla synct das schon).

**Kategorie-Darstellung (3 Designs zur Wahl, Nutzer-Vorgabe: nicht auf eins festlegen):**
- Design 0 (Seitenpanel-Overlay, ursprüngliche Version) bleibt erhalten.
- Design 2 "Slot-Übernahme" (neuer Default) gebaut: Kategorie übernimmt Slot 1, übernimmt Vanillas
  eigenen `selectedTab` durchgängig (kein extra Hervorhebungs-Hack nötig), Item-Inhalt wird beim
  Mitglied-Wechsel per `CreativeModeTabAccessor` ausgetauscht, `currentPage` wird durch eine
  synthetische `CreativeTabsScreenPage` (Kategorie + bis zu 8 Mitglieder + echter Operator-Werkzeuge-
  Tab als Zurück-Button) ersetzt. **Bekannte Einschränkung:** mehr als 8 Mitglieder werden aktuell
  abgeschnitten, keine echte 2. Unterseite.
- Design 1 "Mitglieder als Items im Grid" NICHT umgesetzt (bräuchte Eingriffe ins Item-Slot-
  Klicksystem selbst - deutlich höheres Risiko, bewusst für eine eigene Runde zurückgestellt).
- Neuer Design-Umschalter im Editor-Startbildschirm (zyklet durch die 3 Optionen, Design 2 = Default).

**GUI-Skalierung:** `FixedScaleScreen`-Muster aus CobbleCompanion portiert (dort selbst unbenutzt/
ungetestet) und um `mouseReleasedScaled`/`mouseDraggedScaled`-Hooks erweitert (CreativeMenus Screens
brauchen mehr Drag&Drop als die CobbleCompanion-Vorlage bot). Alle 8 Screens migriert, GUI-Größe
2-Äquivalent unabhängig von der tatsächlichen Einstellung. Größter Unsicherheitsfaktor: die eingebettete
Live-Vorschau (`EmbeddedCreativeMenuPreview`) rendert einen zweiten echten Screen und musste auf
Echt-Koordinaten + `renderInRealSpace()` umgestellt werden - das ist der am wenigsten erprobte Teil.

**Export/Import:** über die System-Zwischenablage (`ClipboardUtil`, Gson-JSON, keine native Datei-API
verfügbar) - Startbildschirm exportiert/importiert die komplette lokale Konfiguration, alle vier
Tab-/Kategorie-Editoren (lokal + Server) exportieren/importieren nur ihren eigenen Datensatz
(id/Slot-ID/OP-Stufe bleiben beim Import erhalten, nur Inhalt wird ersetzt).

**Verifikationsstand:** Alle Phasen kompilieren einzeln sauber. Mixin-relevante Teile (Auswahl-Fix,
Design-2-Slot-Übernahme) liefen zweimal crashfrei durch `runClient` (kompletter Modset, bis nach dem
Laden von Shadern/Modellen/Partikeln/Animationen - kein Mixin-Apply-Fehler). GUI-Skalierung und
Export/Import sind reines Java ohne Mixin-Bezug, nur kompiliert + code-geprüft. Alles Interaktive
braucht wie immer einen Ingame-Test durch den Nutzer, insbesondere: Design-2-Kategorie-Browsing,
Server-Editor-Sperren/Sortieren-Zusammenspiel, eingebettete Vorschau bei GUI-Größe 3/4.

Nach jeder Phase: kurzer Check-in mit dem Nutzer, bevor die nächste Phase startet.
