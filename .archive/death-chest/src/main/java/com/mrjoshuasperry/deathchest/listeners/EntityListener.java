package com.mrjoshuasperry.deathchest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.deathchest.Main;

public class EntityListener implements Listener {
  private final Main plugin;

  public EntityListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent event) {
    Bukkit.getLogger().info("Entity explode event");

    event.blockList().removeIf(block -> {
      if (block.getType() != Material.CHEST) {
        return false;
      }

      Chest chest = (Chest) block.getState();
      PersistentDataContainer container = chest.getPersistentDataContainer();

      Bukkit.getLogger().info(
          "Chest exploded: " + container.getOrDefault(plugin.getDeathChestKey(), PersistentDataType.BOOLEAN, false));
      return container.getOrDefault(plugin.getDeathChestKey(), PersistentDataType.BOOLEAN, false);
    });
  }
}
