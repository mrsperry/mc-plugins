package com.mrjoshuasperry.pocketplugins.modules.mobgriefing;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class MobGriefing extends Module {
  protected boolean noSheepGriefing;
  protected boolean noEndermanGriefing;

  public MobGriefing(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.noSheepGriefing = readableConfig.getBoolean("no-sheep-griefing", true);
    this.noEndermanGriefing = readableConfig.getBoolean("no-enderman-griefing", true);
  }

  @EventHandler
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    Entity entity = event.getEntity();
    if (entity == null) {
      return;
    }

    EntityType type = entity.getType();

    if (this.noSheepGriefing && type == EntityType.SHEEP) {
      entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRASS_PLACE, 1, 1);
      event.setCancelled(true);
      return;
    }

    if (this.noEndermanGriefing && type == EntityType.ENDERMAN) {
      event.setCancelled(true);
      return;
    }
  }
}
