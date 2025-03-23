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
    int stepEvery = (trimmedString.length() - 1) / (colorStops.size() - 1);

    for (int index = 0; index < trimmedString.length(); index++) {
      if (index % stepEvery == 0) {
        startColor = colorStops.get(index / stepEvery);
        endColor = colorStops.get(Math.min(index / stepEvery + 1, colorStops.size() - 1));
      }

      float ratio = (float) (index % stepEvery) / stepEvery;
      characterColors.add(TextColor.lerp(ratio, startColor, endColor));
    }

    Bukkit.getLogger().info("Colors: " + characterColors.toString());
    return TextColors.sequence(string, characterColors);
  }

  public static TextComponent rainbowify(String string) {
    return TextColors.sequence(string, TextColors.rainbowColors);
  }
}
