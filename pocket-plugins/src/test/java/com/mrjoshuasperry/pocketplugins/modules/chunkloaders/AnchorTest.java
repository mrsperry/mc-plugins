package com.mrjoshuasperry.pocketplugins.modules.chunkloaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AnchorTest {
  @Test
  void roundTripsThroughSerialization() {
    Anchor anchor = new Anchor("world_the_end", 100, 64, -200);

    Anchor restored = Anchor.deserialize(anchor.serialize());

    assertEquals(anchor, restored);
  }

  @Test
  void derivesChunkCoordinatesForPositiveBlocks() {
    Anchor anchor = new Anchor("world", 33, 70, 16);

    assertEquals(2, anchor.chunkX());
    assertEquals(1, anchor.chunkZ());
  }

  @Test
  void floorsChunkCoordinatesForNegativeBlocks() {
    // -1 divided by 16 truncates to 0, but the block sits in chunk -1; the >> 4
    // shift is what keeps a loader just west of origin covering the right chunks.
    Anchor anchor = new Anchor("world", -1, 70, -17);

    assertEquals(-1, anchor.chunkX());
    assertEquals(-2, anchor.chunkZ());
  }

  @Test
  void toleratesLongsFromYamlWhenDeserializing() {
    Map<String, Object> map = new HashMap<>();
    map.put("world", "world");
    map.put("x", 5L);
    map.put("y", 64L);
    map.put("z", 5L);

    assertEquals(new Anchor("world", 5, 64, 5), Anchor.deserialize(map));
  }

  @Test
  void rejectsAMalformedEntryWithoutThrowing() {
    Map<String, Object> map = new HashMap<>();
    map.put("world", "world");
    map.put("x", "not a number");
    map.put("y", 64);
    map.put("z", 5);

    assertNull(Anchor.deserialize(map));
  }

  @Test
  void rejectsAnEntryMissingTheWorld() {
    Map<String, Object> map = new HashMap<>();
    map.put("x", 5);
    map.put("y", 64);
    map.put("z", 5);

    assertNull(Anchor.deserialize(map));
  }
}
