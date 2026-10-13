package com.mrjoshuasperry.pocketplugins.modules.armorswapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

class ArmorSwapperTest {
  private ArmorStand stand;

  @BeforeEach
  void setUp() {
    ServerMock server = MockBukkit.mock();
    WorldMock world = server.addSimpleWorld("world");
    stand = world.spawn(new Location(world, 0, 64, 0), ArmorStand.class);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void anOrdinaryStandIsSwappable() {
    assertTrue(ArmorSwapper.isSwappable(stand));
  }

  @Test
  void invisibleAndMarkerStandsAreLeftAlone() {
    stand.setVisible(false);
    assertFalse(ArmorSwapper.isSwappable(stand), "invisible stands carry things like flying biome bombs");

    stand.setVisible(true);
    stand.setMarker(true);
    assertFalse(ArmorSwapper.isSwappable(stand));
  }

  @Test
  void aSlotWithAnyLockIsLeftAlone() {
    // MockBukkit has no equipment locks, so this drives the check with each lock directly
    assertTrue(ArmorSwapper.noLock(lock -> false));
    for (ArmorStand.LockType locked : ArmorStand.LockType.values()) {
      assertFalse(ArmorSwapper.noLock(lock -> lock == locked), locked + " should block the swap");
    }
  }
}
