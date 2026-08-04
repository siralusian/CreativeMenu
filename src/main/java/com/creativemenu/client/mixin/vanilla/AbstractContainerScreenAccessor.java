package com.creativemenu.client.mixin.vanilla;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * leftPos/topPos sind in AbstractContainerScreen deklariert, nicht direkt in
 * CreativeModeInventoryScreen - ein @Shadow im Mixin auf CreativeModeInventoryScreen selbst
 * scheitert dafür mit "was not located in the target class" (per Crash-Report + Testlauf via
 * runClient verifiziert; auch das Nachbilden der Superklassen-Kette in der Mixin-Klasse behebt das
 * NICHT). Der korrekte Weg: ein eigenes Accessor-Mixin direkt auf die deklarierende Klasse, dessen
 * Interface sich dann per Cast von jeder Unterklasseninstanz aus verwenden lässt (siehe
 * CreativeModeInventoryScreenMixin).
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int creativemenu$getLeftPos();

    @Accessor("topPos")
    int creativemenu$getTopPos();

    @Accessor("imageWidth")
    int creativemenu$getImageWidth();

    @Accessor("imageHeight")
    int creativemenu$getImageHeight();
}
