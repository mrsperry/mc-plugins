package com.mrjoshuasperry.levelup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

class LevelUpTest {
  private static Map<String, Object> color(Object alpha, Object red, Object green, Object blue) {
    Map<String, Object> map = new HashMap<>();
    map.put("alpha", alpha);
    map.put("red", red);
    map.put("green", green);
    map.put("blue", blue);
    return map;
  }

  @Test
  void parsesAWellFormedColor() {
    assertEquals(Color.fromARGB(255, 100, 150, 200), Main.parseColor(color(255, 100, 150, 200)));
  }

  @Test
  void parsesNumericStrings() {
    // YAML can hand components back as strings; the parser reads them via toString.
    assertEquals(Color.fromARGB(255, 10, 20, 30), Main.parseColor(color("255", "10", "20", "30")));
  }

  @Test
  void returnsNullForNonNumericComponent() {
    assertNull(Main.parseColor(color(255, "ff", 0, 0)));
  }

  @Test
  void returnsNullForMissingComponent() {
    Map<String, Object> map = new HashMap<>();
    map.put("alpha", 255);
    map.put("red", 100);
    // green and blue absent
    assertNull(Main.parseColor(map));
  }

  @Test
  void returnsNullForOutOfRangeComponent() {
    assertNull(Main.parseColor(color(255, 999, 0, 0)));
  }
}
