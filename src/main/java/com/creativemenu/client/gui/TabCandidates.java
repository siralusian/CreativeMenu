package com.creativemenu.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/** Vorschlagsliste für das Tag-Textfeld im Custom-Tab-Editor (einziger verbliebener Autocomplete-Fall). */
public class TabCandidates {

    private static List<String> tags;

    public static List<String> tags() {
        if (tags == null) {
            tags = BuiltInRegistries.ITEM.getTagNames()
                .map(tag -> tag.location().toString())
                .sorted()
                .toList();
        }
        return tags;
    }
}
