package com.mrjoshuasperry.mcutils.types;

import java.util.Map;

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
}
