# CreativeMenu

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/C3W0229LCP)

[🇩🇪 Deutsche Version weiter unten](#deutsch)

## English

Makes the Creative inventory menu freely customizable — hide tabs, reorder them, build your own
custom tabs, and (if you run a server) enforce a shared layout for your players. Standalone
NeoForge mod, **no Cobblemon dependency**. Works in both singleplayer and multiplayer; the
server-side "prescriptions" feature is naturally multiplayer-only, everything else is just as
useful solo.

### What it does

- **Hide/show** any tab — original Minecraft/mod tabs and your own custom ones.
- **Reorder** every tab freely; newly installed mods get appended automatically.
- **Custom tabs** with three content sources: an explicit item list, a tag (dynamically resolved),
  or a merge of several whole existing tabs (each keeps its own internal item order).
- **Category tabs**: a tab that expands into a dropdown/flyout of its member tabs when clicked,
  instead of taking up a slot per member.
- **Server-side prescriptions** (admin-set tabs/categories, enforced per OP level) — lets a server
  owner standardize what every player's Creative menu looks like, per permission level, while
  still letting players do their own local customization on top where allowed.
- **Export/import**: your whole local config (or just one tab/category) via the system clipboard —
  handy for sharing a layout with someone else or backing it up.

### How to use it

- **Open your personal editor**: `/creativemenu open` (or the shorter alias `/crm open`) — a
  drag-and-drop list of tabs/categories, live preview, hide/show toggles, and buttons to create or
  edit custom tabs/categories.
- **Reset your local settings**: `/creativemenu resett true` (yes, that's the actual command —
  note the double "t").
- **Admin access** (needed for `/creativemenu server` below), independent from vanilla OP status:
  - `/creativemenu admin <name>` grants admin.
  - **First admin on a fresh server**: run it once from the **server console** — the console can
    always grant, regardless of OP status.
- **Server-wide prescriptions** *(admin only)*: `/creativemenu server` opens the admin editor
  (separate tab/category lists, each entry can be pinned per OP level with allow-hide/allow-sort
  flags); `/creativemenu server resett true` resets the server-wide config.
- **Permission thresholds** *(admin only)*: `/creativemenu permissions get` shows the current
  minimum OP level required for players to add/remove items, show/hide tabs, and sort tabs
  themselves; `/creativemenu permissions set <addremove|showhide|sort> <0-4>` changes one of them.

### Building

No third-party mod dependencies — this mod is fully standalone, nothing needed in `libs/`.

### Other CobbleCompanion-family projects

CreativeMenu isn't part of the CobbleCompanion family (no Cobblemon dependency, works in any
NeoForge modpack), but it's made by the same author:

