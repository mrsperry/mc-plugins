package com.mrjoshuasperry.deathchest.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class PlayerListenerTest {
  // The enchantment registry needs a server
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void onlyCurseOfVanishingItemsVanish() {
    ItemStack cursed = new ItemStack(Material.DIAMOND_HELMET);
    cursed.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    ItemStack bound = new ItemStack(Material.DIAMOND_CHESTPLATE);
    bound.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 1);

    assertEquals(List.of(cursed),
        PlayerListener.vanishing(new ItemStack[] { null, new ItemStack(Material.TORCH), cursed, bound }));
  }

  @Test
  void survivalAndCreativeDeathsLeaveAPile() {
    assertTrue(PlayerListener.leavesAPile(GameMode.SURVIVAL));
    assertTrue(PlayerListener.leavesAPile(GameMode.CREATIVE));
  }

  @Test
  void adventureAndSpectatorDeathsDropAsVanilla() {
    assertFalse(PlayerListener.leavesAPile(GameMode.ADVENTURE));
    assertFalse(PlayerListener.leavesAPile(GameMode.SPECTATOR));
  }
}
