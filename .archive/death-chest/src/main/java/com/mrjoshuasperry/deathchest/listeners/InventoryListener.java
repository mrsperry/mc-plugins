package com.mrjoshuasperry.deathchest.listeners;

import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

import com.mrjoshuasperry.deathchest.DeathChest;
import com.mrjoshuasperry.deathchest.DeathChestInventory;
import com.mrjoshuasperry.deathchest.Main;

public class InventoryListener implements Listener {
  private final Main plugin;

  public InventoryListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    Inventory inventory = event.getInventory();
    InventoryHolder holder = inventory.getHolder();

    if (!(holder instanceof DeathChestInventory)) {
      return;
    }

    Chest chest = ((DeathChestInventory) holder).getChest();

    if (inventory.isEmpty()) {
      DeathChest.destroy(chest);
      return;
    }

    DeathChest.updateChestContents(this.plugin, chest, inventory, false);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory inventory = event.getInventory();
    Inventory clickedInventory = event.getClickedInventory();

    if (inventory == null || clickedInventory == null) {
      return;
    }

    InventoryHolder holder = inventory.getHolder();
    InventoryHolder clickedHolder = clickedInventory.getHolder();

    if (!(holder instanceof DeathChestInventory || clickedHolder instanceof DeathChestInventory)) {
      return;
    }

    InventoryAction action = event.getAction();

    // Prevent moving items into the death chest
    if ((action == InventoryAction.HOTBAR_SWAP
        || action == InventoryAction.PLACE_ALL
        || action == InventoryAction.PLACE_ONE
        || action == InventoryAction.PLACE_SOME
        || action == InventoryAction.SWAP_WITH_CURSOR) && clickedHolder instanceof DeathChestInventory) {
      event.setCancelled(true);
    }

    if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && !(clickedHolder instanceof DeathChestInventory)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    InventoryView view = event.getView();
    Inventory topInventory = view.getTopInventory();
    InventoryHolder holder = topInventory.getHolder();

    if (!(holder instanceof DeathChestInventory)) {
      return;
    }

    int size = topInventory.getSize();
    if (event.getRawSlots().stream().anyMatch(slot -> slot < size)) {
      event.setCancelled(true);
    }
  }
}
