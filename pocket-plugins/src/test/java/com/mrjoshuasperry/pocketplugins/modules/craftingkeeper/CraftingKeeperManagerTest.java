package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class CraftingKeeperManagerTest {
  private World world;
  private CraftingKeeperManager manager;

  @BeforeEach
  void setUp() {
    ServerMock server = MockBukkit.mock();
    world = server.addSimpleWorld("test");
    manager = CraftingKeeperManager.getInstance();
    // The manager is a process-wide singleton, so state leaks between tests. Loading
    // an empty section resets its map to a known-clean starting point.
    manager.load(new MemoryConfiguration());
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void saveAndLoadReproducesSparseContents() {
    Location location = new Location(world, 1, 2, 3);
    ItemStack[] contents = new ItemStack[9];
    contents[0] = new ItemStack(Material.DIAMOND, 5);
    contents[3] = new ItemStack(Material.STONE, 2);
    manager.saveInventory(location, contents);

    MemoryConfiguration root = new MemoryConfiguration();
    manager.save(root);
    manager.load(root);

    ItemStack[] restored = manager.getSavedInventory(new Location(world, 1, 2, 3));
    assertNotNull(restored);
    // The array length is restored from "size", even though only two slots were set.
    assertEquals(9, restored.length);
    assertEquals(Material.DIAMOND, restored[0].getType());
    assertEquals(5, restored[0].getAmount());
    assertNull(restored[1], "empty slots stay empty");
    assertEquals(Material.STONE, restored[3].getType());
  }

  @Test
  void isSavedTracksSaveAndRemove() {
    Location location = new Location(world, 10, 20, 30);
    assertFalse(manager.isSaved(location));

    manager.saveInventory(location, new ItemStack[] { new ItemStack(Material.DIRT) });
    assertTrue(manager.isSaved(location));

    manager.removeSaved(location);
    assertFalse(manager.isSaved(location));
  }

  @Test
  void loadSkipsTablesWithoutALocation() {
    MemoryConfiguration root = new MemoryConfiguration();
    root.set("0.size", 5); // a numbered table carrying a size but no location
    manager.load(root);

    assertNull(manager.getSavedInventory(new Location(world, 1, 2, 3)));
  }
}
