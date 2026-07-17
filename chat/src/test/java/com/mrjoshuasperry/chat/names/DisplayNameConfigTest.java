package com.mrjoshuasperry.chat.names;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

class DisplayNameConfigTest {
  @Test
  void loadsColorsPrefixAndSuffix() {
    MemoryConfiguration config = new MemoryConfiguration();
    config.set("name-colors", List.of("red", "#00ff00"));
    config.set("prefix", "[A]");
    config.set("prefix-colors", List.of("blue"));
    config.set("suffix", "[B]");

    DisplayNameConfig loaded = DisplayNameConfig.loadFromConfig(config);

    assertEquals(2, loaded.getNameColors().size());
    assertEquals(NamedTextColor.RED, loaded.getNameColors().get(0));
    assertEquals(0x00ff00, loaded.getNameColors().get(1).value());
    assertEquals("[A]", loaded.getPrefix());
    assertEquals("[B]", loaded.getSuffix());
    assertEquals(1, loaded.getPrefixColors().size());
    assertTrue(loaded.getSuffixColors().isEmpty());
  }

  @Test
  void defaultsToEmptyWhenNothingIsConfigured() {
    DisplayNameConfig loaded = DisplayNameConfig.loadFromConfig(new MemoryConfiguration());

    assertTrue(loaded.getNameColors().isEmpty());
    assertEquals("", loaded.getPrefix());
    assertEquals("", loaded.getSuffix());
  }

  @Test
  void gettersReturnDefensiveCopies() {
    DisplayNameConfig config = new DisplayNameConfig();
    config.setNameColors(new ArrayList<>(List.of(NamedTextColor.RED)));

    List<TextColor> copy = config.getNameColors();
    copy.clear();

    assertEquals(1, config.getNameColors().size());
  }

  @Test
  void settersTrimPrefixAndSuffix() {
    DisplayNameConfig config = new DisplayNameConfig();
    config.setPrefix("  [A]  ");
    config.setSuffix("  [B]  ");

    assertEquals("[A]", config.getPrefix());
    assertEquals("[B]", config.getSuffix());
  }

  @Test
  void saveAndLoadRoundTripsColorValues() {
    // Regression: saveToConfig used to write raw TextColor objects that
    // loadFromConfig (reading string lists) could not read back, losing all colours.
    DisplayNameConfig original = new DisplayNameConfig();
    original.setNameColors(new ArrayList<>(List.of(NamedTextColor.RED, TextColor.color(0x123456))));

    MemoryConfiguration config = new MemoryConfiguration();
    original.saveToConfig(config);
    DisplayNameConfig reloaded = DisplayNameConfig.loadFromConfig(config);

    assertEquals(2, reloaded.getNameColors().size());
    assertEquals(NamedTextColor.RED.value(), reloaded.getNameColors().get(0).value());
    assertEquals(0x123456, reloaded.getNameColors().get(1).value());
  }
}
