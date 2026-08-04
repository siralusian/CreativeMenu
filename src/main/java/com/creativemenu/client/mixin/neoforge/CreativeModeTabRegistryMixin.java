package com.creativemenu.client.mixin.neoforge;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Einziger Eingriffspunkt für Tab-Reihenfolge/Sichtbarkeit/Custom-Tabs: {@code getSortedCreativeModeTabs()}
 * ist die Quelle, aus der {@code CreativeModeInventoryScreen.init()} seine Seiten (10 Tabs/Seite,
 * 5 oben/5 unten) baut - Rendering UND Klick-Handling lesen beide aus genau dieser Seiten-Struktur
 * (siehe CreativeTabsScreenPage.getVisibleTabs()). Wer hier eingreift, muss weder das Rendering noch
 * das Klick-Handling separat anfassen (per Analyse der NeoForge/Vanilla-Quellen verifiziert, siehe
 * ROADMAP.md).
 */
@Mixin(CreativeModeTabRegistry.class)
public class CreativeModeTabRegistryMixin {

    @Inject(method = "getSortedCreativeModeTabs", at = @At("RETURN"), cancellable = true)
    private static void creativemenu$applyLocalLayout(CallbackInfoReturnable<List<CreativeModeTab>> cir) {
        List<CreativeModeTab> vanillaOrder = cir.getReturnValue();
        cir.setReturnValue(com.creativemenu.client.tabs.TabLayoutBuilder.build(
            vanillaOrder, com.creativemenu.client.tabs.ClientTabConfigManager.get()));

        // Nutzer-Fund: synthetische Tabs (Custom/Kategorie/Server) werden bei JEDEM Aufruf neu
        // instanziiert (CreativeModeTab hat kein equals/hashCode) - Vanillas eigener,
        // identitätsbasierter selectedTab-Abgleich in CreativeModeInventoryScreen.init() (läuft
        // NACH diesem Rückgabewert) würde die alte Objektinstanz nie wiederfinden und immer auf
        // den ersten Tab der Seite zurückfallen (reale Tabs sind davon nicht betroffen, deren
        // Objektinstanz bleibt über Rebuilds hinweg dieselbe - das erklärt die beobachtete
        // Asymmetrie). Hier den zuletzt gemerkten Slot (siehe SelectedSlotTracker, aktualisiert
        // bei jeder echten Auswahl über CreativeModeInventoryScreenMixin) auf die FRISCHE Instanz
        // auflösen und zurückschreiben, bevor Vanillas eigene Auflösung läuft.
        String lastSlotId = com.creativemenu.client.gui.SelectedSlotTracker.get();
        CreativeModeTab fresh = com.creativemenu.client.tabs.TabIdentityRegistry.bySlotId(lastSlotId);
        if (fresh != null) com.creativemenu.client.gui.SelectedTabSnapshot.set(fresh);
    }
}
