package com.mrjoshuasperry.pocketplugins.modules.lightningrods;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockIgniteEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/**
 * Makes a lightning rod actually ground the strike it attracted: fires that
 * vanilla would light around the impact are suppressed when a rod is what got
 * hit. A strike anywhere else still burns things down as usual.
 */
public class LightningRods extends Module {
  protected int searchRadius;

  public LightningRods(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.searchRadius = readableConfig.getInt("search-radius", 2);
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockIgnite(BlockIgniteEvent event) {
    if (event.getCause() != BlockIgniteEvent.IgniteCause.LIGHTNING) {
      return;
    }

    Entity igniter = event.getIgnitingEntity();
    // Paper hands over the strike itself; fall back to the fire's own position so
    // a null igniter doesn't quietly turn the module off
    Location origin = igniter instanceof LightningStrike
        ? igniter.getLocation()
        : event.getBlock().getLocation();

    if (this.hasLightningRodNear(origin)) {
      event.setCancelled(true);
    }
  }

  /**
   * Lightning redirected by a rod spawns at the rod's own block, so a small cube
   * around the impact is enough to tell an attracted strike from a stray one.
   */
  protected boolean hasLightningRodNear(Location origin) {
    World world = origin.getWorld();
    int originX = origin.getBlockX();
    int originY = origin.getBlockY();
    int originZ = origin.getBlockZ();

    int minY = Math.max(originY - this.searchRadius, world.getMinHeight());
    int maxY = Math.min(originY + this.searchRadius, world.getMaxHeight() - 1);

    for (int x = -this.searchRadius; x <= this.searchRadius; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = -this.searchRadius; z <= this.searchRadius; z++) {
          if (world.getBlockAt(originX + x, y, originZ + z).getType() == Material.LIGHTNING_ROD) {
            return true;
          }
        }
      }
    }

    return false;
  }
}
