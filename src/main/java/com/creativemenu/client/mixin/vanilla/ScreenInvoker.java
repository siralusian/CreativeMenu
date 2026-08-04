package com.creativemenu.client.mixin.vanilla;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * addRenderableWidget/removeWidget sind in {@code Screen} deklariert, nicht direkt in
 * {@code CreativeModeInventoryScreen} - ein @Shadow im dortigen Mixin scheitert deshalb mit
 * "was not located in the target class" (per Crash-Report bestätigt: Live-Test crashte beim
 * Hochfahren, exakt dieselbe Fehlerart wie bereits bei leftPos/topPos, siehe
 * {@link AbstractContainerScreenAccessor}). Der korrekte Weg für METHODEN (statt Feldern) einer
 * Oberklasse: ein eigenes @Invoker-Mixin direkt auf die deklarierende Klasse, dessen Interface sich
 * dann per Cast von jeder Unterklasseninstanz aus verwenden lässt (siehe
 * CreativeModeInventoryScreenMixin#creativemenu$addBrowsePageButtonsIfNeeded/removeBrowsePageButtons).
 */
@Mixin(Screen.class)
public interface ScreenInvoker {

    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T creativemenu$invokeAddRenderableWidget(T widget);

    @Invoker("removeWidget")
    void creativemenu$invokeRemoveWidget(GuiEventListener listener);
}
