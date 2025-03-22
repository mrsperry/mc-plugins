package com.mrjoshuasperry.pocketplugins.modules.mobsizes;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class MobSizes extends Module {
  protected final int maxMountHealth = 30;
  protected final int minMountHealth = 15;

  protected double minSize;
  protected double maxSize;
  protected boolean enableMountHealthSizing = true;

  public MobSizes() {
    super("MobSizes");
  }

  @Override
  public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super.initialize(readableConfig, writableConfig);

    this.minSize = readableConfig.getDouble("min-size", 0.85);
    this.maxSize = readableConfig.getDouble("max-size", 1.15);
    this.enableMountHealthSizing = readableConfig.getBoolean("enable-mount-health-sizing", true);
  }

  @EventHandler
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    LivingEntity entity = event.getEntity();
    if (!(entity instanceof Mob)) {
      return;
    }

    if (entity instanceof AbstractHorse) {
      this.setMountScale((AbstractHorse) entity);
      return;
    }

    double scale = this.getPlugin().getRandom().nextDouble(this.minSize, this.maxSize);
    entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
  }

  protected void setMountScale(AbstractHorse entity) {
    AttributeInstance healthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
    if (healthAttribute == null) {
      return;
    }

    double maxHealth = healthAttribute.getBaseValue();
    // Calculate the scale based on the mount's max health, clamped to the min and
    // max sizes
    double scale = minSize + (maxHealth - this.minMountHealth) * (this.maxSize - this.minSize)
        / (this.maxMountHealth - this.minMountHealth);

    entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
  }
}
