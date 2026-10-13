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
  /**
   * Lowest contrast ratio against black that chat text may have. Chat and tooltips sit
   * on a dark background, so anything darker (a near-black biome color, or even
   * vanilla's {@code blue}) is hard to read. 4.5 is the WCAG AA threshold for text.
   */
  public static final double MIN_CHAT_CONTRAST = 4.5;

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
        // Checked per character: a gradient between two legible stops can still dip
        builder.append(Component.text(part, TextColors.legible(colors.get(index))));
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

  /**
   * Parses a hex ({@code #rrggbb}) or named color for chat. Colors come from config
   * and players, so the result is always run through {@link #legible}.
   */
  public static TextColor parseTextColor(String colorString, TextColor defaultColor) {
    if (colorString == null) {
      return TextColors.legible(defaultColor);
    }

    if (colorString.startsWith("#")) {
      TextColor color = TextColor.fromHexString(colorString);

      if (color == null) {
        Bukkit.getLogger().warning("Invalid hex color: " + colorString);
        return TextColors.legible(defaultColor);
      }

      return TextColors.legible(color);
    }

    TextColor namedColor = NamedTextColor.NAMES.value(colorString);
    return TextColors.legible(namedColor == null ? defaultColor : namedColor);
  }

  /**
   * Returns the color unchanged if it is legible on chat's dark background, or
   * lightened toward white just enough that it is. Use it on any color that comes
   * from config or a player before it reaches chat, an item name or lore.
   */
  public static TextColor legible(TextColor color) {
    if (color == null || TextColors.contrastAgainstBlack(color) >= MIN_CHAT_CONTRAST) {
      return color;
    }

    for (int step = 1; step < 20; step++) {
      TextColor lighter = TextColor.lerp(step / 20f, color, NamedTextColor.WHITE);
      if (TextColors.contrastAgainstBlack(lighter) >= MIN_CHAT_CONTRAST) {
        return lighter;
      }
    }

    return NamedTextColor.WHITE;
  }

  /** The WCAG contrast ratio of a color against black, from 1 (black) to 21 (white). */
  public static double contrastAgainstBlack(TextColor color) {
    double luminance = 0.2126 * TextColors.linear(color.red())
        + 0.7152 * TextColors.linear(color.green())
        + 0.0722 * TextColors.linear(color.blue());
    return (luminance + 0.05) / 0.05;
  }

  private static double linear(int channel) {
    double value = channel / 255.0;
    return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
  }
}
