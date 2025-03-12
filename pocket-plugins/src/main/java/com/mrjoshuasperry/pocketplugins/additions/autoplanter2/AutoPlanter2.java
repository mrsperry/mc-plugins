package com.mrjoshuasperry.pocketplugins.additions.autoplanter2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.Pathfinder.PathResult;
import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.classes.Pair;
import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;
import com.mrjoshuasperry.pocketplugins.utils.PathfinderUtil;

// Todo:
// bees carrying the item below them
// hopper support for beehives
// drop seeds when bees die

public class AutoPlanter2 extends Module {
  protected JavaPlugin plugin;
  protected Random random;

  protected int beeCooldown;
  protected int hiveRadius;

  protected Set<Location> reservedLocations;

  public AutoPlanter2() {
    super("AutoPlanter2");

    this.plugin = PocketPlugins.getInstance();
    this.random = new Random();

    this.reservedLocations = new HashSet<>();
  }

  @Override
  public void init(YamlConfiguration config) {
    super.init(config);

    this.beeCooldown = Math.max(config.getInt("bee-cooldown", 1000), 100);
    this.hiveRadius = config.getInt("hive-radius", 4);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().isRightClick()) {
      return;
    }

    if (event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }

    Block block = event.getClickedBlock();

    if (block == null) {
      return;
    }

    Material blockType = block.getType();

    if (blockType != Material.BEEHIVE && blockType != Material.BEE_NEST) {
      return;
    }

    Player player = event.getPlayer();

    if (player.isSneaking()) {
      return;
    }

    Beehive hive = (Beehive) block.getState();
    org.bukkit.block.data.type.Beehive beehiveData = (org.bukkit.block.data.type.Beehive) hive.getBlockData();

    if (beehiveData.getHoneyLevel() > 0) {
      PlayerInventory inventory = player.getInventory();
      Material mainHandType = inventory.getItemInMainHand().getType();
      Material offHandType = inventory.getItemInOffHand().getType();

      if (mainHandType == Material.SHEARS || mainHandType == Material.GLASS_BOTTLE || offHandType == Material.SHEARS ||
          offHandType == Material.GLASS_BOTTLE) {
        return;
      }
    }

