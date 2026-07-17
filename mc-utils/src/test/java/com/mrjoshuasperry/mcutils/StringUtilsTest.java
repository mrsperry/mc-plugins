package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StringUtilsTest {
  @Test
  void capitalizesEachWord() {
    assertEquals("Hello World", StringUtils.capitalize("hello world"));
  }

  @Test
  void lowercasesTheRestOfEachWord() {
    assertEquals("Hello", StringUtils.capitalize("hELLO"));
    assertEquals("Hello World", StringUtils.capitalize("HELLO WORLD"));
  }

  @Test
  void capitalizesSingleCharacter() {
    assertEquals("X", StringUtils.capitalize("x"));
  }

  @Test
  void passesNullAndEmptyThrough() {
    assertNull(StringUtils.capitalize(null));
    assertEquals("", StringUtils.capitalize(""));
  }

  @Test
  void doubledSpacesDoNotCrash() {
    // Regression: split(" ") yields an empty token between doubled spaces, which
    // used to hit "".substring(0, 1) and throw. Spacing must be preserved.
    assertEquals("A  B", StringUtils.capitalize("a  b"));
    assertEquals(" A", StringUtils.capitalize(" a"));
  }

  @Test
  void toEnumNameUppercasesAndReplacesSpaces() {
    assertEquals("SWEET_BERRY_BUSH", StringUtils.toEnumName("Sweet Berry Bush"));
    assertEquals("STONE", StringUtils.toEnumName("stone"));
  }

  @Test
  void toEnumNamePassesNullThrough() {
    assertNull(StringUtils.toEnumName(null));
  }
}
