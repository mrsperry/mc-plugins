package com.mrjoshuasperry.mcutils.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import net.kyori.adventure.text.Component;

class ItemBuilderTest {
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void setsTypeAndAmount() {
    ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD).setAmount(3).build();

    assertEquals(Material.DIAMOND_SWORD, item.getType());
    assertEquals(3, item.getAmount());
  }

  @Test
  void setMaterialChangesType() {
    ItemStack item = new ItemBuilder(Material.STONE).setMaterial(Material.DIRT).build();

    assertEquals(Material.DIRT, item.getType());
  }

  @Test
  void addLoreAccumulatesLines() {
    ItemStack item = new ItemBuilder(Material.PAPER)
        .addLore(Component.text("first"))
        .addLore(Component.text("second"))
        .build();

    assertNotNull(item.getItemMeta().lore());
    assertEquals(2, item.getItemMeta().lore().size());
  }

  @Test
  void addEnchantmentIsApplied() {
    ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
        .addEnchantment(Enchantment.SHARPNESS, 3)
        .build();

    assertEquals(3, item.getEnchantmentLevel(Enchantment.SHARPNESS));
  }

  @Test
  void setNameSetsItemName() {
    ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
        .setName(Component.text("Excalibur"))
        .build();

    assertEquals(Component.text("Excalibur"), item.getItemMeta().itemName());
  }
}
