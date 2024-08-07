package com.mrjoshuasperry.deathchest;

import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.kyori.adventure.text.Component;

public class DeathChestInventory implements InventoryHolder {
  private final Inventory inventory;
  private final Chest chest;

  public DeathChestInventory(Main plugin, Chest chest, int slots, Component title) {
    this.inventory = plugin.getServer().createInventory(this, slots, title);
    this.chest = chest;
  }

  @Override
  public Inventory getInventory() {
    return this.inventory;
  }

  public Chest getChest() {
    return this.chest;
  }
}