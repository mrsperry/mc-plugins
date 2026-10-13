package com.mrjoshuasperry.pocketplugins.modules.biomebombs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import com.mrjoshuasperry.mcutils.TextColors;

class BiomeBombDataTest {
  private static ConfigurationSection validSection() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("biome", "birch_forest");
    section.set("catalyst", "DIRT");
    section.set("color.red", 10);
    section.set("color.green", 20);
    section.set("color.blue", 30);
    return section;
  }

  private static void assertRejectedWith(String message, ConfigurationSection section) {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new BiomeBombData(section));
    assertEquals(message, ex.getMessage());
  }

  @Test
  void parsesAValidSection() {
    BiomeBombData data = new BiomeBombData(validSection());

    assertEquals("BIRCH_FOREST", data.getBiomeType());
    assertEquals("birch forest", data.getBiomeName());
    assertEquals(Material.DIRT, data.getCatalyst());
    assertEquals(Color.fromRGB(10, 20, 30), data.getColor());
  }

  @Test
  void aColorTooDarkForChatIsLightenedForTextOnly() {
    BiomeBombData data = new BiomeBombData(validSection());

    // The particles and firework keep the configured color; only the name is lightened
    assertEquals(Color.fromRGB(10, 20, 30), data.getColor());
    assertTrue(TextColors.contrastAgainstBlack(data.getTextColor()) >= TextColors.MIN_CHAT_CONTRAST);
  }

  @Test
  void requiresBiomeName() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("catalyst", "DIRT");
    assertRejectedWith("no biome name", section);
  }

  @Test
  void requiresCatalyst() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("biome", "plains");
    assertRejectedWith("no catalyst", section);
  }

  @Test
  void rejectsUnknownCatalystMaterial() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("biome", "plains");
    section.set("catalyst", "NOT_A_MATERIAL");
    assertRejectedWith("'NOT_A_MATERIAL' is not a material", section);
  }

  @Test
  void requiresColorSection() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("biome", "plains");
    section.set("catalyst", "DIRT");
    assertRejectedWith("no color", section);
  }

  @Test
  void rejectsColorChannelOutOfRange() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("biome", "plains");
    section.set("catalyst", "DIRT");
    section.set("color.red", 10);
    section.set("color.green", 20);
    section.set("color.blue", 300);
    assertRejectedWith("color has a value outside 0-255", section);
  }
}
