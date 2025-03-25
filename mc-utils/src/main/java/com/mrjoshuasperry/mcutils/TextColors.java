package com.mrjoshuasperry.mcutils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import com.google.common.collect.Lists;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class TextColors {
  private static final List<TextColor> rainbowColors = Lists.newArrayList(
      NamedTextColor.RED,
      NamedTextColor.GOLD,
      NamedTextColor.YELLOW,
      NamedTextColor.GREEN,
      NamedTextColor.BLUE,
      NamedTextColor.LIGHT_PURPLE);

  public static TextComponent sequence(String string, List<TextColor> colors) {
    TextComponent.Builder builder = Component.text();
    int index = 0;

    if (colors.isEmpty()) {
      return Component.text(string);
    }

    for (String part : string.split("")) {
      if (part.equals(" ")) {
        builder.append(Component.space());
      } else {
        builder.append(Component.text(part, colors.get(index)));
        index = (index + 1) % colors.size();
      }
    }

    return builder.build();
  }

  public static TextComponent gradient(String string, List<TextColor> colorStops) {
    List<TextColor> characterColors = new ArrayList<>();

    String trimmedString = string.replace(" ", "");

    TextColor startColor = colorStops.get(0);
    TextColor endColor = colorStops.get(0);

    int stepEvery = (int) Math.ceil((trimmedString.length() - 1f) / (colorStops.size() - 1f));
    if (stepEvery <= 0) {
      stepEvery = 1;
    }

    for (int index = 0; index < trimmedString.length(); index++) {
      if (index % stepEvery == 0) {
        startColor = colorStops.get(index / stepEvery);
        endColor = colorStops.get(Math.min(index / stepEvery + 1, colorStops.size() - 1));
      }

      float ratio = (float) (index % stepEvery) / stepEvery;
      characterColors.add(TextColor.lerp(ratio, startColor, endColor));
    }

    return TextColors.sequence(string, characterColors);
  }

  public static TextComponent rainbowify(String string) {
    return TextColors.gradient(string, TextColors.rainbowColors);
  }

  public static TextColor parseTextColor(String colorString) {
    return TextColors.parseTextColor(colorString, null);
  }

  public static TextColor parseTextColor(String colorString, TextColor defaultColor) {
    if (colorString == null) {
      return defaultColor;
    }

    if (colorString.startsWith("#")) {
      TextColor color = TextColor.fromHexString(colorString);

      if (color == null) {
        Bukkit.getLogger().warning("Invalid hex color: " + colorString);
        return defaultColor;
      }

      return color;
    }

    TextColor namedColor = NamedTextColor.NAMES.value(colorString);
    return namedColor == null ? defaultColor : namedColor;
  }
}
