package com.mrjoshuasperry.mcutils.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PairTupleTest {
  @Test
  void pairExposesBothValues() {
    Pair<String, Integer> pair = new Pair<>("key", 42);

    assertEquals("key", pair.getKey());
    assertEquals(42, pair.getValue());
  }

  @Test
  void pairAllowsNullMembers() {
    Pair<String, String> pair = new Pair<>(null, null);

    assertNull(pair.getKey());
    assertNull(pair.getValue());
  }

  @Test
  void tupleExposesAllThreeValues() {
    Tuple<String, Integer, Boolean> tuple = new Tuple<>("a", 1, true);

    assertEquals("a", tuple.getValue1());
    assertEquals(1, tuple.getValue2());
    assertEquals(true, tuple.getValue3());
  }
}
