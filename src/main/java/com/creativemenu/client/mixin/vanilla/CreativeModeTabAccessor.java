package com.creativemenu.client.mixin.vanilla;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;
import java.util.Set;

/**
 * Erlaubt direktes Setzen der Item-Listen eines {@link CreativeModeTab}, ohne über
 * {@code buildContents()} zu gehen - das würde einen im Registry auflösbaren ResourceKey verlangen
 * (siehe CreativeModeTab.buildContents Zeile ~144), den unsere rein clientseitig erzeugten Custom-
 * Tabs (siehe {@code com.creativemenu.client.tabs.TabLayoutBuilder}) absichtlich nie haben, da sie
 * niemals in {@code BuiltInRegistries.CREATIVE_MODE_TAB} registriert werden.
 */
@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor {

    @Accessor("displayItems")
    void creativemenu$setDisplayItems(Collection<ItemStack> items);

    @Accessor("displayItemsSearchTab")
    void creativemenu$setDisplayItemsSearchTab(Set<ItemStack> items);
}
