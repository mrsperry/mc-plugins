package com.mrjoshuasperry.compressedmobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

class CompressedMobsTest {
  @Test
  void settingsReadConfiguredValues() {
    MemoryConfiguration config = new MemoryConfiguration();
    config.set("compress-chance", 50.0);
    config.set("min-yield", 2);
    config.set("max-yield", 8);

    Settings settings = Settings.fromConfig(config, 100, 3, 5);

    assertEquals(50.0, settings.compressChance());
    assertEquals(2, settings.minYield());
    assertEquals(8, settings.maxYield());
  }

  @Test
  void settingsFallBackToDefaults() {
    Settings settings = Settings.fromConfig(new MemoryConfiguration(), 100, 3, 5);

    assertEquals(100.0, settings.compressChance());
    assertEquals(3, settings.minYield());
    assertEquals(5, settings.maxYield());
  }

  @Test
  void yieldStaysWithinConfiguredBounds() {
    Settings settings = new Settings(100, 3, 5);
    Random random = new Random(42);

    for (int i = 0; i < 1000; i++) {
      int yield = Main.rollYield(settings, random);
      assertTrue(yield >= 3 && yield <= 5, "yield out of bounds: " + yield);
    }
  }

  @Test
  void yieldWithEqualBoundsIsConstant() {
    Settings settings = new Settings(100, 4, 4);
    Random random = new Random(1);

    for (int i = 0; i < 100; i++) {
      assertEquals(4, Main.rollYield(settings, random));
    }
  }
}
