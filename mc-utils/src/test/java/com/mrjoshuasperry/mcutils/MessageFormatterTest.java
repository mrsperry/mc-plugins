package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageFormatterTest {
  @Test
  void addWorldNameWrapsNameInColouredBrackets() {
    // §8 = dark gray, §7 = gray (legacy section-sign colour codes).
    assertEquals("§8[§7world§8]", MessageFormatter.addWorldName("world"));
  }

  @Test
  void addTimestampMatchesTheExpectedStructure() {
    String timestamp = MessageFormatter.addTimestamp();

    assertTrue(timestamp.matches("§8\\[§7\\d{2}:\\d{2}§8\\]"),
        "unexpected timestamp format: " + timestamp);
  }

  @Test
  void addTimeAndWorldConcatenatesBoth() {
    String combined = MessageFormatter.addTimeAndWorld("nether");

    assertTrue(combined.endsWith(MessageFormatter.addWorldName("nether")));
    assertTrue(combined.startsWith("§8[§7"));
  }
}
