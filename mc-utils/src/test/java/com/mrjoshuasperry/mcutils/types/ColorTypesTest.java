package com.mrjoshuasperry.mcutils.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

class ColorTypesTest {
  @Test
  void stringToColorIsCaseInsensitive() {
    assertSame(Color.RED, ColorTypes.stringToColor("red"));
    assertSame(Color.RED, ColorTypes.stringToColor("RED"));
    assertSame(Color.AQUA, ColorTypes.stringToColor("Aqua"));
  }

  @Test
  void stringToColorReturnsNullForUnknown() {
    assertNull(ColorTypes.stringToColor("not-a-color"));
  }

  @Test
  void colorToStringResolvesKnownConstants() {
    assertEquals("RED", ColorTypes.colorToString(Color.RED));
    assertEquals("YELLOW", ColorTypes.colorToString(Color.YELLOW));
  }

  @Test
  void colorToStringReturnsNullForUnmappedColor() {
    assertNull(ColorTypes.colorToString(Color.fromRGB(1, 2, 3)));
  }

  @Test
  void roundTripsThroughBothDirections() {
    for (String name : ColorTypes.getColors().keySet()) {
      Color color = ColorTypes.stringToColor(name);
      assertEquals(name, ColorTypes.colorToString(color));
    }
  }
}
