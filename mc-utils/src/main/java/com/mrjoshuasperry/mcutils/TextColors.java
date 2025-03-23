package com.mrjoshuasperry.mcutils;

import java.util.List;

import com.google.common.collect.Lists;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class TextColors {
  private static final List<NamedTextColor> rainbowColors = Lists.newArrayList(
      NamedTextColor.RED,
      NamedTextColor.GOLD,
      NamedTextColor.YELLOW,
      NamedTextColor.GREEN,
      NamedTextColor.BLUE,
      NamedTextColor.LIGHT_PURPLE);

  public static TextComponent sequence(String string, List<NamedTextColor> colors) {
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

  public static TextComponent rainbowify(String string) {
    return TextColors.sequence(string, TextColors.rainbowColors);
  }
}
