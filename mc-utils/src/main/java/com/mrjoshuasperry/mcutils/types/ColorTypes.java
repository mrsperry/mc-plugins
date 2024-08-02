package com.mrjoshuasperry.mcutils.types;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;

public class ColorTypes {
    private static Map<String, Color> colors = Map.ofEntries(
            Map.entry("AQUA", Color.AQUA),
            Map.entry("BLACK", Color.BLACK),
            Map.entry("BLUE", Color.BLUE),
            Map.entry("FUCHSIA", Color.FUCHSIA),
            Map.entry("GRAY", Color.GRAY),
            Map.entry("GREEN", Color.GREEN),
            Map.entry("LIME", Color.LIME),
            Map.entry("MAROON", Color.MAROON),
            Map.entry("NAVY", Color.NAVY),
            Map.entry("OLIVE", Color.OLIVE),
            Map.entry("ORANGE", Color.ORANGE),
            Map.entry("PURPLE", Color.PURPLE),
            Map.entry("RED", Color.RED),
            Map.entry("SILVER", Color.SILVER),
            Map.entry("TEAL", Color.TEAL),
            Map.entry("WHITE", Color.WHITE),
            Map.entry("YELLOW", Color.YELLOW));

    private static Map<Color, ChatColor> chatColors = Map.ofEntries(
            Map.entry(Color.AQUA, ChatColor.AQUA),
            Map.entry(Color.BLACK, ChatColor.BLACK),
            Map.entry(Color.BLUE, ChatColor.BLUE),
            Map.entry(Color.FUCHSIA, ChatColor.LIGHT_PURPLE),
            Map.entry(Color.GRAY, ChatColor.DARK_GRAY),
            Map.entry(Color.GREEN, ChatColor.DARK_GREEN),
            Map.entry(Color.LIME, ChatColor.GREEN),
            Map.entry(Color.MAROON, ChatColor.DARK_RED),
            Map.entry(Color.NAVY, ChatColor.DARK_BLUE),
            Map.entry(Color.OLIVE, ChatColor.DARK_GREEN),
            Map.entry(Color.ORANGE, ChatColor.GOLD),
            Map.entry(Color.PURPLE, ChatColor.DARK_PURPLE),
            Map.entry(Color.RED, ChatColor.RED),
            Map.entry(Color.SILVER, ChatColor.GRAY),
            Map.entry(Color.TEAL, ChatColor.DARK_AQUA),
            Map.entry(Color.WHITE, ChatColor.WHITE),
            Map.entry(Color.YELLOW, ChatColor.YELLOW));

    public static Map<String, Color> getColors() {
        return ColorTypes.colors;
    }

    public static Color stringToColor(String color) {
        return ColorTypes.colors.getOrDefault(color.toUpperCase(), null);
    }

    public static String colorToString(Color color) {
        for (Map.Entry<String, Color> current : ColorTypes.colors.entrySet()) {
            if (current.getValue() == color) {
                return current.getKey();
            }
        }

        return null;
    }

    public static ChatColor colorToChatColor(final Color color) {
        return ColorTypes.chatColors.getOrDefault(color, null);
    }
}
