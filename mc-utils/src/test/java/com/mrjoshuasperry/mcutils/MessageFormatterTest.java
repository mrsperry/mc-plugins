package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

class MessageFormatterTest {
  private static String plain(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  @Test
  void addWorldNameUsesAGrayNameInDarkGrayBrackets() {
    Component expected = Component.text("[", NamedTextColor.DARK_GRAY)
        .append(Component.text("world").color(NamedTextColor.GRAY))
        .append(Component.text("]", NamedTextColor.DARK_GRAY));

    assertEquals(expected, MessageFormatter.addWorldName("world"));
  }

  @Test
  void addTimestampFormatsAsBracketedTime() {
    String rendered = plain(MessageFormatter.addTimestamp());
    assertTrue(rendered.matches("\\[\\d{2}:\\d{2}\\]"), rendered);
  }

  @Test
  void addTimeAndWorldAppendsTheWorldAfterTheTimestamp() {
    String rendered = plain(MessageFormatter.addTimeAndWorld("nether"));
    assertTrue(rendered.matches("\\[\\d{2}:\\d{2}\\]\\[nether\\]"), rendered);
  }
}
