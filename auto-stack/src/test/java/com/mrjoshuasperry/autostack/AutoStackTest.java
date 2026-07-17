package com.mrjoshuasperry.autostack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class AutoStackTest {
  private Player player;

  @BeforeEach
  void setUp() {
    ServerMock server = MockBukkit.mock();
    player = server.addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void refillsTheHandFromAnotherMatchingStack() {
    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.DIRT, 1));
    inventory.setItem(20, new ItemStack(Material.DIRT, 30));

    Main.restackHand(inventory, inventory.getItemInMainHand(), EquipmentSlot.HAND);

    assertEquals(Material.DIRT, inventory.getItemInMainHand().getType());
    assertEquals(30, inventory.getItemInMainHand().getAmount());

    ItemStack source = inventory.getItem(20);
    assertTrue(source == null || source.getAmount() == 0, "the source stack should be emptied");
  }

  @Test
  void leavesTheHandAloneWhenNoMatchingStackExists() {
    PlayerInventory inventory = player.getInventory();
    inventory.setItemInMainHand(new ItemStack(Material.DIRT, 1));
    inventory.setItem(20, new ItemStack(Material.STONE, 30));

    Main.restackHand(inventory, inventory.getItemInMainHand(), EquipmentSlot.HAND);

    assertEquals(1, inventory.getItemInMainHand().getAmount());
    assertEquals(30, inventory.getItem(20).getAmount());
  }
}
