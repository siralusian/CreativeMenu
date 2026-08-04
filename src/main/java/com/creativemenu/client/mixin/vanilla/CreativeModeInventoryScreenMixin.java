package com.creativemenu.client.mixin.vanilla;

import com.creativemenu.client.gui.SelectedSlotTracker;
import com.creativemenu.client.gui.SelectedTabSnapshot;
import com.creativemenu.client.tabs.CategoryBrowseState;
import com.creativemenu.client.tabs.CategoryRegistry;
import com.creativemenu.client.tabs.ClientTabConfigManager;
import com.creativemenu.client.tabs.TabIdentityRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Kategorie-Tabs (siehe {@link CategoryRegistry}): belegen einen normalen Tab-Slot. Nutzer-Vorgabe
 * (Feedback-Runde nach Live-Test): statt sich auf ein Design festzulegen, gibt es jetzt drei zur
 * Wahl (siehe {@code LocalTabLayout#categoryDisplayMode}, im Editor-Startbildschirm einstellbar):
 *
 * <ul>
 *   <li>0 - "Overlay" (erste Version): öffnet beim Anklicken ein Seitenpanel mit Mitglieder-Kacheln
 *   links neben dem Kreativmenü, siehe {@link #creativemenu$renderCategoryOverlay} und die
 *   dazugehörigen mouseClicked/mouseReleased-Zweige weiter unten.</li>
 *   <li>1 - "Item-Auswahl": Klick auf die Kategorie zeigt ihre Mitglieder als Pseudo-Items im Grid
 *   ("Picker"-Ansicht, siehe {@link #creativemenu$enterItemPickerMode}/
 *   {@link #creativemenu$applyPickerItems}) - Klick auf ein Mitglieds-Pseudo-Item (per
 *   {@code slotClicked}-Injection {@link #creativemenu$onSlotClicked} per Objektidentität erkannt
 *   und abgefangen, bevor Vanillas Aufheben-Logik greift) zeigt dessen echte Items. KEIN eigenes
 *   Zurück-Pseudo-Item (Nutzer-Vorgabe) - ein erneuter Klick auf die bereits aktive Kategorie
 *   schaltet stattdessen zwischen Mitglieds- und Picker-Ansicht um (siehe
 *   {@link #creativemenu$handleItemPickerClick}, HEAD-Injection auf {@code mouseReleased}: MUSS vor
 *   Vanillas eigenem, im selben Klick noch folgendem {@code selectTab}-Aufruf laufen, sonst zeigt
 *   das Grid einen Klick lang den alten Stand - Live-Test-Fund). Die Kategorie bleibt durchgängig
 *   {@code selectedTab} (wie Design 0/2), {@code currentMemberIndex == -1} markiert die
 *   Picker-Ansicht (siehe {@link CategoryBrowseState}).</li>
 *   <li>2 - "Slot-Übernahme" (Default, siehe {@link CategoryBrowseState}): {@link #currentPage} wird
 *   durch eine SYNTHETISCHE {@link CreativeTabsScreenPage} ersetzt (Kategorie + bis zu 8 Mitgliedern
 *   + einem eigenen, frisch gebauten Zurück-Tab) - Vanilla rendert/positioniert/klick-testet diese
 *   dann komplett selbst, kein eigener Render-/Klick-Eingriff dafür nötig (Zurück auf DIESEN Ansatz
 *   nach einem Zwischenstopp mit Overlay-basiertem Rendering, der laut Nutzer-Feedback optisch
 *   schlechter aussah als diese Version). Die Kategorie bleibt durchgängig Vanillas echter
 *   {@code selectedTab}, nur ihr Item-Inhalt wird beim Mitglied-Wechsel ausgetauscht (über
 *   {@link CreativeModeTabAccessor}). WICHTIG (Live-Test-Fund, behoben): nach dem Austauschen der
 *   Items MUSS {@code selectTab(category)} erneut aufgerufen werden - Vanilla baut die sichtbare
 *   Item-Grid-Liste offenbar nur BEIM AUSWÄHLEN eines Tabs neu auf, nicht automatisch bei jeder
 *   Änderung der zugrundeliegenden {@code displayItems}.
 *   Der Zurück-Button: NICHT der echte Operator-Werkzeuge-Tab (Versuch verworfen - der ist
 *   zusätzlich einer von NeoForges eigenen "Default-Tabs" und wurde dadurch trotz Überzeichnung
 *   immer noch mit seinem originalen Icon gerendert), sondern ein KOMPLETT EIGENER, synthetischer
 *   Tab (structure_void-Icon, siehe {@link #creativemenu$buildBackButtonTab}) - dessen Icon
 *   rendert Vanilla direkt korrekt, ohne dass wir irgendetwas überzeichnen müssen.
 *   Der Tab-Name-Text unter der Leiste (Vanillas {@code renderLabels}, zeigt immer nur
 *   {@code selectedTab.getDisplayName()} = den Kategorie-Namen) wird um "- [Mitglieds-Name]"
 *   ergänzt, siehe {@link #creativemenu$renderActiveMemberLabel} - WICHTIG: braucht einen
 *   leftPos/topPos-Offset, da an dieser Stelle (TAIL von render()) Vanillas eigene
 *   pushPose/translate-Klammer um diese Koordinaten schon wieder aufgelöst ist (Live-Test-Fund:
 *   ohne Offset landete der Text in der Fensterecke statt neben dem Kategorie-Namen).
 *   Mehr als 8 Mitglieder werden auf mehrere Seiten verteilt (siehe
 *   {@link #creativemenu$rebuildBrowsePage}) - Kategorie UND Zurück-Tab werden dafür auf JEDER
 *   Seite erneut eingebaut (gleiche Instanzen), damit der Zurück-Button nicht auf eine erst per
 *   Blättern erreichbare Seite "wandert" (Nutzer-Vorgabe). {@code this.pages} ist final - beim
 *   Betreten/Verlassen wird nur der Inhalt getauscht, die echte Liste vorher in
 *   {@link CategoryBrowseState#setOriginalPages} gesichert. Zum Blättern echte Vanilla-Buttons
 *   (siehe {@link #creativemenu$addBrowsePageButtonsIfNeeded}) statt eigenem Klickbereich (eine
 *   erste Version mit eigenem Text-Klickbereich sprang laut Live-Test bei einem Klick direkt 2
 *   Seiten weiter). Erneutes Klicken der bereits aktiven Kategorie ist ein No-Op (Live-Test-Fund:
 *   rief sonst {@code enterBrowseMode} erneut auf und überschrieb die gesicherten
 *   Original-Items/-Seiten mit dem bereits ausgetauschten Zwischenstand, wodurch der Zurück-Button
 *   danach ins Leere lief).</li>
 * </ul>
 *
 * leftPos/topPos sind in AbstractContainerScreen deklariert (Oberklasse), nicht direkt in
 * CreativeModeInventoryScreen - ein @Shadow scheitert dafür mit "was not located in the target
 * class" (per Testlauf via runClient verifiziert), deshalb der Umweg über
 * {@link AbstractContainerScreenAccessor} statt direktem @Shadow.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    // Exakte Kopie der privaten Vanilla-Sprite-Konstanten (siehe CreativeModeInventoryScreen) -
    // damit Kategorie-Mitglieder-Kacheln (Design 0) die GLEICHE Tab-Textur bekommen wie ein
    // normaler, nicht-selektierter Tab-Button.
    private static final ResourceLocation[] CREATIVEMENU_TILE_SPRITES = {
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_unselected_7")
    };

    // Exakte Kopie von Vanillas SELECTED_TOP_TABS/SELECTED_BOTTOM_TABS (siehe
    // CreativeModeInventoryScreen.renderTabButton) - für die zusätzliche "aktives Mitglied"-
    // Hervorhebung in Design 2 (siehe creativemenu$renderActiveMemberHighlight).
    private static final ResourceLocation[] SELECTED_TOP_TABS = {
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_selected_7")
    };
    private static final ResourceLocation[] SELECTED_BOTTOM_TABS = {
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_1"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_2"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_3"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_4"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_5"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_6"),
        ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_bottom_selected_7")
    };
    private static final int MEMBERS_PER_COLUMN = 4;
    private static final int TILE_W = 26;
    private static final int TILE_H = 32;
    // Unser eigener Zurück-Tab belegt (wie jeder synthetische Tab) wieder einen Slot pro Seite
    // (letzter Slot) - Mitglieder füllen die restlichen bis zu 8 Slots EINER Seite. Bei mehr
    // Mitgliedern als das auf eine Seite passt, baut creativemenu$rebuildBrowsePage() mehrere
    // Seiten, jede davon mit Kategorie (Slot 1) + eigenem Zurück-Tab (letzter Slot) - so bleibt der
    // Zurück-Button auf JEDER Seite an derselben Stelle sichtbar/klickbar (Nutzer-Vorgabe: er darf
    // nicht auf eine andere Seite "wandern").
    private static final int BROWSE_MAX_MEMBERS_PER_PAGE = 8;

    @Shadow
    private CreativeTabsScreenPage currentPage;

    @Shadow
    private List<CreativeTabsScreenPage> pages;

    @Shadow
    private boolean checkTabClicked(CreativeModeTab tab, double x, double y) {
        throw new AssertionError();
    }

    @Shadow
    private void selectTab(CreativeModeTab tab) {
        throw new AssertionError();
    }

    @Shadow
    private int getTabX(CreativeModeTab tab) {
        throw new AssertionError();
    }

    // Referenzen auf die von uns pro Browse-Sitzung hinzugefügten echten Vanilla-Buttons (siehe
    // creativemenu$addBrowsePageButtonsIfNeeded/creativemenu$removeBrowsePageButtons) - unsere
    // erste Version mit eigenem Text-Klickbereich statt echter Button-Widgets führte laut
    // Nutzer-Test dazu, dass ein Klick gleich 2 Seiten weiterblätterte; echte Vanilla-Buttons
    // (dieselbe Klasse/Logik, die Vanilla für reale Mehrseiten-Setups selbst nutzt) sind
    // battle-tested und haben dieses Problem nicht.
    private Button creativemenu$prevPageButton;
    private Button creativemenu$nextPageButton;

    // Design 1 (Item-Auswahl, siehe unten): die EXAKTEN ItemStack-Instanzen, die aktuell als
    // Mitglieds-Kacheln in der Picker-Ansicht im Item-Grid der Kategorie stehen -
    // creativemenu$onSlotClicked erkennt einen Treffer per Objektidentität (Vanilla kopiert die
    // Stacks beim Anzeigen nicht, siehe ItemPickerMenu#items/CreativeModeInventoryScreen.CONTAINER).
    private List<ItemStack> creativemenu$pickerItemStacks;

    /**
     * Merkt sich die Slot-ID jeder echten Auswahl (Vanilla-Klick, Kategorie-Mitglieds-Klick - beide
     * laufen letztlich über diese Methode) - siehe {@link SelectedSlotTracker}/{@link
     * TabIdentityRegistry} und {@code CreativeModeTabRegistryMixin} für die Wiederherstellung.
     * Räumt außerdem {@link CategoryBrowseState} auf, sobald zu einem GANZ ANDEREN Tab gewechselt
     * wird (Nutzer-Fund: die "Kategorie - Mitglied"-Beschriftung blieb sonst über dem neuen Tab
     * hängen). Design 1 (Item-Auswahl) startet NICHT mehr von hier aus - siehe
     * {@link #creativemenu$handleItemPickerClick} (muss VOR Vanillas eigenem {@code selectTab}-
     * Aufruf laufen, ein TAIL-Hook wäre für diesen Zweck "einen Frame zu spät").
     * <p>
     * WICHTIG (Live-Test-Fund, behoben): der "anderer Tab"-Vergleich lief ursprünglich per
     * Objektidentität ({@code tab != CategoryBrowseState.category()}) - das schlug beim
     * Schließen/Wiederöffnen fälschlich zu, weil Vanillas EIGENER {@code init()}-Code mitten im
     * Wiederaufbau selbst schon {@code this.selectTab(frischeKategorie)} aufruft (siehe
     * {@code CreativeModeInventoryScreen.init()}: {@code selectedTab = ...; this.selectTab(...)}),
     * NOCH BEVOR {@link #creativemenu$restoreBrowseModeOnReopen} (TAIL von {@code init()}, läuft
     * erst am ALLERENDE) die Objektreferenz in {@link CategoryBrowseState} auf die frische Instanz
     * aktualisieren konnte - {@code CreativeModeTab} hat kein {@code equals()}, die frische Instanz
     * ist also zwangsläufig "!= " die alte, obwohl es dieselbe Kategorie ist. Das löschte den
     * Browse-Zustand (inkl. {@code currentMemberIndex}), BEVOR er wiederhergestellt werden konnte -
     * man landete nach dem Wiederöffnen im ersten statt im zuletzt aktiven Mitglied. Fix: Vergleich
     * über die stabile Slot-ID ({@link CategoryBrowseState#categorySlotId()}) statt Objektidentität.
     */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void creativemenu$onSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        String slotId = TabIdentityRegistry.slotIdOf(tab);
        if (slotId != null) SelectedSlotTracker.record(slotId);

        if (CategoryBrowseState.isActive() && !java.util.Objects.equals(slotId, CategoryBrowseState.categorySlotId())) {
            creativemenu$clearOverlayMemberSelection();
        }
    }

    /**
     * Nutzer-Fund: "im Tab bleiben beim Schließen" (siehe {@link SelectedSlotTracker}/
     * {@code CreativeModeTabRegistryMixin}) restauriert nur, DASS die Kategorie wieder
     * {@code selectedTab} ist - nicht, dass man sich noch in ihrer Browse-Unteransicht befand.
     * {@link CategoryBrowseState#isActive()} bleibt zwar über ein Schließen hinweg true (nichts
     * ruft {@code exit()} beim reinen Schließen auf), aber {@code category()}/{@code members()}
     * zeigen nach dem Rebuild noch auf die ALTEN, jetzt verworfenen Instanzen (synthetische Tabs
     * werden bei jedem {@code TabLayoutBuilder.build()}-Aufruf neu instanziiert). Deshalb hier am
     * Ende von {@code init()} (nachdem Vanilla {@code this.pages}/{@code selectedTab} fertig
     * aufgebaut/abgeglichen hat) mit den FRISCHEN Instanzen erneut in den Browse-Modus wechseln -
     * unter Beibehaltung des zuletzt aktiven Mitglieds-Index (siehe
     * {@link CategoryBrowseState#refreshInstances}), statt wie ein echter Neueinstieg auf das
     * erste Mitglied zurückzufallen. Gilt genauso für Design 0 (Seitenpanel, siehe
     * {@link #creativemenu$selectOverlayMember}) und Design 1 (Item-Auswahl, siehe
     * {@link #creativemenu$enterItemPickerMode}) - beide aber OHNE die Seiten-/Zurück-Tab-
     * Rekonstruktion, die nur Design 2 (Slot-Übernahme) braucht.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void creativemenu$restoreBrowseModeOnReopen(CallbackInfo ci) {
        int mode = ClientTabConfigManager.get().categoryDisplayMode;
        if (mode != 0 && mode != 1 && mode != 2) return;
        if (!CategoryBrowseState.isActive()) return;

        CreativeModeTab freshCategory = SelectedTabSnapshot.get();
        if (freshCategory == null || !CategoryRegistry.isCategory(freshCategory)) return;
        List<CreativeModeTab> freshMembers = CategoryRegistry.membersOf(freshCategory);
        if (freshMembers == null || freshMembers.isEmpty()) return;

        List<ItemStack> unionItems = new ArrayList<>(freshCategory.getDisplayItems());
        CategoryBrowseState.refreshInstances(freshCategory, freshMembers, unionItems);
        int idx = CategoryBrowseState.currentMemberIndex();

        if (mode == 1) {
            if (idx < 0) {
                creativemenu$applyPickerItems(freshCategory, freshMembers);
            } else {
                creativemenu$applyMemberContent(freshMembers.get(idx));
            }
            this.selectTab(freshCategory);
            return;
        }

        // Nutzer-Fund (Crash): -1 ist nur in Design 1 ein gültiger Sentinel ("Picker-Ansicht, kein
        // Mitglied gewählt") - blieb er nach einem Wechsel auf Design 0/2 (z.B. per Design-Umschalter
        // im Editor, ohne dass CategoryBrowseState dabei zurückgesetzt wird) bestehen, crashte
        // freshMembers.get(-1) hier mit IndexOutOfBoundsException. Design 0/2 kennen diesen Sentinel
        // nicht - auf 0 klemmen.
        if (idx < 0) idx = 0;
        CreativeModeTab member = freshMembers.get(idx);
        creativemenu$applyMemberContent(member);
        this.selectTab(freshCategory);

        if (mode != 2) return;

        CategoryBrowseState.setOriginalPages(new ArrayList<>(this.pages));
        creativemenu$rebuildBrowsePage();

        for (CreativeTabsScreenPage page : this.pages) {
            if (page.getVisibleTabs().contains(member)) {
                this.currentPage = page;
                break;
            }
        }
    }

    private int creativemenu$leftPos() {
        return ((AbstractContainerScreenAccessor) this).creativemenu$getLeftPos();
    }

    private int creativemenu$topPos() {
        return ((AbstractContainerScreenAccessor) this).creativemenu$getTopPos();
    }

    // --- Design 0: Seitenpanel-Overlay ---

    /**
     * Position der Kachel für Mitglied Nr. {@code memberIndex} (0-basiert) - spaltenweise von oben
     * nach unten (bis {@link #MEMBERS_PER_COLUMN}), weitere Spalten links von leftPos, mit
     * Original-Tab-Abständen (26x32, ohne Lücke, wie im echten Tab-Balken).
     */
    private static int[] creativemenu$tileRect(int leftPos, int topPos, int memberIndex) {
        int col = memberIndex / MEMBERS_PER_COLUMN;
        int row = memberIndex % MEMBERS_PER_COLUMN;
        int bx = leftPos - (col + 1) * TILE_W;
        int by = topPos + row * TILE_H;
        return new int[] {bx, by};
    }

    /** Gesamt-Umriss des Overlays (für Klick-Blockade) - deckt alle Spalten/Reihen ab. */
    private static int[] creativemenu$overlayBounds(int leftPos, int topPos, int memberCount) {
        int cols = (memberCount + MEMBERS_PER_COLUMN - 1) / MEMBERS_PER_COLUMN;
        int rows = Math.min(MEMBERS_PER_COLUMN, memberCount);
        int w = cols * TILE_W;
        int h = rows * TILE_H;
        int x = leftPos - w;
        return new int[] {x, topPos, w, h};
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void creativemenu$renderCategoryOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        int mode = ClientTabConfigManager.get().categoryDisplayMode;
        if (mode == 2) {
            creativemenu$renderActiveMemberHighlight(guiGraphics);
            creativemenu$renderActiveMemberLabel(guiGraphics);
            return;
        }
        if (mode == 1) {
            // Design 1 (Item-Auswahl): braucht kein eigenes Overlay-Rendering - die Picker-/
            // Mitglieds-Items sind ganz normale Items im Grid, Vanilla zeichnet sie selbst.
            // creativemenu$renderActiveMemberLabel no-opt bereits von selbst bei currentMemberIndex
            // == -1 (Picker-Ansicht, siehe dortige Guard-Bedingung).
            creativemenu$renderActiveMemberLabel(guiGraphics);
            return;
        }
        if (mode != 0) return;

        // Nutzer-Vorgabe: dieselbe "Kategorie - Mitglied"-Beschriftung wie bei Design 2 (Slot-
        // Übernahme) - creativemenu$renderActiveMemberLabel ist generisch (liest nur CategoryBrowseState),
        // funktioniert hier unverändert, da creativemenu$selectOverlayMember denselben Zustand befüllt.
        creativemenu$renderActiveMemberLabel(guiGraphics);

        int leftPos = creativemenu$leftPos();
        int topPos = creativemenu$topPos();

        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (!CategoryRegistry.isCategory(tab) || !CategoryRegistry.isExpanded(tab)) continue;
            List<CreativeModeTab> members = CategoryRegistry.membersOf(tab);
            if (members == null) continue;

            for (int i = 0; i < members.size(); i++) {
                CreativeModeTab member = members.get(i);
                int[] rect = creativemenu$tileRect(leftPos, topPos, i);
                int bx = rect[0];
                int by = rect[1];

                guiGraphics.blitSprite(CREATIVEMENU_TILE_SPRITES[i % CREATIVEMENU_TILE_SPRITES.length], bx, by, TILE_W, TILE_H);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
                int iconX = bx + 5;
                int iconY = by + 8;
                ItemStack iconStack = member.getIconItem();
                guiGraphics.renderItem(iconStack, iconX, iconY);
                guiGraphics.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, iconStack, iconX, iconY);
                guiGraphics.pose().popPose();

                if (mouseX >= bx && mouseX < bx + TILE_W && mouseY >= by && mouseY < by + TILE_H) {
                    guiGraphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, member.getDisplayName(), mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Nutzer-Fund: eine aufgeklappte Kategorie in Spalte 0 kollidierte mit dem Seiten-Wechseln-Button
     * (der optisch/positionell darunter/dahinter liegt) - Button-Presses feuern in mouseCLICKED, nicht
     * erst in mouseReleased, weshalb ein Klick auf eine Mitglieder-Kachel zusätzlich (unbeabsichtigt)
     * die Seite umschalten konnte, bevor unser eigenes mouseReleased überhaupt lief. Deshalb hier
     * JEDEN Klick innerhalb des Overlay-Bereichs abfangen, bevor Vanilla ihn an seine Buttons
     * weiterreichen kann - unabhängig davon, welcher Button zufällig darunter/dahinter liegt.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void creativemenu$onMouseClicked(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (ClientTabConfigManager.get().categoryDisplayMode != 0) return;
        if (button == 0 && creativemenu$isWithinAnyExpandedOverlay(mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    private boolean creativemenu$isWithinAnyExpandedOverlay(double mouseX, double mouseY) {
        int leftPos = creativemenu$leftPos();
        int topPos = creativemenu$topPos();
        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (!CategoryRegistry.isCategory(tab) || !CategoryRegistry.isExpanded(tab)) continue;
            List<CreativeModeTab> members = CategoryRegistry.membersOf(tab);
            if (members == null || members.isEmpty()) continue;

            int[] bounds = creativemenu$overlayBounds(leftPos, topPos, members.size());
            if (mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2] && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3]) {
                return true;
            }
        }
        return false;
    }

    /** Index 0 = getroffene Kategorie, Index 1 = getroffenes Mitglied - null wenn kein Treffer. */
    private CreativeModeTab[] creativemenu$findOverlayHit(double mouseX, double mouseY) {
        int leftPos = creativemenu$leftPos();
        int topPos = creativemenu$topPos();
        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (!CategoryRegistry.isCategory(tab) || !CategoryRegistry.isExpanded(tab)) continue;
            List<CreativeModeTab> members = CategoryRegistry.membersOf(tab);
            if (members == null) continue;

            for (int i = 0; i < members.size(); i++) {
                int[] rect = creativemenu$tileRect(leftPos, topPos, i);
                if (mouseX >= rect[0] && mouseX < rect[0] + TILE_W && mouseY >= rect[1] && mouseY < rect[1] + TILE_H) {
                    return new CreativeModeTab[] {tab, members.get(i)};
                }
            }
        }
        return null;
    }

    /**
     * Nutzer-Fund: ein Klick auf eine Mitglieds-Kachel im Seitenpanel wählte bisher das Mitglied
     * DIREKT als {@code selectedTab} aus ({@code this.selectTab(overlayHit)}). Ist dieses Mitglied
     * kein selbst im Haupt-Tab-Balken sichtbarer Tab (z.B. ausgeblendet, nur über die Kategorie
     * erreichbar), findet Vanillas eigener Auswahl-Abgleich beim Schließen/Wiederöffnen
     * ({@code init()}: {@code this.pages.stream().filter(page -> page.getVisibleTabs().contains(selectedTab))})
     * ihn auf KEINER Seite - man landete danach wieder im ersten normalen Tab. Fix (gleiches Prinzip
     * wie Design 2/Slot-Übernahme): die KATEGORIE bleibt {@code selectedTab} (die IST immer Teil
     * einer echten Seite), nur ihr Item-Inhalt wird auf das gewählte Mitglied umgeschaltet - dafür
     * wird {@link CategoryBrowseState} auch hier genutzt (liefert gleich das Rückgedächtnis für
     * {@link #creativemenu$restoreBrowseModeOnReopen} und die "Kategorie - Mitglied"-Beschriftung),
     * aber OHNE {@link #creativemenu$rebuildBrowsePage} - Design 0 braucht keine synthetische
     * Seiten-/Zurück-Tab-Konstruktion, das Seitenpanel bleibt ja weiterhin sichtbar/bedienbar.
     */
    private void creativemenu$selectOverlayMember(CreativeModeTab category, CreativeModeTab member) {
        List<CreativeModeTab> members = CategoryRegistry.membersOf(category);
        if (members == null) return;
        int idx = members.indexOf(member);

        List<ItemStack> originalItems = CategoryBrowseState.isActive() && CategoryBrowseState.category() == category
            ? CategoryBrowseState.originalItems()
            : new ArrayList<>(category.getDisplayItems());
        CategoryBrowseState.enter(category, members, originalItems);
        if (idx >= 0) CategoryBrowseState.setCurrentMemberIndex(idx);
        creativemenu$applyMemberContent(member);
        this.selectTab(category);
    }

    /**
     * Nutzer-Fund: nach Auswahl eines Mitglieds über {@link #creativemenu$selectOverlayMember}
     * blieb {@link CategoryBrowseState} aktiv, auch wenn man danach einen GANZ ANDEREN, nicht zur
     * Kategorie gehörenden Tab anklickte - die "Kategorie - Mitglied"-Beschriftung ({@link
     * #creativemenu$renderActiveMemberLabel} prüft nur {@code isActive()}, nicht ob die Kategorie
     * noch der tatsächlich angezeigte Tab ist) blieb dadurch fälschlich über dem neuen Tab stehen.
     * Anders als {@link #creativemenu$exitBrowseMode} (Design 2) fasst diese Variante
     * {@code this.pages}/{@code this.currentPage} NICHT an - Design 0 hat die Seitenstruktur nie
     * verändert, ein Seitenwechsel wäre hier ein unerwünschter Nebeneffekt.
     */
    private void creativemenu$clearOverlayMemberSelection() {
        if (!CategoryBrowseState.isActive()) return;
        CreativeModeTab category = CategoryBrowseState.category();
        List<ItemStack> original = CategoryBrowseState.originalItems();
        if (category != null && original != null) {
            ((CreativeModeTabAccessor) category).creativemenu$setDisplayItems(new ArrayList<>(original));
            ((CreativeModeTabAccessor) category).creativemenu$setDisplayItemsSearchTab(new LinkedHashSet<>(original));
        }
        CategoryBrowseState.exit();
        creativemenu$pickerItemStacks = null;
    }

    // --- Design 1 (Item-Auswahl): Mitglieder als Pseudo-Items im Grid ---

    /**
     * Läuft VOR Vanillas eigenem {@code mouseReleased}-Body (siehe Aufrufstelle, HEAD-Injection
     * ohne Cancel). Live-Test-Fund: ein erster Klick auf die Kategorie zeigte fälschlich schon den
     * ersten Untertab an - Ursache war, den Item-Tausch aus einem TAIL-Hook von {@code selectTab}
     * auszulösen, ALSO NACHDEM Vanillas eigener {@code selectTab}-Aufruf die Item-Grid-Liste schon
     * (mit den alten Items) aufgebaut hatte; erst ein zweiter Klick zeigte dann die Picker-Ansicht.
     * Hier läuft der Item-Tausch dagegen VOR Vanillas eigenem, im selben Klick noch folgenden
     * {@code selectTab}-Aufruf - der liest dann sofort die schon aktualisierten Items.
     * Nutzer-Vorgabe: kein eigenes Zurück-Pseudo-Item - ein erneuter Klick auf die (bereits
     * ausgewählte) Kategorie schaltet stattdessen zwischen Mitglieds- und Picker-Ansicht um.
     */
    private void creativemenu$handleItemPickerClick(double mouseX, double mouseY) {
        double d0 = mouseX - (double) creativemenu$leftPos();
        double d1 = mouseY - (double) creativemenu$topPos();
        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (!this.checkTabClicked(tab, d0, d1)) continue;
            if (CategoryRegistry.isCategory(tab)) {
                boolean alreadyShowingMember = CategoryBrowseState.isActive()
                    && CategoryBrowseState.category() == tab
                    && CategoryBrowseState.currentMemberIndex() >= 0;
                if (alreadyShowingMember) {
                    creativemenu$backToItemPicker(tab);
                } else {
                    creativemenu$enterItemPickerMode(tab);
                }
            }
            return;
        }
    }

    /**
     * Baut die "Picker"-Ansicht auf: die Item-Slots zeigen ein Pseudo-Item pro Mitglied (dessen
     * Icon, umbenannt auf den Mitglieds-Namen) statt echter Items. {@code currentMemberIndex == -1}
     * markiert diesen Zustand (siehe {@link CategoryBrowseState}). Reiner Zustands-/Item-Tausch,
     * OHNE eigenen {@code selectTab}-Aufruf - der Aufrufer entscheidet, ob/wie neu ausgewählt wird
     * (siehe {@link #creativemenu$handleItemPickerClick}, das Vanillas eigenen, gleich folgenden
     * {@code selectTab}-Aufruf dafür nutzt).
     */
    private void creativemenu$enterItemPickerMode(CreativeModeTab category) {
        if (CategoryBrowseState.isActive() && CategoryBrowseState.category() == category) return;
        List<CreativeModeTab> members = CategoryRegistry.membersOf(category);
        if (members == null || members.isEmpty()) return;

        List<ItemStack> originalItems = new ArrayList<>(category.getDisplayItems());
        CategoryBrowseState.enter(category, members, originalItems);
        CategoryBrowseState.setCurrentMemberIndex(-1);
        creativemenu$applyPickerItems(category, members);
    }

    private void creativemenu$backToItemPicker(CreativeModeTab category) {
        CategoryBrowseState.setCurrentMemberIndex(-1);
        creativemenu$applyPickerItems(category, CategoryBrowseState.members());
    }

    /** Baut die Picker-Pseudo-Items (ein Icon pro Mitglied, umbenannt) und setzt sie als Item-Inhalt der Kategorie. */
    private void creativemenu$applyPickerItems(CreativeModeTab category, List<CreativeModeTab> members) {
        List<ItemStack> items = new ArrayList<>();
        for (CreativeModeTab member : members) {
            ItemStack stack = new ItemStack(member.getIconItem().getItem());
            stack.set(DataComponents.CUSTOM_NAME, member.getDisplayName());
            items.add(stack);
        }
        creativemenu$pickerItemStacks = items;
        ((CreativeModeTabAccessor) category).creativemenu$setDisplayItems(new ArrayList<>(items));
        ((CreativeModeTabAccessor) category).creativemenu$setDisplayItemsSearchTab(new LinkedHashSet<>(items));
    }

    private void creativemenu$selectPickerMember(int index) {
        CategoryBrowseState.setCurrentMemberIndex(index);
        CreativeModeTab member = CategoryBrowseState.members().get(index);
        creativemenu$applyMemberContent(member);
        // Gleiches Live-Test-Fund-Muster wie Design 0/2: Vanilla baut die sichtbare Item-Grid-Liste
        // nur BEIM AUSWÄHLEN eines Tabs neu auf, nicht automatisch bei jeder Änderung von dessen
        // displayItems - dieser Klick kommt aus slotClicked (kein anschließender eigener
        // selectTab-Aufruf von Vanilla wie bei einem Tab-Klick), daher hier selbst nötig.
        this.selectTab(CategoryBrowseState.category());
    }

    /**
     * Fängt Klicks auf unsere Picker-Pseudo-Items ab, BEVOR Vanillas eigene Aufheben-/Cursor-Logik
     * greift (sonst würde man sich das Pseudo-Item einfach ins Inventar legen). Erkennung per
     * Objektidentität, nicht per {@code ItemStack}-Inhalt: Vanilla kopiert die Stacks beim Anzeigen
     * nicht (bestätigt per Quellcode-Analyse: {@code ItemPickerMenu.items} übernimmt
     * {@code selectedTab.getDisplayItems()} per {@code addAll}, {@code SimpleContainer} kopiert
     * beim {@code setItem} ebenfalls nicht) - der exakt selbe Stack, den wir in
     * {@link #creativemenu$applyPickerItems} erzeugt haben, kommt hier unverändert im Slot an.
     * In der Mitglieds-Ansicht ({@code currentMemberIndex >= 0}) sind es ganz normale echte Items -
     * dort gibt es nichts abzufangen (Zurück läuft über {@link #creativemenu$handleItemPickerClick}).
     */
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void creativemenu$onSlotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (ClientTabConfigManager.get().categoryDisplayMode != 1) return;
        if (!CategoryBrowseState.isActive() || slot == null) return;
        if (CategoryBrowseState.currentMemberIndex() >= 0) return;

        ItemStack clicked = slot.getItem();
        if (clicked.isEmpty()) return;
        List<ItemStack> pickerStacks = creativemenu$pickerItemStacks;
        if (pickerStacks == null) return;
        for (int i = 0; i < pickerStacks.size(); i++) {
            if (pickerStacks.get(i) == clicked) {
                creativemenu$selectPickerMember(i);
                ci.cancel();
                return;
            }
        }
    }

    // --- Design 2 (Slot-Übernahme, Default) ---

    /**
     * Nutzer-Vorgabe: die Kategorie soll als aktiver Tab sichtbar bleiben, ZUSÄTZLICH soll aber
     * auch der aktuell gezeigte Mitglieds-Tab als aktiv markiert werden - Vanillas eigene
     * Hervorhebung folgt aber strikt {@code selectedTab} (bleibt bewusst die Kategorie, siehe
     * Klassenkommentar), zeigt also von sich aus immer nur EINEN hervorgehobenen Tab. Deshalb hier
     * zusätzlich das Mitglied selbst mit exakt demselben Sprite/Icon-Aufbau wie Vanillas eigenes
     * {@code renderTabButton} (nur mit dem "ausgewählt"-statt "nicht ausgewählt"-Sprite) neu
     * gezeichnet - überschreibt Vanillas eigene (nicht-ausgewählte) Zeichnung von vorhin.
     */
    private void creativemenu$renderActiveMemberHighlight(GuiGraphics guiGraphics) {
        if (!CategoryBrowseState.isActive()) return;
        List<CreativeModeTab> members = CategoryBrowseState.members();
        int idx = CategoryBrowseState.currentMemberIndex();
        if (idx < 0 || idx >= members.size()) return;
        CreativeModeTab member = members.get(idx);
        if (!this.currentPage.getVisibleTabs().contains(member)) return;

        boolean top = this.currentPage.isTop(member);
        int column = this.currentPage.getColumn(member);
        int imageHeight = ((AbstractContainerScreenAccessor) this).creativemenu$getImageHeight();
        int leftPos = creativemenu$leftPos();
        int topPos = creativemenu$topPos();

        int j = leftPos + this.getTabX(member);
        int k = topPos - (top ? 28 : -(imageHeight - 4));
        ResourceLocation[] sprites = top ? SELECTED_TOP_TABS : SELECTED_BOTTOM_TABS;
        guiGraphics.blitSprite(sprites[net.minecraft.util.Mth.clamp(column, 0, sprites.length - 1)], j, k, 26, 32);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        int iconX = j + 5;
        int iconY = k + 8 + (top ? 1 : -1);
        ItemStack iconStack = member.getIconItem();
        guiGraphics.renderItem(iconStack, iconX, iconY);
        guiGraphics.renderItemDecorations(net.minecraft.client.Minecraft.getInstance().font, iconStack, iconX, iconY);
        guiGraphics.pose().popPose();
    }

    /**
     * Nutzer-Fund: der Text unter der Tab-Leiste (Vanillas {@code renderLabels}, zeichnet
     * {@code selectedTab.getDisplayName()}) zeigt durchgängig nur den Kategorie-Namen, da
     * {@code selectedTab} bewusst die Kategorie bleibt - das verrät nicht, welches Mitglied gerade
     * aktiv ist. Hier wird deshalb zusätzlich " - [Mitglieds-Name]" direkt hinter Vanillas eigenem
     * Text angehängt (Breite über font.width() ermittelt, damit nichts überlappt).
     */
    private void creativemenu$renderActiveMemberLabel(GuiGraphics guiGraphics) {
        if (!CategoryBrowseState.isActive()) return;
        List<CreativeModeTab> members = CategoryBrowseState.members();
        int idx = CategoryBrowseState.currentMemberIndex();
        if (idx < 0 || idx >= members.size()) return;
        CreativeModeTab category = CategoryBrowseState.category();
        CreativeModeTab member = members.get(idx);

        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        int categoryNameWidth = font.width(category.getDisplayName());
        String suffix = " - " + member.getDisplayName().getString();
        // Live-Test-Fund: an dieser Stelle (TAIL von render()) hat Vanilla seine pushPose/translate
        // um (leftPos, topPos) laengst wieder per popPose() rueckgaengig gemacht (siehe
        // CreativeModeInventoryScreen.render(): direkt nach super.render() rechnet Vanilla selbst
        // schon wieder in absoluten leftPos/topPos-Koordinaten fuer die Seitenanzeige) - Vanillas
        // eigener renderLabels()-Aufruf zeichnet "8, 6" dagegen INNERHALB dieser Translation. Ohne
        // den Offset hier landete unser Suffix in der Fensterecke statt neben dem Kategorie-Namen.
        int x = creativemenu$leftPos() + 8 + categoryNameWidth;
        int y = creativemenu$topPos() + 6;
        guiGraphics.drawString(font, suffix, x, y, category.getLabelColor(), false);
    }

    /**
     * Nutzer-Vorgabe: bei mehr als 8 Mitgliedern reicht eine einzelne Seite nicht mehr - der
     * Zurück-Button darf dabei aber nicht auf eine Seite "wandern", die man erst per Blättern
     * erreicht. Lösung: JEDE Browse-Seite bekommt ihren eigenen Zurück-Button an derselben Stelle
     * (siehe {@link #creativemenu$rebuildBrowsePage}). Fürs Blättern selbst NICHT (mehr) unser
     * eigener Text-Klickbereich (Live-Test-Fund: sprang bei einem Klick direkt 2 Seiten weiter),
     * sondern echte Vanilla-{@link Button}-Widgets - exakt dieselbe Klasse/Position wie Vanillas
     * eigene "&lt;"/"&gt;"-Buttons für reale Mehrseiten-Setups (siehe {@code init()}), nur zur
     * Laufzeit per {@code addRenderableWidget} hinzugefügt statt beim Screen-Start.
     */
    private void creativemenu$addBrowsePageButtonsIfNeeded() {
        if (this.pages.size() <= 1) return;
        int leftPos = creativemenu$leftPos();
        int topPos = creativemenu$topPos();
        int imageWidth = ((AbstractContainerScreenAccessor) this).creativemenu$getImageWidth();
        ScreenInvoker self = (ScreenInvoker) this;
        creativemenu$prevPageButton = self.creativemenu$invokeAddRenderableWidget(Button.builder(Component.literal("<"),
                b -> this.currentPage = this.pages.get(Math.max(this.pages.indexOf(this.currentPage) - 1, 0)))
            .pos(leftPos, topPos - 50).size(20, 20).build());
        creativemenu$nextPageButton = self.creativemenu$invokeAddRenderableWidget(Button.builder(Component.literal(">"),
                b -> this.currentPage = this.pages.get(Math.min(this.pages.indexOf(this.currentPage) + 1, this.pages.size() - 1)))
            .pos(leftPos + imageWidth - 20, topPos - 50).size(20, 20).build());
    }

    /** Muss beim Verlassen des Browse-Modus laufen, sonst blieben die Buttons als Karteileichen im Screen. */
    private void creativemenu$removeBrowsePageButtons() {
        ScreenInvoker self = (ScreenInvoker) this;
        if (creativemenu$prevPageButton != null) {
            self.creativemenu$invokeRemoveWidget(creativemenu$prevPageButton);
            creativemenu$prevPageButton = null;
        }
        if (creativemenu$nextPageButton != null) {
            self.creativemenu$invokeRemoveWidget(creativemenu$nextPageButton);
            creativemenu$nextPageButton = null;
        }
    }

    private boolean creativemenu$handleBrowseModeClick(double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        double d0 = mouseX - (double) creativemenu$leftPos();
        double d1 = mouseY - (double) creativemenu$topPos();

        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (!this.checkTabClicked(tab, d0, d1)) continue;

            if (CategoryBrowseState.isActive() && tab == CategoryBrowseState.backButtonTab()) {
                creativemenu$exitBrowseMode();
                cir.setReturnValue(true);
                return true;
            }
            if (CategoryBrowseState.isActive() && CategoryBrowseState.members().contains(tab)) {
                creativemenu$switchBrowseMember(CategoryBrowseState.members().indexOf(tab));
                cir.setReturnValue(true);
                return true;
            }
            // Nutzer-Fund: erneutes Klicken der BEREITS aktiven Kategorie fiel sonst durch bis zur
            // generischen isCategory-Prüfung darunter und rief creativemenu$enterBrowseMode ERNEUT
            // auf - dabei wurden originalItems/originalPages mit dem bereits ausgetauschten
            // Zwischenstand überschrieben (categoryTab.getDisplayItems() zeigte zu dem Zeitpunkt
            // schon die Mitglieds-Items, this.pages schon die synthetischen Browse-Seiten). Danach
            // brachte der Zurück-Button nicht mehr zur echten Seite zurück, und der Zurück-Tab war
            // als gewöhnlicher Tab mit dem Structure-Void-Item aufrufbar. Klick auf die bereits
            // aktive Kategorie ist daher jetzt ein No-Op.
            if (CategoryBrowseState.isActive() && tab == CategoryBrowseState.category()) {
                cir.setReturnValue(true);
                return true;
            }
            if (CategoryRegistry.isCategory(tab)) {
                creativemenu$enterBrowseMode(tab);
                cir.setReturnValue(true);
                return true;
            }
            // Nutzer-Fund: die "fremden" echten Tabs Operator-Werkzeuge/Inventar/Schnellzugriffsleiste/
            // Suche sind NeoForges eigene "Default-Tabs" (siehe CreativeModeTabRegistry) und tauchen
            // deshalb auch auf UNSERER synthetischen Browse-Seite auf. Ein Klick darauf fiel bisher
            // ungecancelt durch zu Vanillas eigenem mouseReleased (das den Tab korrekt wechselt), OHNE
            // dass wir den Browse-Modus sauber verließen - CategoryBrowseState blieb aktiv, this.pages
            // die synthetischen Browse-Seiten, die Kategorie zeigte weiter die ausgetauschten
            // Mitglieds-Items, während unser eigenes Rendering (renderActiveMemberHighlight/-Label)
            // parallel weiterlief. Deshalb hier VOR dem Durchfallen sauber aufräumen; der Klick selbst
            // bleibt ungecancelt, damit Vanillas eigene Logik den tatsächlichen Tab-Wechsel übernimmt.
            if (CategoryBrowseState.isActive()) {
                creativemenu$exitBrowseMode();
            }
            return false;
        }
        return false;
    }

    private void creativemenu$enterBrowseMode(CreativeModeTab categoryTab) {
        List<CreativeModeTab> members = CategoryRegistry.membersOf(categoryTab);
        if (members == null || members.isEmpty()) return;

        List<ItemStack> originalItems = new ArrayList<>(categoryTab.getDisplayItems());
        CategoryBrowseState.enter(categoryTab, members, originalItems);
        CategoryBrowseState.setOriginalPages(new ArrayList<>(this.pages));
        creativemenu$rebuildBrowsePage();
        creativemenu$applyMemberContent(members.get(0));
        this.selectTab(categoryTab);
    }

    private void creativemenu$switchBrowseMember(int index) {
        CategoryBrowseState.setCurrentMemberIndex(index);
        creativemenu$applyMemberContent(CategoryBrowseState.members().get(index));
        // Live-Test-Fund: OHNE diesen erneuten Aufruf blieb die Item-Grid-Anzeige auf dem Stand des
        // vorherigen Mitglieds stehen - Vanilla baut seine sichtbare Item-Liste offenbar nur beim
        // AUSWÄHLEN eines Tabs neu auf, nicht automatisch bei jeder Änderung von dessen
        // displayItems. category ist bereits selectedTab, dieser Aufruf ändert also NICHT, welcher
        // Tab als aktiv markiert ist - er zwingt Vanilla nur, seine Anzeige neu aufzubauen.
        this.selectTab(CategoryBrowseState.category());
    }

    private void creativemenu$applyMemberContent(CreativeModeTab member) {
        List<ItemStack> items = new ArrayList<>(member.getDisplayItems());
        CreativeModeTab category = CategoryBrowseState.category();
        ((CreativeModeTabAccessor) category).creativemenu$setDisplayItems(items);
        ((CreativeModeTabAccessor) category).creativemenu$setDisplayItemsSearchTab(new LinkedHashSet<>(items));
    }

    /**
     * Nutzer-Vorgabe: statt den echten Operator-Werkzeuge-Tab umzufunktionieren (der sich - vermutlich
     * weil er zusätzlich einer von NeoForges eigenen "Default-Tabs" ist, siehe
     * {@code CreativeModeTabRegistry} - hartnäckig gegen Icon-Überzeichnung erwies, das originale
     * Command-Block-Icon blieb sichtbar), bauen wir jetzt einen KOMPLETT EIGENEN, frischen
     * Zurück-Tab (wie ein Custom-Tab in TabLayoutBuilder) - Vanilla rendert dessen Icon dann direkt
     * korrekt, ohne dass wir irgendetwas überzeichnen müssen. Landet als letzter Eintrag unserer
     * Liste (Slot 10) - Position ist unproblematisch, da dieser Tab (anders als der echte
     * Operator-Tab) NICHT gleichzeitig über einen zweiten Mechanismus (Default-Tabs) positioniert wird.
     */
    private void creativemenu$rebuildBrowsePage() {
        CreativeModeTab category = CategoryBrowseState.category();
        List<CreativeModeTab> members = CategoryBrowseState.members();
        CreativeModeTab backButtonTab = creativemenu$buildBackButtonTab();
        CategoryBrowseState.setBackButtonTab(backButtonTab);

        // Mehrere Seiten bei >8 Mitgliedern - Kategorie UND Zurück-Tab werden auf JEDER Seite erneut
        // mit eingebaut (gleiche Instanzen), damit beide unabhängig davon, welche Mitglieder-Seite
        // gerade aktiv ist, immer an derselben Stelle (Slot 1 bzw. letzter Slot) sichtbar/klickbar sind.
        int total = members.size();
        int pageCount = Math.max(1, (total + BROWSE_MAX_MEMBERS_PER_PAGE - 1) / BROWSE_MAX_MEMBERS_PER_PAGE);
        List<CreativeTabsScreenPage> browsePages = new ArrayList<>();
        for (int p = 0; p < pageCount; p++) {
            List<CreativeModeTab> synthetic = new ArrayList<>();
            synthetic.add(category);
            int from = p * BROWSE_MAX_MEMBERS_PER_PAGE;
            int to = Math.min(from + BROWSE_MAX_MEMBERS_PER_PAGE, total);
            for (int i = from; i < to; i++) synthetic.add(members.get(i));
            synthetic.add(backButtonTab);
            browsePages.add(new CreativeTabsScreenPage(synthetic));
        }

        // this.pages ist final (siehe Vanilla-Quelle) - Inhalt austauschen statt Referenz ersetzen,
        // damit Vanillas eigene "X / Y"-Seitenanzeige in render() (liest this.pages.size() direkt,
        // ungated) automatisch mitläuft.
        this.pages.clear();
        this.pages.addAll(browsePages);
        this.currentPage = this.pages.get(0);

        // Defensiv: falls hier bereits Buttons aus einer vorherigen Browse-Sitzung hängen
        // (z.B. direkter Kategorie-zu-Kategorie-Wechsel ohne Zurück-Klick dazwischen).
        creativemenu$removeBrowsePageButtons();
        creativemenu$addBrowsePageButtonsIfNeeded();
    }

    /**
     * Braucht MINDESTENS ein Item in displayItems, sonst filtert Vanillas eigenes
     * {@code shouldDisplay()} (type==CATEGORY braucht hasAnyItems()) den Tab komplett aus
     * getVisibleTabs() heraus - er wäre dann unsichtbar UND nicht anklickbar.
     */
    private static CreativeModeTab creativemenu$buildBackButtonTab() {
        ItemStack icon = new ItemStack(Items.STRUCTURE_VOID);
        List<ItemStack> items = List.of(icon);
        CreativeModeTab tab = CreativeModeTab.builder()
            .title(Component.translatable("gui.creativemenu.editor.back"))
            .icon(() -> icon)
            .displayItems((params, output) -> output.accept(icon))
            .build();
        ((CreativeModeTabAccessor) tab).creativemenu$setDisplayItems(new ArrayList<>(items));
        ((CreativeModeTabAccessor) tab).creativemenu$setDisplayItemsSearchTab(new LinkedHashSet<>(items));
        return tab;
    }

    private void creativemenu$exitBrowseMode() {
        CreativeModeTab category = CategoryBrowseState.category();
        List<ItemStack> original = CategoryBrowseState.originalItems();
        if (category != null && original != null) {
            ((CreativeModeTabAccessor) category).creativemenu$setDisplayItems(new ArrayList<>(original));
            ((CreativeModeTabAccessor) category).creativemenu$setDisplayItemsSearchTab(new LinkedHashSet<>(original));
        }
        List<CreativeTabsScreenPage> originalPages = CategoryBrowseState.originalPages();
        CategoryBrowseState.exit();
        creativemenu$removeBrowsePageButtons();

        // Echte Seitenliste zurückschreiben (wir haben this.pages beim Betreten mit den
        // Browse-Seiten überschrieben, siehe creativemenu$rebuildBrowsePage) - sonst wären reale
        // Mehrseiten-Setups (>10 Top-Level-Tabs) nach dem Verlassen kaputt.
        if (originalPages != null) {
            this.pages.clear();
            this.pages.addAll(originalPages);
        }

        for (CreativeTabsScreenPage page : this.pages) {
            if (category != null && page.getVisibleTabs().contains(category)) {
                this.currentPage = page;
                return;
            }
        }
        if (!this.pages.isEmpty()) this.currentPage = this.pages.get(0);
    }

    // --- Klick-Routing (verzweigt nach gewähltem Design) ---

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void creativemenu$onMouseReleased(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        int mode = ClientTabConfigManager.get().categoryDisplayMode;
        if (mode == 2) {
            creativemenu$handleBrowseModeClick(mouseX, mouseY, cir);
            return;
        }
        if (mode == 1) {
            creativemenu$handleItemPickerClick(mouseX, mouseY);
            // NICHT cancel: Vanillas eigener mouseReleased-Body soll den Tab weiterhin ganz normal
            // per selectTab(...) auswählen - das passiert NACH unserem HEAD-Code hier, liest also
            // bereits unsere frisch gesetzten displayItems (siehe creativemenu$handleItemPickerClick).
            return;
        }
        if (mode != 0) return;

        CreativeModeTab[] overlayHit = creativemenu$findOverlayHit(mouseX, mouseY);
        if (overlayHit != null) {
            CategoryRegistry.collapseAll();
            creativemenu$selectOverlayMember(overlayHit[0], overlayHit[1]);
            cir.setReturnValue(true);
            return;
        }

        double d0 = mouseX - (double) creativemenu$leftPos();
        double d1 = mouseY - (double) creativemenu$topPos();
        for (CreativeModeTab tab : this.currentPage.getVisibleTabs()) {
            if (this.checkTabClicked(tab, d0, d1)) {
                if (CategoryRegistry.isCategory(tab)) {
                    CategoryRegistry.toggle(tab);
                    cir.setReturnValue(true);
                } else {
                    CategoryRegistry.collapseAll();
                    creativemenu$clearOverlayMemberSelection();
                }
                return;
            }
        }
        CategoryRegistry.collapseAll();
    }
}
