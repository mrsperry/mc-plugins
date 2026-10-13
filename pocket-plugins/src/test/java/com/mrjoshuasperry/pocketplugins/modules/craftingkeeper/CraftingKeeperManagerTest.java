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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class CraftingKeeperManagerTest {
  private ServerMock server;
  private World world;
  private CraftingKeeperManager manager;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
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
  void takingAGridLeavesNothingBehindForTheNextPlayer() {
    Location location = new Location(world, 4, 5, 6);
    manager.saveInventory(location, new ItemStack[] { new ItemStack(Material.DIAMOND) });

    ItemStack[] taken = manager.take(location);

    assertEquals(Material.DIAMOND, taken[0].getType());
    assertNull(manager.take(location), "a second opener must not get a copy");
  }

  @Test
  void aGridInAWorldThatIsNotLoadedIsKept() {
    MemoryConfiguration root = new MemoryConfiguration();
    root.set("0.world", "unloaded_world");
    root.set("0.x", 1);
    root.set("0.y", 2);
    root.set("0.z", 3);
    root.set("0.size", 1);
    root.set("0.contents.0", new ItemStack(Material.EMERALD, 3));
    manager.load(root);

    MemoryConfiguration saved = new MemoryConfiguration();
    manager.save(saved);

    assertEquals("unloaded_world", saved.getString("0.world"));
    assertEquals(3, saved.getItemStack("0.contents.0").getAmount());
  }

  @Test
  void filesFromBeforeTheWorldNameFormatStillLoad() {
    MemoryConfiguration root = new MemoryConfiguration();
    root.set("0.location", new Location(world, 7, 8, 9));
    root.set("0.size", 1);
    root.set("0.contents.0", new ItemStack(Material.GOLD_INGOT));
    manager.load(root);

    assertEquals(Material.GOLD_INGOT, manager.getSavedInventory(new Location(world, 7, 8, 9))[0].getType());
  }

  @Test
  void aWorkbenchIsTheClickedTableOnlyWhileThePlayerIsStillAtIt() {
    Location table = new Location(world, 0, 64, 0);
    table.getBlock().setType(Material.CRAFTING_TABLE);
    Player player = server.addPlayer();

    player.teleport(new Location(world, 2, 64, 0));
    assertTrue(CraftingKeeper.isTableFor(table, player));

    player.teleport(new Location(world, 50, 64, 0));
    assertFalse(CraftingKeeper.isTableFor(table, player), "a stale click on a far table doesn't count");

    player.teleport(new Location(world, 2, 64, 0));
    table.getBlock().setType(Material.AIR);
    assertFalse(CraftingKeeper.isTableFor(table, player), "nor does one that has since gone");
  }

  @Test
  void loadSkipsTablesWithoutALocation() {
    MemoryConfiguration root = new MemoryConfiguration();
    root.set("0.size", 5); // a numbered table carrying a size but no location
    manager.load(root);

    assertNull(manager.getSavedInventory(new Location(world, 1, 2, 3)));
  }
}
