package com.mrjoshuasperry.pocketplugins.additions.beeplanter;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Matrix4f;

import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class PlanterBee {
  protected Bee bee;

  protected NamespacedKey beeItemTypeKey;
  protected NamespacedKey beeItemEntityIdKey;

  public PlanterBee(Bee bee) {
    this.bee = bee;

    JavaPlugin plugin = PocketPlugins.getInstance();
    this.beeItemTypeKey = new NamespacedKey(plugin, "bee-item-type");
    this.beeItemEntityIdKey = new NamespacedKey(plugin, "bee-item-entity-id");
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

  protected void pickupSeed(ItemStack seed) {
    World world = this.bee.getWorld();
    Location location = this.bee.getLocation();
    PersistentDataContainer container = this.bee.getPersistentDataContainer();

    world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 1, 1);
    container.set(this.beeItemTypeKey, PersistentDataType.BYTE_ARRAY, seed.serializeAsBytes());

    ItemDisplay item = world.spawn(location, ItemDisplay.class, (ItemDisplay entity) -> {
      entity.setItemStack(seed);
    });

    item.setTransformationMatrix(new Matrix4f().translate(0, -0.7f, 0).scale(0.5f));

    bee.addPassenger(item);
    container.set(this.beeItemEntityIdKey, PersistentDataType.STRING, item.getUniqueId().toString());
  }

  protected ItemStack getCarriedSeed() {
    byte[] seed = this.bee.getPersistentDataContainer().get(this.beeItemTypeKey, PersistentDataType.BYTE_ARRAY);
    if (seed == null) {
      return null;
    }

    this.bee.getPersistentDataContainer().remove(this.beeItemTypeKey);
    return ItemStack.deserializeBytes(seed);
  }

  public boolean plantCarriedSeed(Location location) {
    World world = this.bee.getWorld();

    ItemStack seed = this.getCarriedSeed();
    if (seed == null) {
      return false;
    }

    world.spawnParticle(Particle.HAPPY_VILLAGER, location, 10, 0.5, 0.25, 0.5);
    world.playSound(location, Sound.ITEM_CROP_PLANT, 1, 1);
    location.getBlock().setType(CropTypes.getCropFromSeed(seed.getType()));
    this.removeSeedDisplay();
    return true;
  }

  public void dropCarriedSeed() {
    Location location = bee.getLocation();
    World world = bee.getWorld();

    ItemStack seed = this.getCarriedSeed();
    if (seed == null) {
      return;
    }

    world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 1, 1);
    world.dropItemNaturally(location, seed);
    this.removeSeedDisplay();
  }

  public void removeSeedDisplay() {
    PersistentDataContainer container = this.bee.getPersistentDataContainer();
    String entityId = container.get(this.beeItemEntityIdKey, PersistentDataType.STRING);
    if (entityId == null) {
      Bukkit.getLogger().info("No entity ID found for item display");
      return;
    }

    Entity entity = this.bee.getWorld().getEntity(UUID.fromString(entityId));
    if (entity == null) {
      Bukkit.getLogger().info("Entity was null: " + entityId);
      return;
    }

    if (!(entity instanceof ItemDisplay item)) {
      Bukkit.getLogger().info("Entity was not item display");
      return;
    }

    item.remove();
    container.remove(this.beeItemEntityIdKey);
  }

  public Bee getBee() {
    return this.bee;
  }
}
