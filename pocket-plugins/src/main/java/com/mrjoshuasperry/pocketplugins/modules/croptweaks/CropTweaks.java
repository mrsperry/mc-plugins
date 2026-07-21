package com.mrjoshuasperry.pocketplugins.modules.croptweaks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.mcutils.types.ToolTypes;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class CropTweaks extends Module {
  private static final int SUGAR_CANE_MAX_HEIGHT = 3;
  private static final int CACTUS_MAX_HEIGHT = 3;

  private final boolean canBlazePowderNetherWart;
  private final boolean canBonemealSugarcane;
  private final boolean canBonemealCactus;
  private final boolean canBonemealVines;
  private final boolean canBonemealStems;

  public CropTweaks(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.canBlazePowderNetherWart = readableConfig.getBoolean("can-blaze-powder-nether-wart", true);
    this.canBonemealSugarcane = readableConfig.getBoolean("can-bonemeal-sugarcane", true);
    this.canBonemealCactus = readableConfig.getBoolean("can-bonemeal-cactus", true);
    this.canBonemealVines = readableConfig.getBoolean("can-bonemeal-vines", true);
    this.canBonemealStems = readableConfig.getBoolean("can-bonemeal-stems", true);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }

    ItemStack item = event.getItem();
    if (item == null) {
      return;
    }

    boolean isCreative = event.getPlayer().getGameMode() == GameMode.CREATIVE;

    tryHarvestWithHoe(block, item, isCreative);

    if (this.canBlazePowderNetherWart) {
      tryBlazePowderNetherWart(block, item, isCreative);
    }

    // Bone meal is normally inert on these blocks, so nothing vanilla would have
    // done is lost by cancelling — it just stops the interaction firing a second
    // time for the off hand and double-spending the stack.
    if (tryBonemeal(block, item, isCreative)) {
      event.setCancelled(true);
    }
  }

  private void tryHarvestWithHoe(Block block, ItemStack tool, boolean isCreative) {
    Material cropType = block.getType();
    if (!CropTypes.getHarvestableTypes().contains(cropType)) {
      return;
    }

    if (!ToolTypes.getHoeTypes().contains(tool.getType())) {
      return;
    }

    Ageable data = (Ageable) block.getBlockData();
    if (data.getAge() != data.getMaximumAge()) {
      return;
    }

    Material cropToDrop;
    Sound soundToPlay = Sound.BLOCK_CROP_BREAK;

    switch (cropType) {
      case NETHER_WART -> {
        cropToDrop = Material.NETHER_WART;
        soundToPlay = Sound.BLOCK_NETHER_WART_BREAK;
      }
      case WHEAT -> cropToDrop = Material.WHEAT;
      case CARROTS -> cropToDrop = Material.CARROT;
      case POTATOES -> cropToDrop = Material.POTATO;
      case BEETROOTS -> cropToDrop = Material.BEETROOT;
      default -> {
        return;
      }
    }

    // Ask the block for its vanilla loot given the hoe: this is where Fortune is
    // applied. Read it while the crop is still mature — resetting the age first
    // would yield the immature drop table. Keep only the produce item so the
    // free replant doesn't also hand out a windfall of seeds.
    int amountToDrop = block.getDrops(tool).stream()
        .filter(drop -> drop.getType() == cropToDrop)
        .mapToInt(ItemStack::getAmount)
        .sum();

    // The crop is replanted in place at no cost, so reserve one of the derived
    // drop to stand in for the seed a manual break-and-replant would have spent.
    // Floored at one so a harvest is never empty — that floor also leaves wheat
    // and beetroot, whose vanilla produce is a single item, dropping that item.
    amountToDrop = Math.max(1, amountToDrop - 1);

    data.setAge(0);
    block.setBlockData(data);

    Location location = block.getLocation();
    World world = location.getWorld();
    Random random = PocketPlugins.getInstance().getRandom();

    // TODO: move this natural damage logic to mc-utils
    if (!isCreative) {
      Damageable meta = (Damageable) tool.getItemMeta();
      if (random.nextFloat() < 1.0f / (tool.getEnchantmentLevel(Enchantment.UNBREAKING) + 1)) {
        meta.setDamage(meta.getDamage() + 1);
        tool.setItemMeta(meta);
      }
    }

    if (amountToDrop > 0) {
      world.dropItemNaturally(location, new ItemStack(cropToDrop, amountToDrop));
      world.playSound(location, soundToPlay, 1, 1);
    }
  }

  private void tryBlazePowderNetherWart(Block block, ItemStack item, boolean isCreative) {
    if (item.getType() != Material.BLAZE_POWDER) {
      return;
    }

    if (block.getType() != Material.NETHER_WART) {
      return;
    }

    Ageable data = (Ageable) block.getBlockData();
    if (data.getAge() == data.getMaximumAge()) {
      return;
    }

    data.setAge(data.getAge() + 1);
    block.setBlockData(data);
    block.getWorld().spawnParticle(Particle.ASH, block.getLocation().add(0.5f, 0.75f, 0.5f), 20, 0.2f, 0.2f, 0.2f, 3);
    block.getWorld().playSound(block.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1);

    if (!isCreative) {
      item.setAmount(item.getAmount() - 1);
    }
  }

  /**
   * Lets bone meal act on plants vanilla leaves inert. Returns whether anything
   * grew, so the caller only spends the stack and cancels the interaction on a
   * hit.
   */
  private boolean tryBonemeal(Block block, ItemStack item, boolean isCreative) {
    if (item.getType() != Material.BONE_MEAL) {
      return false;
    }

    boolean grew = switch (block.getType()) {
      case SUGAR_CANE -> this.canBonemealSugarcane && growColumn(block, Material.SUGAR_CANE, SUGAR_CANE_MAX_HEIGHT);
      case CACTUS -> this.canBonemealCactus && growCactus(block);
      case VINE -> this.canBonemealVines && growVinesDownward(block);
      case PUMPKIN_STEM, MELON_STEM -> this.canBonemealStems && growStemFruit(block);
      default -> false;
    };

    if (grew && !isCreative) {
      item.setAmount(item.getAmount() - 1);
    }

    return grew;
  }

  /**
   * Fills a vertical column of {@code type} (sugar cane, cactus) up to
   * {@code maxHeight} tall, stacking new blocks on the exposed top. Returns
   * whether at least one block was added.
   */
  private boolean growColumn(Block block, Material type, int maxHeight) {
    Block base = block;
    while (base.getRelative(BlockFace.DOWN).getType() == type) {
      base = base.getRelative(BlockFace.DOWN);
    }

    Block top = base;
    int height = 1;
    while (top.getRelative(BlockFace.UP).getType() == type) {
      top = top.getRelative(BlockFace.UP);
      height++;
    }

    boolean grew = false;
    while (height < maxHeight) {
      Block above = top.getRelative(BlockFace.UP);

      // Stop at the first obstruction. A cactus also pops off next to a solid
      // block, so don't grow one into a spot vanilla would immediately reject;
      // sugar cane has no such neighbour rule.
      boolean blocked = !above.getType().isAir()
          || (type == Material.CACTUS && hasSolidHorizontalNeighbor(above));
      if (blocked) {
        break;
      }

      above.setType(type);
      top = above;
      height++;
      grew = true;
    }

    if (grew) {
      spawnBonemealEffect(top);
    }

    return grew;
  }

  private boolean hasSolidHorizontalNeighbor(Block block) {
    for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
      if (block.getRelative(face).getType().isSolid()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Grows a cactus toward its full height first; once it is already maxed out, a
   * further application crowns the top with a cactus flower.
   */
  private boolean growCactus(Block block) {
    if (growColumn(block, Material.CACTUS, CACTUS_MAX_HEIGHT)) {
      return true;
    }

    Block top = block;
    while (top.getRelative(BlockFace.UP).getType() == Material.CACTUS) {
      top = top.getRelative(BlockFace.UP);
    }

    Block above = top.getRelative(BlockFace.UP);
    if (!above.getType().isAir()) {
      return false;
    }

    above.setType(Material.CACTUS_FLOWER);
    spawnBonemealEffect(above);
    return true;
  }

  /**
   * Extends a vine straight down, copying its attachment faces, until it meets a
   * block or the bottom of the world.
   */
  private boolean growVinesDownward(Block block) {
    BlockData data = block.getBlockData();
    int minHeight = block.getWorld().getMinHeight();

    boolean grew = false;
    Block below = block.getRelative(BlockFace.DOWN);
    while (below.getY() >= minHeight && below.getType().isAir()) {
      below.setBlockData(data.clone(), false);
      grew = true;
      below = below.getRelative(BlockFace.DOWN);
    }

    if (grew) {
      spawnBonemealEffect(block);
    }

    return grew;
  }

  /**
   * A fully grown stem spawns its fruit on a free, soil-backed neighbour and
   * turns to face it. Immature stems are left alone so vanilla bone meal keeps
   * advancing their age as usual.
   */
  private boolean growStemFruit(Block block) {
    Ageable data = (Ageable) block.getBlockData();
    if (data.getAge() != data.getMaximumAge()) {
      return false;
    }

    boolean pumpkin = block.getType() == Material.PUMPKIN_STEM;
    Material fruit = pumpkin ? Material.PUMPKIN : Material.MELON;
    Material attachedStem = pumpkin ? Material.ATTACHED_PUMPKIN_STEM : Material.ATTACHED_MELON_STEM;

    List<BlockFace> faces = new ArrayList<>(
        List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));
    Collections.shuffle(faces, PocketPlugins.getInstance().getRandom());

    for (BlockFace face : faces) {
      Block target = block.getRelative(face);
      if (!target.getType().isAir()) {
        continue;
      }

      Material soil = target.getRelative(BlockFace.DOWN).getType();
      if (!Tag.DIRT.isTagged(soil) && soil != Material.FARMLAND) {
        continue;
      }

      target.setType(fruit);

      Directional stem = (Directional) attachedStem.createBlockData();
      stem.setFacing(face);
      block.setBlockData(stem);

      spawnBonemealEffect(target);
      return true;
    }

    return false;
  }

  private void spawnBonemealEffect(Block block) {
    Location location = block.getLocation().add(0.5, 0.5, 0.5);
    block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 15, 0.3, 0.3, 0.3, 0);
    block.getWorld().playSound(block.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1, 1);
  }
}