    HiveInventory inventory = new HiveInventory(hive);
    player.openInventory(inventory.getInventory());
    event.setCancelled(true);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder(false) instanceof HiveInventory hiveInventory)) {
      return;
    }

    ((HiveInventory) hiveInventory).save();
  }

  @EventHandler
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getSpawnReason() != SpawnReason.BEEHIVE) {
      return;
    }

    Bee bee = (Bee) event.getEntity();

    if (bee.isAggressive()) {
      Bukkit.getLogger().info("Bee is aggro, cancelling");
      return;
    }

    Beehive hive = (Beehive) bee.getHive().getBlock().getState();
    this.runPlantingCycle(bee, hive);
  }

  protected void runPlantingCycle(Bee bee, Beehive hive) {
    Bukkit.getLogger().info("Reserved locs: " + reservedLocations.size());
    PlanterBee planter = new PlanterBee(bee, hive);

    long cooldown = planter.getCooldown(this.beeCooldown);
    if (cooldown < this.beeCooldown) {
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> runPlantingCycle(bee, hive),
          (long) (Math.ceil((this.beeCooldown - cooldown) / 1000f)) * 20);
      return;
    }

    HiveInventory inventory = new HiveInventory(hive);
    List<ItemStack> availableSeeds = new ArrayList<>();

    for (ItemStack item : inventory.getInventory().getContents()) {
      if (item == null) {
        continue;
      }

      Material type = item.getType();
      if (CropTypes.getSeedTypes().contains(type)) {
        availableSeeds.add(item);
      }
    }

    if (availableSeeds.isEmpty()) {
      Bukkit.getLogger().info("No seeds found in beehive");
      return;
    }

    Location hiveLocation = hive.getLocation();
    Location returnLocation = this.getHiveReturnLocation(bee, hive);
    if (returnLocation == null) {
      Bukkit.getLogger().info("Cannot find a return location");
      return;
    }

    Pathfinder pathfinder = bee.getPathfinder();
    List<Location> availableLocations = this.getAvailableFarmland(pathfinder, hiveLocation, this.hiveRadius);

    if (availableLocations.isEmpty()) {
      Bukkit.getLogger().info("No farmland found");
      return;
    }

    Location farmland = availableLocations.get(this.random.nextInt(availableLocations.size()));
    this.reservedLocations.add(farmland.getBlock().getLocation());

    PathfinderUtil pathfinderUtil = new PathfinderUtil();
    pathfinderUtil.pathToMultiple(bee, 1, Lists.newArrayList(
        new Pair<>(returnLocation, (Boolean success) -> this.movedToHive(planter, success)),
        new Pair<>(farmland, (Boolean success) -> this.movedToFarmland(farmland, planter, hive, success)),
        new Pair<>(returnLocation,
            (Boolean success) -> this.finishPlantingCycle(planter, hive, success))),
        () -> this.runPlantingCycle(bee, hive));
  }

  protected boolean movedToHive(PlanterBee bee, Boolean success) {
    if (!success) {
      bee.playAngryState();
      return false;
    }

    return bee.grabSeedFromHive();
  }

  protected boolean movedToFarmland(Location location, PlanterBee bee, Beehive hive, Boolean success) {
    this.reservedLocations.remove(location.getBlock().getLocation());

    if (!success) {
      bee.playAngryState();
      bee.dropCarriedSeed();
      return false;
    }

    return bee.plantSeed(location);
  }

  protected boolean finishPlantingCycle(PlanterBee bee, Beehive hive, boolean success) {
    if (!success) {
      bee.playAngryState();
      return false;
    }

    Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.runPlantingCycle(bee.getBee(), hive), 0);
    return true;
  }

  protected Location getHiveReturnLocation(Bee bee, Beehive hive) {
    List<Location> locations = new ArrayList<>();

    Block block = hive.getBlock();
    List<BlockFace> faces = Lists.newArrayList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
        BlockFace.UP, BlockFace.DOWN);

    for (BlockFace face : faces) {
      Block relative = block.getRelative(face);
      Material type = relative.getType();

      if (type.isCollidable() || type.isSolid()) {
        continue;
      }

      if (type == Material.WATER || type == Material.LAVA) {
        continue;
      }

      locations.add(relative.getLocation().clone().add(0.5, 0.5, 0.5));
    }

    Pathfinder pathfinder = bee.getPathfinder();
    Location beeLocation = bee.getLocation();

    locations.stream().filter((Location location) -> pathfinder.findPath(location).canReachFinalPoint()).close();
    locations.sort(
        (Location previous, Location next) -> previous.distance(beeLocation) < next.distance(beeLocation) ? -1 : 1);

    return locations.get(0);
  }

  protected List<Location> getAvailableFarmland(Pathfinder pathfinder, Location center, int radius) {
    List<Location> availableLocations = new ArrayList<>();

    for (int x = -radius; x <= radius; x++) {
      for (int y = -radius; y <= radius; y++) {
        for (int z = -radius; z <= radius; z++) {
          Block block = center.clone().add(x, y, z).getBlock();

          if (block.getType() != Material.FARMLAND) {
            continue;
          }

          Block above = block.getRelative(BlockFace.UP);
          Material aboveType = above.getType();
          if (!aboveType.isAir()) {
            continue;
          }

          Location aboveLocation = above.getLocation();
          Location destination = above.getLocation().add(
              this.random.nextDouble(0.25, 0.75),
              0.5,
              this.random.nextDouble(0.25, 0.75));

          PathResult path = pathfinder.findPath(destination);
          if (!path.canReachFinalPoint()) {
            continue;
          }

          if (this.reservedLocations.contains(aboveLocation)) {
            continue;
          }

          availableLocations.add(destination);
        }
      }
    }

    return availableLocations;
  }
}
