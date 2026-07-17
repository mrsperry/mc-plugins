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

/** @author mrsperry */
public class MobSizes extends Module {
  protected final int maxMountHealth = 30;
  protected final int minMountHealth = 15;

  protected double minSize;
  protected double maxSize;
  protected boolean enableMountHealthSizing = true;

  public MobSizes(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

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

    if (this.enableMountHealthSizing && entity instanceof AbstractHorse) {
      this.setMountScale((AbstractHorse) entity);
      return;
    }

    double scale = this.getPlugin().getRandom().nextDouble(this.minSize, this.maxSize);
    this.setScale(entity, scale);
  }

  protected void setMountScale(AbstractHorse entity) {
    AttributeInstance healthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
    if (healthAttribute == null) {
      return;
    }

    double scale = healthToScale(healthAttribute.getBaseValue(),
        this.minSize, this.maxSize, this.minMountHealth, this.maxMountHealth);
    this.setScale(entity, scale);
  }

  /**
   * Linearly maps a mount's max health onto the [minSize, maxSize] range and clamps
   * to it. Package-private and static so the interpolation + clamp is unit-testable
   * without a live entity/attribute.
   */
  static double healthToScale(double maxHealth, double minSize, double maxSize, int minMountHealth,
      int maxMountHealth) {
    double scale = minSize + (maxHealth - minMountHealth) * (maxSize - minSize) / (maxMountHealth - minMountHealth);
    return Math.max(minSize, Math.min(maxSize, scale));
  }

  private void setScale(LivingEntity entity, double scale) {
    AttributeInstance scaleAttribute = entity.getAttribute(Attribute.SCALE);
    if (scaleAttribute == null) {
      return;
    }

    scaleAttribute.setBaseValue(scale);
  }
}
