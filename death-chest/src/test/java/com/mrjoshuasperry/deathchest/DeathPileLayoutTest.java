package com.mrjoshuasperry.deathchest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import com.mrjoshuasperry.deathchest.DeathPile.Orbit;
import com.mrjoshuasperry.deathchest.DeathPile.Slot;

/**
 * The pile layout and its orbit encoding are pure, so they can be checked without a
 * live world: stacks fill rings arranged on a sphere — the equator widest and at the
 * center height, further rings inset toward the poles — each ring evenly spread, and
 * the orbit blob round-trips.
 */
class DeathPileLayoutTest {
  private static final double EPSILON = 1e-9;

  @Test
  void oneSlotPerItem() {
    assertEquals(20, DeathPile.layout(20).size());
  }

  @Test
  void everySlotSitsOnItsRingRadius() {
    for (Slot slot : DeathPile.layout(20)) {
      Vector offset = DeathPile.position(slot, 0);
      assertEquals(slot.radius(), Math.hypot(offset.getX(), offset.getZ()), EPSILON);
    }
  }

  @Test
  void ringsFillToCapacityBeforeStartingAnother() {
    // 20 stacks over a capacity of 8 => rings of 8, 8, 4 at three heights.
    Map<Double, Integer> perHeight = new HashMap<>();
    for (Slot slot : DeathPile.layout(20)) {
      perHeight.merge(slot.height(), 1, Integer::sum);
    }

    assertEquals(3, perHeight.size(), "expected three rings");
    assertEquals(8, perHeight.get(DeathPile.CENTER_HEIGHT), "equator fills first");

    List<Integer> counts = new ArrayList<>(perHeight.values());
    counts.sort(null);
    assertEquals(List.of(4, 8, 8), counts);
  }

  @Test
  void equatorIsOutermostAndOtherRingsAreInsetAboveAndBelow() {
    // 17 stacks => rings of 8, 8, 1 => equator plus one ring each above and below.
    List<Slot> slots = DeathPile.layout(17);

    boolean sawAbove = false;
    boolean sawBelow = false;
    for (Slot slot : slots) {
      if (Math.abs(slot.height() - DeathPile.CENTER_HEIGHT) < EPSILON) {
        assertEquals(DeathPile.EQUATOR_RADIUS, slot.radius(), EPSILON, "equator should be widest");
        continue;
      }

      assertTrue(slot.radius() < DeathPile.EQUATOR_RADIUS, "off-equator rings must be inset");
      sawAbove |= slot.height() > DeathPile.CENTER_HEIGHT;
      sawBelow |= slot.height() < DeathPile.CENTER_HEIGHT;
    }

    assertTrue(sawAbove, "expected a ring above the equator");
    assertTrue(sawBelow, "expected a ring below the equator");
  }

  @Test
  void partialRingIsEvenlySpread() {
    // A lone 3-stack ring should be spaced a third of a turn apart.
    List<Slot> slots = DeathPile.layout(3);
    assertEquals(3, slots.size());
    for (int index = 0; index < slots.size(); index++) {
      assertEquals(2.0 * Math.PI * index / 3, slots.get(index).theta0(), EPSILON);
    }
  }

  @Test
  void phaseRotatesEveryItemTogether() {
    Slot slot = DeathPile.layout(1).get(0);
    Vector base = DeathPile.position(slot, 0);
    Vector quarter = DeathPile.position(slot, Math.PI / 2);
    // A quarter turn moves it, but keeps it on the ring and at the same height.
    assertTrue(base.distance(quarter) > EPSILON, "phase should move the item");
    assertEquals(base.getY(), quarter.getY(), EPSILON);
    assertEquals(slot.radius(), Math.hypot(quarter.getX(), quarter.getZ()), EPSILON);
  }

  @Test
  void orbitBlobRoundTrips() throws IOException {
    Slot slot = new Slot(1.2, 1.8, 0.75);
    byte[] blob = DeathPile.writeOrbit(10.5, 64.0, -20.5, slot);
    Orbit decoded = DeathPile.readOrbit(blob);

    assertEquals(10.5, decoded.centerX(), EPSILON);
    assertEquals(64.0, decoded.centerY(), EPSILON);
    assertEquals(-20.5, decoded.centerZ(), EPSILON);
    assertEquals(slot, decoded.slot());
  }
}
