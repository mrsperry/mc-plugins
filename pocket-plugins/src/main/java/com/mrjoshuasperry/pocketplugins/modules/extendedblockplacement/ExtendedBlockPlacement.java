package com.mrjoshuasperry.pocketplugins.modules.extendedblockplacement;

import org.bukkit.Bukkit;
import org.bukkit.Fluid;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.SoundGroup;
import org.bukkit.Statistic;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/**
 * Lets blocks go where vanilla refuses them for no visible reason.
 *
 * <p>
 * Lanterns hang from the underside of leaves: click the bottom of a leaf block
 * with one. It falls off and drops when the leaves are broken or decay, as it
 * would from any other block.
 */
public class ExtendedBlockPlacement extends Module {
  protected boolean lanternsUnderLeaves;

  public ExtendedBlockPlacement(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.lanternsUnderLeaves = readableConfig.getBoolean("lanterns-under-leaves", true);
  }

  /**
   * Leaves offer nothing to hang from as far as the game is concerned, so it
   * refuses the click. Place the lantern the way vanilla does under any other
   * block.
   */
  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!this.lanternsUnderLeaves || event.getAction() != Action.RIGHT_CLICK_BLOCK
        || event.getBlockFace() != BlockFace.DOWN) {
      return;
    }

    Block leaves = event.getClickedBlock();
    ItemStack item = event.getItem();
    EquipmentSlot hand = event.getHand();
    if (leaves == null || item == null || hand == null || !Tag.LEAVES.isTagged(leaves.getType())
        || !(blockDataOf(item.getType()) instanceof Lantern lantern)) {
      return;
    }

    Player player = event.getPlayer();
    if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
      return;
    }

    // Under a solid block a click on the underside only ever hangs the lantern.
    // Vanilla would otherwise stand it on the ground below the leaves
    event.setCancelled(true);

    Block target = leaves.getRelative(BlockFace.DOWN);
    if (target.getY() < target.getWorld().getMinHeight() || !target.isReplaceable()) {
      return;
    }

    lantern.setHanging(true);
    lantern.setWaterlogged(target.getWorld().getFluidData(target.getLocation()).getFluidType() == Fluid.WATER);
    if (isObstructed(target, lantern)) {
      return;
    }

    this.place(player, hand, item, target, leaves, lantern);
  }

  /**
   * The game checks a lantern's support whenever the block above it changes,
   * and leaves change by themselves as their distance from a log is worked out.
   * Keep the lantern for as long as there are leaves to hang from.
   */
  @EventHandler(ignoreCancelled = true)
  public void onBlockDestroy(BlockDestroyEvent event) {
    Block block = event.getBlock();
    if (this.lanternsUnderLeaves && block.getBlockData() instanceof Lantern lantern
        && hangsFromLeaves(lantern.isHanging(), block.getRelative(BlockFace.UP).getType())) {
      event.setCancelled(true);
    }
  }

  /**
   * Places the block the way a player placing it would: protection plugins get
   * their say through the place event, then it makes its sound, swings the
   * hand and uses up the item.
   */
  protected void place(Player player, EquipmentSlot hand, ItemStack item, Block target, Block against,
      BlockData data) {
    BlockState replaced = target.getState();
    target.setBlockData(data);

    BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, replaced, against, item, player,
        canBuild(player, target), hand);
    if (!placeEvent.callEvent() || !placeEvent.canBuild()) {
      replaced.update(true);
      return;
    }

    World world = target.getWorld();
    Location center = target.getLocation().toCenterLocation();
    SoundGroup sounds = data.getSoundGroup();
    world.playSound(center, sounds.getPlaceSound(), SoundCategory.BLOCKS, (sounds.getVolume() + 1) / 2,
        sounds.getPitch() * 0.8f);
    world.sendGameEvent(player, GameEvent.BLOCK_PLACE, center.toVector());

    player.incrementStatistic(Statistic.USE_ITEM, item.getType());
    // The client thinks the click failed, so it won't swing on its own
    player.swingHand(hand);
    if (player.getGameMode() != GameMode.CREATIVE) {
      item.subtract();
    }
  }

  protected static boolean hangsFromLeaves(boolean hanging, Material above) {
    return hanging && Tag.LEAVES.isTagged(above);
  }

  /** Null for anything that isn't a block. */
  protected static BlockData blockDataOf(Material material) {
    return material.isBlock() ? material.createBlockData() : null;
  }

  /** Vanilla won't place a block into a player, mob or vehicle standing in its way. */
  protected static boolean isObstructed(Block target, BlockData data) {
    Location origin = target.getLocation();
    for (BoundingBox box : data.getCollisionShape(origin).getBoundingBoxes()) {
      BoundingBox placed = box.clone().shift(origin);
      if (!target.getWorld().getNearbyEntities(placed, ExtendedBlockPlacement::blocksBuilding).isEmpty()) {
        return true;
      }
    }

    return false;
  }

  protected static boolean blocksBuilding(Entity entity) {
    if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
      return false;
    }

    return entity instanceof LivingEntity || entity instanceof Vehicle || entity instanceof FallingBlock
        || entity instanceof TNTPrimed || entity instanceof EnderCrystal;
  }

  /**
   * Spawn protection, worked out as the server does for its own place events:
   * it guards only the main world, and only once there are operators, who are
   * exempt from it.
   */
  protected static boolean canBuild(Player player, Block block) {
    int radius = Bukkit.getSpawnRadius();
    World world = block.getWorld();
    if (radius <= 0 || player.isOp() || Bukkit.getOperators().isEmpty()
        || !world.equals(Bukkit.getWorlds().getFirst())) {
      return true;
    }

    Location spawn = world.getSpawnLocation();
    return Math.max(Math.abs(block.getX() - spawn.getBlockX()), Math.abs(block.getZ() - spawn.getBlockZ())) > radius;
  }
}
