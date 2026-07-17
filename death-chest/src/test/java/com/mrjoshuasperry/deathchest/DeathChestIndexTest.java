package com.mrjoshuasperry.deathchest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The forward/backward offset pair rearranges the 41 player-inventory slots into
 * the death-chest display and back. Correctness requires two properties: the round
 * trip must be the identity (items land back where they came from), and the forward
 * map must be injective (no two source slots collapse onto one display slot, which
 * would drop an item when both are stored in the same map).
 */
class DeathChestIndexTest {
  private static final int SLOT_COUNT = 41;

  @Test
  void backwardUndoesForwardForEverySlot() {
    for (int slot = 0; slot < SLOT_COUNT; slot++) {
      int roundTripped = DeathChest.getItemIndexBackwardOffset(DeathChest.getItemIndexForwardOffset(slot));
      assertEquals(slot, roundTripped, "round trip failed for slot " + slot);
    }
  }

  @Test
  void forwardOffsetIsInjective() {
    Set<Integer> displaySlots = new HashSet<>();
    for (int slot = 0; slot < SLOT_COUNT; slot++) {
      displaySlots.add(DeathChest.getItemIndexForwardOffset(slot));
    }

    assertEquals(SLOT_COUNT, displaySlots.size(), "two source slots collapsed onto one display slot");
  }

  @Test
  void slotsNineAndEighteenNoLongerCollide() {
    // Regression: the `> 9` bug mapped both player slots 9 and 18 onto display slot 9.
    assertNotEquals(
        DeathChest.getItemIndexForwardOffset(9),
        DeathChest.getItemIndexForwardOffset(18));
  }

  @Test
  void forwardMappingTableIsAsDocumented() {
    // Hotbar (0-8) drops down three rows to 27-35.
    assertEquals(27, DeathChest.getItemIndexForwardOffset(0));
    assertEquals(35, DeathChest.getItemIndexForwardOffset(8));
    // Main inventory (9-35) shifts up one row to 0-26.
    assertEquals(0, DeathChest.getItemIndexForwardOffset(9));
    assertEquals(26, DeathChest.getItemIndexForwardOffset(35));
    // Armor + offhand (36-40) stay put.
    assertEquals(36, DeathChest.getItemIndexForwardOffset(36));
    assertEquals(40, DeathChest.getItemIndexForwardOffset(40));
  }
}
