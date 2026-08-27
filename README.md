# CreativeMenu

[🇩🇪 Deutsche Version weiter unten](#deutsch)

## English

Makes the Creative inventory menu freely customizable — hide tabs, reorder them, build your own
custom tabs, and (if you run a server) enforce a shared layout for your players. Works in both
singleplayer and multiplayer; the server-side "prescriptions" feature is naturally multiplayer-only,
everything else is just as useful solo.

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
- **Reset your local settings**: `/creativemenu resett true`
- **Admin access** (needed for `/creativemenu server` below), independent from vanilla OP status:
  - `/creativemenu admin <name>` grants admin.
  - **First admin on a fresh server**: run it once from the **server console**.
- **Server-wide prescriptions** *(admin only)*: `/creativemenu server` opens the admin editor
  (separate tab/category lists, each entry can be pinned per OP level with allow-hide/allow-sort
  flags); `/creativemenu server resett true` resets the server-wide config.
- **Permission thresholds** *(admin only)*: `/creativemenu permissions get` shows the current
  minimum OP level required for players to add/remove items, show/hide tabs, and sort tabs
  themselves; `/creativemenu permissions set <addremove|showhide|sort> <0-4>` changes one of them.

### Dependencies

No further mods are required to use CreativeMenu.

### Check out my other projects too

- [Area Claims](https://curseforge.com/minecraft/mc-mods/area-claims) — Lets players claim their
  own area on your server.
- [CobbleCompanion](https://curseforge.com/minecraft/mc-mods/cobblecompanion-all-in-one) —
  companion tool for the Cobblemon mod.
- [Create: Let's Do Automation](https://curseforge.com/minecraft/mc-mods/create-let-s-do) — lets
  you automatically fill Let's Do work blocks using Create.
- [CreativeMenu](https://curseforge.com/minecraft/mc-mods/creative-menu) — freely design your
  Creative menu the way you want. Fully configurable in-game.
- [CopycatSign](https://curseforge.com/minecraft/mc-mods/create-copycat-sign) — hang pictures on
  your walls, Create trains, airships and more, with freely choosable border and back textures.
- [Item Creator](https://curseforge.com/minecraft/mc-mods/itemcreator) — create items with
  enchantments and more, entirely without /give commands.
- [InvSpy](https://curseforge.com/minecraft/mc-mods/invspy) — powerful tool for server admins.
  Check which player used a chest, or what your players are carrying in their inventory.
- [MobTweaks](https://curseforge.com/minecraft/mc-mods/mobtweak) — tool for server admins. Control
  which mobs may spawn where, adjust loot, or prevent world damage from Creepers, Endermen and co.

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/C3W0229LCP)

---

## Deutsch

Macht das Creative-Inventar-Menü frei gestaltbar – Tabs ausblenden, umsortieren, eigene
Custom-Tabs bauen, und (falls du einen Server betreibst) ein einheitliches Layout für deine
Spieler vorgeben. Funktioniert im Singleplayer und auf Servern; die Server-"Vorschriften"-Funktion ist naturgemäß nur für Server
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
- **Eigene Einstellungen zurücksetzen**: `/creativemenu resett true`
- **Admin-Zugang** (nötig für `/creativemenu server` unten), unabhängig vom Vanilla-OP-Status:
  - `/creativemenu admin <Name>` vergibt Admin-Rechte.
  - **Allererster Admin auf einem frischen Server**: einmalig über die **Server-Konsole**
    ausführen.
- **Server-Vorschriften** *(nur Admin)*: `/creativemenu server` öffnet den Admin-Editor (getrennte
  Tab-/Kategorie-Listen, jeder Eintrag pro OP-Stufe fixierbar mit Ein-/Ausblenden-/
  Sortieren-Erlauben-Flags); `/creativemenu server resett true` setzt die Server-Konfiguration
  zurück.
- **Berechtigungsschwellen** *(nur Admin)*: `/creativemenu permissions get` zeigt die aktuell
  nötige Mindest-OP-Stufe, damit Spieler selbst Hinzufügen/Entfernen, Ein-/Ausblenden und
  Sortieren dürfen; `/creativemenu permissions set <addremove|showhide|sort> <0-4>` ändert eine
  davon.

### Abhängigkeiten

Es werden keine weiteren Mods benötigt, um CreativeMenu verwenden zu können.

### Sieh dir auch meine anderen Projekte an

- [Area Claims](https://curseforge.com/minecraft/mc-mods/area-claims) — Erlaube es Spielern ihren eigenen Bereich auf deinem Server zu beanspruchen.
- [CobbleCompanion](https://curseforge.com/minecraft/mc-mods/cobblecompanion-all-in-one) Hilfstool für die Cobblemon Mod
- [Create: Let's Do Automation](https://curseforge.com/minecraft/mc-mods/create-let-s-do) — Ermöglicht das automatische Befüllen von Let's Do Arbeitsblöcken mithilfe von Create.
- [CreativeMenu](https://curseforge.com/minecraft/mc-mods/creative-menu) — Gestalte dein Creative Menü frei nach deinen Wünschen. Alles ingame einstellbar.
- [CopycatSign](https://curseforge.com/minecraft/mc-mods/create-copycat-sign) — Hänge Bilder an deine Wände, Züge, Luftschiffe und Co mit frei wählbaren Rand- und Rückseiten-Texturen.
- [Item Creator](https://curseforge.com/minecraft/mc-mods/itemcreator) — Erzeuge Items mit Verzauberungen und Co ganz ohne /give Commands
- [InvSpy](https://curseforge.com/minecraft/mc-mods/invspy) — Starkes Tool für Server-Betreiber. Prüfe welcher Spieler sich an einer Truhe bedient hat oder was deine Spieler im Inventar haben.
- [MobTweaks](https://curseforge.com/minecraft/mc-mods/mobtweak) — Tool für Server-Betreiber. Steuere welche Mobs wo spawnen dürfen, passe den Loot an oder verhindere Schaden in der Welt durch Creeper, Enderman und co.

*AI-generated content: this mod was developed with AI assistance (Claude). / KI-generierte Inhalte: Diese Mod wurde mit KI-Unterstützung (Claude) entwickelt.*