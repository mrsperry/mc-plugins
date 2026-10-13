package com.mrjoshuasperry.pocketplugins.modules.shulkerpeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ShulkerPeekTest {
  private static final NamespacedKey LOCK = new NamespacedKey("pocketplugins", "peek");

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void theBoxCantBeClickedOrSwappedOutOfItsSlot() {
    assertTrue(ShulkerPeek.movesBox(true, 4, ClickType.LEFT, -1, 4));
    assertTrue(ShulkerPeek.movesBox(true, 4, ClickType.DROP, -1, 4));
    assertTrue(ShulkerPeek.movesBox(false, 0, ClickType.NUMBER_KEY, 4, 4), "a number key pulls the box into the view");
    assertTrue(ShulkerPeek.movesBox(true, 9, ClickType.SWAP_OFFHAND, -1, ShulkerPeek.OFF_HAND_SLOT));
  }

  @Test
  void everythingElseIsAnOrdinaryClick() {
    assertFalse(ShulkerPeek.movesBox(false, 4, ClickType.LEFT, -1, 4), "slot 4 of the view isn't the box");
    assertFalse(ShulkerPeek.movesBox(true, 5, ClickType.SHIFT_LEFT, -1, 4));
    assertFalse(ShulkerPeek.movesBox(false, 0, ClickType.NUMBER_KEY, 3, 4));
    assertFalse(ShulkerPeek.movesBox(true, 9, ClickType.SWAP_OFFHAND, -1, 4), "the box isn't in the off hand");
  }

  @Test
  void storedStacksKeepTheirSlots() {
    ItemStack stone = new ItemStack(Material.STONE, 3);
    ItemStack[] slots = ShulkerPeek.toSlots(List.of(new ItemStack(Material.AIR), stone));

    assertEquals(ShulkerPeek.SIZE, slots.length);
    assertNull(slots[0]);
    assertEquals(stone, slots[1]);
    assertNull(slots[26], "the component leaves off trailing gaps");
  }

  @Test
  void gapsBecomeEmptyStacksOnTheWayBack() {
    ItemStack stone = new ItemStack(Material.STONE);
    List<ItemStack> contents = ShulkerPeek.fromSlots(new ItemStack[] { null, stone });

    assertEquals(2, contents.size());
    assertTrue(contents.get(0).isEmpty());
    assertEquals(stone, contents.get(1));
  }

  @Test
  void aLockComesOffLeavingTheBoxAsItWas() {
    ItemStack box = new ItemStack(Material.RED_SHULKER_BOX);
    ItemStack locked = ShulkerPeek.withLock(box, LOCK, "abc");

    assertEquals("abc", ShulkerPeek.lockOf(locked, LOCK));
    assertNull(ShulkerPeek.lockOf(box, LOCK), "locking works on a copy");
    assertEquals(box, ShulkerPeek.withoutLock(locked, LOCK));
    assertNull(ShulkerPeek.lockOf(null, LOCK));
  }
}
