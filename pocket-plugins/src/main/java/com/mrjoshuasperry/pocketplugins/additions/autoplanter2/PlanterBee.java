package com.mrjoshuasperry.pocketplugins.additions.autoplanter2;

import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Bee;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class PlanterBee {
  protected Bee bee;
  protected Beehive hive;
  protected HiveInventory hiveInventory;

  protected NamespacedKey beeItemKey = new NamespacedKey(PocketPlugins.getInstance(), "bee-item");
  protected NamespacedKey beeCooldownKey = new NamespacedKey(PocketPlugins.getInstance(), "bee-cooldown");

  public PlanterBee(Bee bee, Beehive hive) {
    this.bee = bee;
    this.hive = hive;
    this.hiveInventory = new HiveInventory(hive);
  }

  public long getCooldown(long baseCooldown) {
    PersistentDataContainer container = this.bee.getPersistentDataContainer();

    Long currentCooldown = container.get(this.beeCooldownKey, PersistentDataType.LONG);
    if (currentCooldown == null) {
      return baseCooldown;
    }

    long currentTime = new Date().getTime();
    return currentTime - currentCooldown;
  }

  public void playAngryState() {
    if (!bee.isValid()) {
      return;
    }

    Location location = this.bee.getLocation();
    World world = this.bee.getWorld();

    world.spawnParticle(Particle.ANGRY_VILLAGER, location, 3, 0.25, 0.25, 0.25);
    world.playSound(location, Sound.ENTITY_BEE_HURT, 1, 1);
  }

  public boolean grabSeedFromHive() {
    World world = this.bee.getWorld();

    List<Material> seedTypes = CropTypes.getSeedTypes();
    ItemStack seed = null;

    for (ItemStack item : this.hiveInventory.getInventory().getContents()) {
      if (item == null) {
        continue;
      }

      Material type = item.getType();
      if (!seedTypes.contains(type)) {
        continue;
      }

      seed = new ItemStack(type);
      item.setAmount(item.getAmount() - 1);
    }

    if (seed == null) {
      return false;
    }

    world.playSound(this.bee.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
    this.bee.getPersistentDataContainer().set(this.beeItemKey, PersistentDataType.BYTE_ARRAY, seed.serializeAsBytes());
    this.hiveInventory.save();
    return true;
  }

  protected ItemStack getCarriedSeed() {
    byte[] seed = this.bee.getPersistentDataContainer().get(this.beeItemKey, PersistentDataType.BYTE_ARRAY);
    if (seed == null) {
      return null;
    }

    this.bee.getPersistentDataContainer().remove(this.beeItemKey);
    return ItemStack.deserializeBytes(seed);
  }

  public boolean plantSeed(Location location) {
    this.bee.getPersistentDataContainer().set(this.beeCooldownKey, PersistentDataType.LONG, new Date().getTime());

    World world = this.bee.getWorld();

    ItemStack seed = this.getCarriedSeed();
    if (seed == null) {
      return false;
    }

    Block block = location.getBlock();
    if (!block.getType().isAir() || block.getRelative(BlockFace.DOWN).getType() != Material.FARMLAND) {
      this.playAngryState();
      this.dropCarriedSeed();
      return false;
    }

    world.spawnParticle(Particle.HAPPY_VILLAGER, location, 10, 0.5, 0.25, 0.5);
    world.playSound(location, Sound.ITEM_CROP_PLANT, 1, 1);
    location.getBlock().setType(CropTypes.getCropFromSeed(seed.getType()));
    return true;
  }

  public void dropCarriedSeed() {
    this.bee.getPersistentDataContainer().set(this.beeCooldownKey, PersistentDataType.LONG, new Date().getTime());

    Location location = bee.getLocation();
    World world = bee.getWorld();

    ItemStack seed = this.getCarriedSeed();
    if (seed == null) {
      return;
    }

    world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 1, 1);
    world.dropItemNaturally(location, seed);
  }

  public Bee getBee() {
    return this.bee;
  }
}
