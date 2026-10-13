package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

class TextColorsTest {
  private static TextComponent child(Component parent, int index) {
    return (TextComponent) parent.children().get(index);
  }

  @Test
  void sequenceCyclesThroughColors() {
    TextComponent result = TextColors.sequence("ab", List.of(NamedTextColor.RED));

    assertEquals(2, result.children().size());
    assertEquals("a", child(result, 0).content());
    assertSame(NamedTextColor.RED, child(result, 0).color());
    assertSame(NamedTextColor.RED, child(result, 1).color());
  }

  @Test
  void sequencePreservesSpacesWithoutConsumingAColor() {
    TextComponent result = TextColors.sequence("a b", List.of(NamedTextColor.RED, NamedTextColor.GREEN));

    assertEquals(3, result.children().size());
    assertSame(NamedTextColor.RED, child(result, 0).color());
    assertEquals(" ", child(result, 1).content());
    assertSame(NamedTextColor.GREEN, child(result, 2).color());
  }

  @Test
  void sequenceWithNoColorsReturnsPlainText() {
    TextComponent result = TextColors.sequence("x", List.of());

    assertEquals("x", result.content());
    assertTrue(result.children().isEmpty());
  }

  @Test
  void gradientAnchorsAtItsColorStops() {
    TextComponent result = TextColors.gradient("ab", List.of(NamedTextColor.RED, NamedTextColor.AQUA));

    assertSame(NamedTextColor.RED, child(result, 0).color());
    assertSame(NamedTextColor.AQUA, child(result, 1).color());
  }

  @Test
  void gradientWithASingleStopDoesNotDivideByZero() {
    TextComponent result = TextColors.gradient("abc", List.of(NamedTextColor.RED));

    assertEquals(3, result.children().size());
    for (int i = 0; i < 3; i++) {
      assertSame(NamedTextColor.RED, child(result, i).color());
    }
  }

  @Test
  void rainbowifyHandlesASingleCharacter() {
    TextComponent result = TextColors.rainbowify("a");

    // First rainbow stop is RED; the important part is it does not crash on length 1.
    assertSame(NamedTextColor.RED, child(result, 0).color());
  }

  @Test
  void parseTextColorReadsHex() {
    assertEquals(0xFF0000, TextColors.parseTextColor("#ff0000").value());
  }

  @Test
  void parseTextColorReadsNamedColors() {
    assertSame(NamedTextColor.RED, TextColors.parseTextColor("red"));
  }

  @Test
  void parseTextColorFallsBackToDefault() {
    assertNull(TextColors.parseTextColor(null));
    assertSame(NamedTextColor.WHITE, TextColors.parseTextColor(null, NamedTextColor.WHITE));
    assertSame(NamedTextColor.WHITE, TextColors.parseTextColor("not-a-color", NamedTextColor.WHITE));
  }

  private static void assertLegible(TextColor color) {
    assertTrue(TextColors.contrastAgainstBlack(color) >= TextColors.MIN_CHAT_CONTRAST,
        color.asHexString() + " is too dark to read in chat");
  }

  @Test
  void legibleLeavesReadableColorsAlone() {
    for (NamedTextColor color : List.of(NamedTextColor.WHITE, NamedTextColor.GRAY, NamedTextColor.GOLD,
        NamedTextColor.RED, NamedTextColor.GREEN, NamedTextColor.AQUA)) {
      assertSame(color, TextColors.legible(color));
    }
  }

  @Test
  void legibleLightensANearBlackColorWithoutWashingItOut() {
    TextColor deepDark = TextColor.color(1, 15, 48);
    TextColor result = TextColors.legible(deepDark);

    assertLegible(result);
    assertNotEquals(NamedTextColor.WHITE.value(), result.value());
    // Lightened toward white, so it keeps its blue tint
    assertTrue(result.blue() > result.red());
  }

  @Test
  void legibleLightensVanillasDarkColors() {
    for (NamedTextColor color : List.of(NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GRAY,
        NamedTextColor.DARK_RED, NamedTextColor.BLUE)) {
      assertLegible(TextColors.legible(color));
    }
  }

  @Test
  void colorsTooDarkToReadAreKeptAsChosen() {
    // Legibility is a rule for colors the code picks; players may choose any color
    assertSame(NamedTextColor.BLACK, TextColors.parseTextColor("black"));
    assertEquals(0x010f30, TextColors.parseTextColor("#010f30").value());

    TextComponent result = TextColors.sequence("ab", List.of(NamedTextColor.DARK_BLUE, NamedTextColor.BLACK));
    assertSame(NamedTextColor.DARK_BLUE, child(result, 0).color());
    assertSame(NamedTextColor.BLACK, child(result, 1).color());
  }
}
