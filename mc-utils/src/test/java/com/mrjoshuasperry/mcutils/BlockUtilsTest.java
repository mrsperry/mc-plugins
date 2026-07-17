package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

class BlockUtilsTest {
  private static final double EPS = 1e-6;

  @Test
  void pureRedIsHueZeroFullSaturationFullBrightness() {
    double[] hsb = BlockUtils.rgbToHsb(Color.fromRGB(255, 0, 0));

    assertEquals(0.0, hsb[0], EPS);
    assertEquals(1.0, hsb[1], EPS);
    assertEquals(1.0, hsb[2], EPS);
  }

  @Test
  void primaryHuesAreSpacedByOneThird() {
    assertEquals(1.0 / 3.0, BlockUtils.rgbToHsb(Color.fromRGB(0, 255, 0))[0], EPS);
    assertEquals(2.0 / 3.0, BlockUtils.rgbToHsb(Color.fromRGB(0, 0, 255))[0], EPS);
  }

  @Test
  void whiteHasNoSaturationAndFullBrightness() {
    double[] hsb = BlockUtils.rgbToHsb(Color.fromRGB(255, 255, 255));

    assertEquals(0.0, hsb[1], EPS);
    assertEquals(1.0, hsb[2], EPS);
  }

  @Test
  void blackIsAllZero() {
    double[] hsb = BlockUtils.rgbToHsb(Color.fromRGB(0, 0, 0));

    assertEquals(0.0, hsb[0], EPS);
    assertEquals(0.0, hsb[1], EPS);
    assertEquals(0.0, hsb[2], EPS);
  }
}
