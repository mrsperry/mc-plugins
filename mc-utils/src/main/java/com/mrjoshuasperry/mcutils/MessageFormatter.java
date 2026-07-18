package com.mrjoshuasperry.mcutils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MessageFormatter {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static Component addTimestamp() {
        return bracketed(Component.text(LocalTime.now().format(TIME_FORMAT)));
    }

    public static Component addWorldName(String name) {
        return bracketed(Component.text(name));
    }

    public static Component addTimeAndWorld(String name) {
        return addTimestamp().append(addWorldName(name));
    }

    private static Component bracketed(Component inner) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(inner.color(NamedTextColor.GRAY))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));
    }
}
