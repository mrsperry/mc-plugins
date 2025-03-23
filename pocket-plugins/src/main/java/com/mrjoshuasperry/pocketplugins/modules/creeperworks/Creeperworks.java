package com.mrjoshuasperry.pocketplugins.modules.creeperworks;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class Creeperworks extends Module {
  public Creeperworks(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
  }

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof Creeper)) {
      return;
    }

    Firework firework = (Firework) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.FIREWORK_ROCKET);
    FireworkMeta meta = firework.getFireworkMeta();
    meta.setPower(this.getPlugin().getRandom().nextInt(1, 3));
    meta.addEffect(FireworkEffect.builder().withColor(Color.LIME).with(Type.CREEPER).build());
    firework.setFireworkMeta(meta);
  }
}
