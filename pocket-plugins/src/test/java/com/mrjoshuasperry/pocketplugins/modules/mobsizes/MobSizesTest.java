package com.mrjoshuasperry.pocketplugins.modules.mobsizes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobSizesTest {
  private static final double MIN_SIZE = 0.85;
  private static final double MAX_SIZE = 1.15;
  private static final int MIN_HP = 15;
  private static final int MAX_HP = 30;
  private static final double EPS = 1e-9;

  private static double scaleFor(double health) {
    return MobSizes.healthToScale(health, MIN_SIZE, MAX_SIZE, MIN_HP, MAX_HP);
  }

  @Test
  void minHealthMapsToMinSize() {
    assertEquals(MIN_SIZE, scaleFor(15), EPS);
  }

  @Test
  void maxHealthMapsToMaxSize() {
    assertEquals(MAX_SIZE, scaleFor(30), EPS);
  }

  @Test
  void midpointHealthInterpolates() {
    assertEquals(1.0, scaleFor(22.5), EPS);
  }

  @Test
  void healthBelowRangeClampsToMinSize() {
    assertEquals(MIN_SIZE, scaleFor(5), EPS);
  }

  @Test
  void healthAboveRangeClampsToMaxSize() {
    assertEquals(MAX_SIZE, scaleFor(50), EPS);
  }
}
