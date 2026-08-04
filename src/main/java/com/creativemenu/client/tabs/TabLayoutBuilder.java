package com.creativemenu.client.tabs;

import com.creativemenu.client.mixin.vanilla.CreativeModeTabAccessor;
import com.creativemenu.data.ServerCategoryPrescription;
import com.creativemenu.data.ServerCustomTabPrescription;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Baut aus der echten (Vanilla/NeoForge-)Tab-Reihenfolge + der lokalen Konfiguration die finale
 * Liste, die {@code CreativeModeTabRegistryMixin} anstelle des Originals zurückgibt. Wird bei jedem
 * Öffnen/Resize des Kreativmenüs neu aufgerufen (siehe Mixin-Klassenkommentar) - bewusst ohne Cache,
 * da die Tab-Anzahl klein genug ist, dass Neuberechnung keine spürbaren Kosten hat, Korrektheit nach
 * Config-Änderungen aber garantiert bleibt.
 *
 * Server-Vorschriften ({@link ServerPrescriptionCache#filteredCustomTabs()}/{@link
 * ServerPrescriptionCache#filteredCategories()}, bereits nach OP-Level gefiltert) landen im selben
 * synthById-Topf wie lokale Custom-Tabs/Kategorien und nutzen dieselbe order/hiddenIds-Maschinerie
 * ("servertab:"/"servercategory:"-Slot-IDs) - so bestimmt der Server nur die ZUSAMMENSETZUNG, die
 * Position in der Reihenfolge bleibt (wie bei neu hinzugekommenen echten Tabs) Sache des Spielers.
 */
public class TabLayoutBuilder {

    public static List<CreativeModeTab> build(List<CreativeModeTab> vanillaSorted, LocalTabLayout layout) {
        CategoryRegistry.reset();
        TabIdentityRegistry.reset();

        Map<String, CreativeModeTab> realById = new LinkedHashMap<>();
        for (CreativeModeTab tab : vanillaSorted) {
            ResourceLocation name = CreativeModeTabRegistry.getName(tab);
            if (name != null) realById.put(name.toString(), tab);
        }

        Set<String> hidden = new HashSet<>(layout.hiddenIds);

        // Server-Sperren (Nutzer-Vorgabe, Phase 3-Nachbesserung): allowHide=false erzwingt
        // Sichtbarkeit unabhängig von der lokalen Konfiguration, allowSort=false erzwingt eine vom
        // Admin vorgegebene Position (siehe Platzierungs-Schleifen unten) statt der lokalen
        // Reihenfolge des Spielers.
        Set<String> lockedSort = new LinkedHashSet<>();
        for (ServerCustomTabPrescription def : ServerPrescriptionCache.filteredCustomTabs()) {
            if (!def.allowHide) hidden.remove(def.slotId());
            if (!def.allowSort) lockedSort.add(def.slotId());
        }
        for (ServerCategoryPrescription def : ServerPrescriptionCache.filteredCategories()) {
            if (!def.allowHide) hidden.remove(def.slotId());
            if (!def.allowSort) lockedSort.add(def.slotId());
        }

        // Custom-Tabs UND Kategorien werden immer gebaut, unabhängig von "hidden" - Ausblenden darf
        // nur steuern, ob ein Slot im Haupt-Tab-Balken auftaucht (siehe Platzierungs-Schleife unten),
        // nicht ob er als Objekt existiert. Nutzer-Fund: wurde ein hier übersprungener Tab von einer
        // Kategorie referenziert, verschwand er auch aus deren Mitgliederliste - Ausblenden UND
        // Kategorie-Zuordnung sind aber laut Vorgabe unabhängige Eigenschaften.
        Map<String, CreativeModeTab> synthById = new LinkedHashMap<>();
        for (CustomTabDefinition def : layout.customTabs) {
            CreativeModeTab tab = buildCustomTab(def, realById);
            if (tab != null) synthById.put(def.slotId(), tab);
        }
        for (ServerCustomTabPrescription def : ServerPrescriptionCache.filteredCustomTabs()) {
            CreativeModeTab tab = buildServerCustomTab(def, realById);
            if (tab != null) synthById.put(def.slotId(), tab);
        }
        for (CategoryDefinition def : layout.categories) {
            CreativeModeTab tab = buildCategoryTab(def, realById, synthById);
            if (tab != null) synthById.put(def.slotId(), tab);
        }
        for (ServerCategoryPrescription def : ServerPrescriptionCache.filteredCategories()) {
            // Server-Kategorien referenzieren laut Vorgabe nur echte Tabs (der Server kennt die
            // lokalen Custom-Tabs/Kategorien des Spielers nicht), daher hier nur realById.
            CreativeModeTab tab = buildServerCategoryTab(def, realById);
            if (tab != null) synthById.put(def.slotId(), tab);
        }

        for (Map.Entry<String, CreativeModeTab> entry : realById.entrySet()) {
            TabIdentityRegistry.register(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, CreativeModeTab> entry : synthById.entrySet()) {
            TabIdentityRegistry.register(entry.getKey(), entry.getValue());
        }

        List<CreativeModeTab> result = new ArrayList<>();
        Set<String> placed = new HashSet<>();

        // Gesperrte Slots ZUERST platzieren, in der vom Admin vorgegebenen Reihenfolge (siehe
        // ServerOrderScreen/orderByLevel) - ihre Position ist nicht verhandelbar, die lokale
        // Reihenfolge des Spielers wird für sie schlicht ignoriert.
        for (String slot : ServerPrescriptionCache.filteredOrder()) {
            if (!lockedSort.contains(slot) || placed.contains(slot)) continue;
            CreativeModeTab tab = synthById.get(slot);
            if (tab != null) {
                result.add(tab);
                placed.add(slot);
            }
        }

        for (String slot : layout.order) {
            if (hidden.contains(slot) || placed.contains(slot)) continue;
            CreativeModeTab tab = isSyntheticSlot(slot) ? synthById.get(slot) : realById.get(slot);
            if (tab != null) {
                result.add(tab);
                placed.add(slot);
            }
        }
        // Neu hinzugekommene (nicht in "order" gelistete) reale Tabs ans Ende anhängen, z.B. nach
        // Installation eines neuen Mods - ohne diesen Fallback würden sie sonst spurlos verschwinden.
        for (Map.Entry<String, CreativeModeTab> entry : realById.entrySet()) {
            if (!placed.contains(entry.getKey()) && !hidden.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        for (Map.Entry<String, CreativeModeTab> entry : synthById.entrySet()) {
            if (!placed.contains(entry.getKey()) && !hidden.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private static boolean isSyntheticSlot(String slot) {
        return slot.startsWith("custom:") || slot.startsWith("category:")
            || slot.startsWith("servertab:") || slot.startsWith("servercategory:");
    }

    private static CreativeModeTab buildCustomTab(CustomTabDefinition def, Map<String, CreativeModeTab> realById) {
        List<ItemStack> items = resolveSourceItems(def.itemIds, def.sourceTabIds, def.tagGroups, realById);
        if (items.isEmpty()) return null;

        String displayName = (def.name == null || def.name.isBlank()) ? "Custom Tab" : def.name;
        ItemStack iconStack = resolveIconOrFallback(def.iconItemId, items.get(0));
        return buildSyntheticTab(displayName, iconStack, items);
    }

    private static CreativeModeTab buildServerCustomTab(ServerCustomTabPrescription def, Map<String, CreativeModeTab> realById) {
        List<ItemStack> items = resolveSourceItems(def.itemIds, def.sourceTabIds, def.tagGroups, realById);
        if (items.isEmpty()) return null;

        String displayName = (def.name == null || def.name.isBlank()) ? "Server Tab" : def.name;
        ItemStack iconStack = resolveIconOrFallback(def.iconItemId, items.get(0));
        return buildSyntheticTab(displayName, iconStack, items);
    }

    // Nutzer-Vorgabe: explizite Items, ganze Tabs UND Tags sollen gleichzeitig zu einem Tab
    // beitragen können statt sich gegenseitig auszuschließen - vereinigt daher immer alle drei
    // Quellen (dedupliziert nach Item-Identität), statt exklusiv nach einem "aktiven" Typ zu
    // schalten. Welches Panel im Editor gerade sichtbar ist, bleibt reine UI-Angelegenheit.
    private static List<ItemStack> resolveSourceItems(List<String> itemIds, List<String> sourceTabIds,
            List<List<String>> tagGroups, Map<String, CreativeModeTab> realById) {
        List<ItemStack> items = new ArrayList<>();
        Set<Item> seen = new HashSet<>();

        for (String id : itemIds) {
            Item item = resolveItem(id);
            if (item != null && seen.add(item)) items.add(new ItemStack(item));
        }
        for (String sourceId : sourceTabIds) {
            CreativeModeTab source = realById.get(sourceId);
            if (source == null) continue;
            for (ItemStack stack : source.getDisplayItems()) {
                if (seen.add(stack.getItem())) items.add(stack);
            }
        }
        List<List<TagKey<Item>>> groups = new ArrayList<>();
        for (List<String> group : tagGroups) {
            List<TagKey<Item>> keys = new ArrayList<>();
            for (String tagId : group) {
                TagKey<Item> key = parseTagKey(tagId);
                if (key != null) keys.add(key);
            }
            if (!keys.isEmpty()) groups.add(keys);
        }
        if (!groups.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR || seen.contains(item)) continue;
                boolean matchesAnyGroup = false;
                for (List<TagKey<Item>> group : groups) {
                    boolean matchesAllInGroup = true;
                    for (TagKey<Item> key : group) {
                        if (!item.builtInRegistryHolder().is(key)) {
                            matchesAllInGroup = false;
                            break;
                        }
                    }
                    if (matchesAllInGroup) {
                        matchesAnyGroup = true;
                        break;
                    }
                }
                if (matchesAnyGroup) {
                    seen.add(item);
                    items.add(new ItemStack(item));
                }
            }
        }
        return items;
    }

    private static CreativeModeTab buildCategoryTab(CategoryDefinition def, Map<String, CreativeModeTab> realById,
            Map<String, CreativeModeTab> synthById) {
        List<CreativeModeTab> members = new ArrayList<>();
        List<ItemStack> unionItems = new ArrayList<>();
        for (String slot : def.memberSlotIds) {
            CreativeModeTab member = isSyntheticSlot(slot) ? synthById.get(slot) : realById.get(slot);
            if (member != null) {
                members.add(member);
                unionItems.addAll(member.getDisplayItems());
            }
        }
        if (members.isEmpty()) return null;

        String displayName = (def.name == null || def.name.isBlank()) ? "Category" : def.name;
        ItemStack fallbackIcon = unionItems.isEmpty() ? new ItemStack(Items.CHEST) : unionItems.get(0);
        ItemStack iconStack = resolveIconOrFallback(def.iconItemId, fallbackIcon);
        // Fällt hasAnyItems()/shouldDisplay() nicht durch, auch wenn alle Mitglieder (theoretisch)
        // leer wären - siehe CreativeModeTab.shouldDisplay(): type==CATEGORY braucht hasAnyItems().
        List<ItemStack> content = unionItems.isEmpty() ? List.of(iconStack) : unionItems;
        CreativeModeTab tab = buildSyntheticTab(displayName, iconStack, content);
        CategoryRegistry.register(tab, members);
        return tab;
    }

    private static CreativeModeTab buildServerCategoryTab(ServerCategoryPrescription def, Map<String, CreativeModeTab> realById) {
        List<CreativeModeTab> members = new ArrayList<>();
        List<ItemStack> unionItems = new ArrayList<>();
        for (String slot : def.memberSlotIds) {
            CreativeModeTab member = realById.get(slot);
            if (member != null) {
                members.add(member);
                unionItems.addAll(member.getDisplayItems());
            }
        }
        if (members.isEmpty()) return null;

        String displayName = (def.name == null || def.name.isBlank()) ? "Server Category" : def.name;
        ItemStack fallbackIcon = unionItems.isEmpty() ? new ItemStack(Items.CHEST) : unionItems.get(0);
        ItemStack iconStack = resolveIconOrFallback(def.iconItemId, fallbackIcon);
        List<ItemStack> content = unionItems.isEmpty() ? List.of(iconStack) : unionItems;
        CreativeModeTab tab = buildSyntheticTab(displayName, iconStack, content);
        CategoryRegistry.register(tab, members);
        return tab;
    }

    private static CreativeModeTab buildSyntheticTab(String displayName, ItemStack iconStack, List<ItemStack> items) {
        CreativeModeTab tab = CreativeModeTab.builder()
            .title(Component.literal(displayName))
            .icon(() -> iconStack)
            .displayItems((params, output) -> items.forEach(output::accept))
            .build();

        ((CreativeModeTabAccessor) tab).creativemenu$setDisplayItems(new ArrayList<>(items));
        ((CreativeModeTabAccessor) tab).creativemenu$setDisplayItemsSearchTab(new LinkedHashSet<>(items));
        return tab;
    }

    private static ItemStack resolveIconOrFallback(String iconItemId, ItemStack fallback) {
        Item iconItem = resolveItem(iconItemId);
        return iconItem != null ? new ItemStack(iconItem) : fallback.copy();
    }

    private static TagKey<Item> parseTagKey(String tagId) {
        if (tagId == null || tagId.isBlank()) return null;
        String path = tagId.startsWith("#") ? tagId.substring(1) : tagId;
        ResourceLocation loc = ResourceLocation.tryParse(path);
        return loc == null ? null : TagKey.create(Registries.ITEM, loc);
    }

    private static Item resolveItem(String id) {
        if (id == null || id.isBlank()) return null;
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
        return item == Items.AIR ? null : item;
    }
}
