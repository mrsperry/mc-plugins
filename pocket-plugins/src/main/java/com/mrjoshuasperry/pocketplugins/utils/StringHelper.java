package com.mrjoshuasperry.pocketplugins.utils;

import java.util.List;

import com.google.common.collect.Lists;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class StringHelper {
    private static final List<NamedTextColor> rainbowColors = Lists.newArrayList(
            NamedTextColor.RED,
            NamedTextColor.GOLD,
            NamedTextColor.YELLOW,
            NamedTextColor.GREEN,
            NamedTextColor.BLUE,
            NamedTextColor.LIGHT_PURPLE);

    public static TextComponent rainbowify(String string) {
        TextComponent.Builder builder = Component.text();
        int index = 0;

        for (String part : string.split("")) {
            if (part.equals(" ")) {
                builder.append(Component.space());
            } else {
                builder.append(Component.text(part, rainbowColors.get(index)));
                index = (index + 1) % rainbowColors.size();
            }
        }

        return builder.build();
    }
}
