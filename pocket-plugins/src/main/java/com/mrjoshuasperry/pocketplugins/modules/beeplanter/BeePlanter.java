package com.mrjoshuasperry.pocketplugins.modules.beeplanter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.classes.Pair;
import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.pocketplugins.utils.Module;
import com.mrjoshuasperry.pocketplugins.utils.PathfinderUtil;

import io.papermc.paper.event.entity.EntityMoveEvent;

public class BeePlanter extends Module {
  protected Random random;

  protected int searchRadius;

  protected Set<Bee> plantingBees;
  protected Set<Location> reservedFarmland;

  public BeePlanter() {
    super("BeePlanter");

    this.random = new Random();

    this.plantingBees = new HashSet<>();
    this.reservedFarmland = new HashSet<>();
  }

  @Override
  public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super.initialize(readableConfig, writableConfig);

    this.searchRadius = readableConfig.getInt("search-radius", 4);
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    this.cleanupBee(event.getEntity());
  }

  @EventHandler
  public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
    this.cleanupBee(event.getEntity());
  }

  @EventHandler
  public void onEntityMove(EntityMoveEvent event) {
    if (event.getEntityType() != EntityType.BEE) {
      return;
    }

    Bee bee = (Bee) event.getEntity();

    if (this.plantingBees.contains(bee)) {
      return;
    }

    List<Item> seeds = new ArrayList<>();
    List<Material> seedTypes = CropTypes.getSeedTypes();

    for (Entity entity : bee.getNearbyEntities(this.searchRadius, this.searchRadius, this.searchRadius)) {
      if (entity.getType() != EntityType.ITEM) {
        continue;
      }

      Item item = (Item) entity;

      if (!seedTypes.contains(item.getItemStack().getType())) {
        continue;
      }

      seeds.add(item);
    }

    if (seeds.isEmpty()) {
      return;
    }

    Item seed = seeds.get(this.random.nextInt(seeds.size()));
    Location seedLocation = seed.getLocation();

    List<Location> availableFarmland = this.getAvailableFarmland(seedLocation, this.searchRadius);
    if (availableFarmland.isEmpty()) {
      return;
    }

    Location farmland = availableFarmland.get(this.random.nextInt(availableFarmland.size()));
    this.reservedFarmland.add(farmland);

    PlanterBee planter = new PlanterBee(this, bee);
    this.plantingBees.add(bee);

    PathfinderUtil pathfinder = new PathfinderUtil();
    pathfinder.pathToMultiple(bee, 1, Lists.newArrayList(
        new Pair<>(seedLocation, (Boolean success) -> this.movedToSeed(planter, farmland, seed, success)),
        new Pair<>(farmland.clone().add(0.5, 1.5, 0.5),
            (Boolean success) -> this.movedToFarmland(planter, farmland, success))),
        () -> this.failedToPath(planter, farmland));
  }

  protected void failedToPath(PlanterBee bee, Location farmland) {
    this.plantingBees.remove(bee.getBee());
    this.reservedFarmland.remove(farmland);
    bee.playAngryState();
    bee.dropCarriedSeed();
  }

  protected boolean movedToSeed(PlanterBee bee, Location farmland, Item item, Boolean success) {
    if (!success) {
      this.failedToPath(bee, farmland);
      return false;
    }

    if (!item.isValid()) {
      this.failedToPath(bee, farmland);
      return false;
    }

    ItemStack stack = item.getItemStack();
    Material type = stack.getType();
    if (type.isAir()) {
      this.failedToPath(bee, farmland);
      return false;
    }

    bee.pickupSeed(new ItemStack(type));
    stack.setAmount(stack.getAmount() - 1);
    return true;
  }

  protected boolean movedToFarmland(PlanterBee bee, Location location, Boolean success) {
    if (!success) {
      this.failedToPath(bee, location);
      return false;
    }

    if (!this.isValidFarmland(location)) {
      this.failedToPath(bee, location);
      return false;
    }

    bee.plantCarriedSeed(location.clone().add(0, 1, 0));
    this.plantingBees.remove(bee.getBee());
    this.reservedFarmland.remove(location);
    return true;
  }

  protected boolean isValidFarmland(Location farmland) {
    Block block = farmland.getBlock();

    if (block.getType() != Material.FARMLAND) {
      return false;
    }

    Block above = block.getRelative(BlockFace.UP);
    Material aboveType = above.getType();
    if (!aboveType.isAir()) {
      return false;
    }

    return true;
  }

  protected List<Location> getAvailableFarmland(Location center, int radius) {
    List<Location> availableLocations = new ArrayList<>();

    for (int x = -radius; x <= radius; x++) {
      for (int y = -radius; y <= radius; y++) {
        for (int z = -radius; z <= radius; z++) {
          Location block = center.clone().add(x, y, z);

          if (!isValidFarmland(block)) {
            continue;
          }

          if (this.reservedFarmland.contains(block)) {
            continue;
          }

          availableLocations.add(block);
        }
      }
    }

    return availableLocations;
  }

  protected void cleanupBee(Entity entity) {
    if (entity.getType() != EntityType.BEE) {
      return;
    }

    Bee bee = (Bee) entity;
    PlanterBee planter = new PlanterBee(this, bee);
    planter.dropCarriedSeed();

    this.plantingBees.remove(bee);
  }
}