- [CobbleCompanion](https://github.com/siralusian/CobbleCompanion) and its extensions/bundles
  ([CobbleDollars](https://github.com/siralusian/CobbleCompanion-CobbleDollars),
  [CobbleDollars/Create](https://github.com/siralusian/CobbleCompanion-CobbleDollars-Create),
  [CobbleDollars/CustomNPCs](https://github.com/siralusian/CobbleCompanion-CobbleDollars-CustomNPCs),
  [CobblemonWorker](https://github.com/siralusian/CobbleCompanion-CobblemonWorker),
  [AllInOne](https://github.com/siralusian/CobbleCompanion-AllInOne),
  [CobbleDollars-Bundle](https://github.com/siralusian/CobbleCompanion-CobbleDollarsBundle))
- [CopycatSign](https://github.com/siralusian/CopycatSign) — a Copycat block that displays a
  custom picture.
- [Create: Let's Do Automation](https://github.com/siralusian/CreateLetsDo) — automates Let's Do:
  Farm & Charm blocks with Create.

---

## Deutsch

Macht das Creative-Inventar-Menü frei gestaltbar – Tabs ausblenden, umsortieren, eigene
Custom-Tabs bauen, und (falls du einen Server betreibst) ein einheitliches Layout für deine
Spieler vorgeben. Eigenständige NeoForge-Mod, **keine Cobblemon-Abhängigkeit**. Funktioniert im
Singleplayer und auf Servern; die Server-"Vorschriften"-Funktion ist naturgemäß nur für Server
relevant, alles andere ist auch solo genauso nützlich.

### Was es macht

- **Ein-/Ausblenden** beliebiger Tabs – original Minecraft-/Mod-Tabs und eigene Custom-Tabs.
- **Frei sortieren**: jeder Tab beliebig verschiebbar; neu installierte Mods werden automatisch
  ans Ende angehängt.
- **Custom-Tabs** mit drei Inhaltsquellen: explizite Item-Liste, ein Tag (dynamisch aufgelöst),
  oder eine Zusammenführung mehrerer ganzer bestehender Tabs (jeder behält seine eigene interne
  Reihenfolge).
- **Kategorie-Tabs**: ein Tab, der beim Anklicken zu einer Dropdown-/Ausklapp-Liste seiner
  Mitglieder wird, statt einen Slot pro Mitglied zu belegen.
- **Server-Vorschriften** (Admin-gesetzte Tabs/Kategorien, durchgesetzt nach OP-Stufe) – ein
  Server-Betreiber kann festlegen, wie das Creative-Menü aller Spieler aussieht, gestaffelt nach
  Berechtigungsstufe, während Spieler dort, wo erlaubt, trotzdem noch selbst weiter anpassen
  können.
- **Export/Import**: die komplette lokale Konfiguration (oder nur ein einzelner Tab/eine
  Kategorie) über die System-Zwischenablage – praktisch zum Teilen eines Layouts oder als Backup.

### Benutzung

- **Eigenen Editor öffnen**: `/creativemenu open` (oder der kürzere Alias `/crm open`) – eine
  Drag&Drop-Liste aus Tabs/Kategorien, Live-Vorschau, Ein-/Ausblenden-Umschalter, sowie Buttons
  zum Erstellen/Bearbeiten von Custom-Tabs/-Kategorien.
- **Eigene Einstellungen zurücksetzen**: `/creativemenu resett true` (ja, das ist der tatsächliche
  Befehl – mit doppeltem „t").
- **Admin-Zugang** (nötig für `/creativemenu server` unten), unabhängig vom Vanilla-OP-Status:
  - `/creativemenu admin <Name>` vergibt Admin-Rechte.
  - **Allererster Admin auf einem frischen Server**: einmalig über die **Server-Konsole**
    ausführen – die Konsole darf immer berechtigen, unabhängig vom OP-Status.
- **Server-Vorschriften** *(nur Admin)*: `/creativemenu server` öffnet den Admin-Editor (getrennte
  Tab-/Kategorie-Listen, jeder Eintrag pro OP-Stufe fixierbar mit Ein-/Ausblenden-/
  Sortieren-Erlauben-Flags); `/creativemenu server resett true` setzt die Server-Konfiguration
  zurück.
- **Berechtigungsschwellen** *(nur Admin)*: `/creativemenu permissions get` zeigt die aktuell
  nötige Mindest-OP-Stufe, damit Spieler selbst Hinzufügen/Entfernen, Ein-/Ausblenden und
  Sortieren dürfen; `/creativemenu permissions set <addremove|showhide|sort> <0-4>` ändert eine
  davon.

### Bauen

Keine Fremd-Mod-Abhängigkeiten – diese Mod ist komplett eigenständig, in `libs/` wird nichts
benötigt.

### Weitere Projekte aus der CobbleCompanion-Familie

CreativeMenu gehört nicht zur CobbleCompanion-Familie (keine Cobblemon-Abhängigkeit, funktioniert
in jedem NeoForge-Modpack), stammt aber vom selben Autor:

- [CobbleCompanion](https://github.com/siralusian/CobbleCompanion) und seine
  Erweiterungen/Bundles
  ([CobbleDollars](https://github.com/siralusian/CobbleCompanion-CobbleDollars),
  [CobbleDollars/Create](https://github.com/siralusian/CobbleCompanion-CobbleDollars-Create),
  [CobbleDollars/CustomNPCs](https://github.com/siralusian/CobbleCompanion-CobbleDollars-CustomNPCs),
  [CobblemonWorker](https://github.com/siralusian/CobbleCompanion-CobblemonWorker),
  [AllInOne](https://github.com/siralusian/CobbleCompanion-AllInOne),
  [CobbleDollars-Bundle](https://github.com/siralusian/CobbleCompanion-CobbleDollarsBundle))
- [CopycatSign](https://github.com/siralusian/CopycatSign) — ein Copycat-Block, der ein frei
  wählbares Bild anzeigt.
- [Create: Let's Do Automation](https://github.com/siralusian/CreateLetsDo) — automatisiert Let's
  Do: Farm & Charm-Blöcke mit Create.
