package com.mrjoshuasperry.pocketplugins.modules.dispensery;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.UnaryOperator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class Dispensery extends Module {
  private final List<Material> placeableMaterials;

  public Dispensery(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
    this.placeableMaterials = new ArrayList<>();

    for (String material : readableConfig.getStringList("placeable-materials")) {
      try {
        this.placeableMaterials.add(Material.valueOf(material.toUpperCase().replace(" ", "_")));
      } catch (IllegalArgumentException e) {
        this.getPlugin().getLogger().warning("Invalid dispensery material: " + material);
      }
    }
  }

  /**
   * Changes the stack that was dispensed, a tick later once the cancelled dispense
   * has settled. It's found by matching the dispensed item exactly, so a renamed or
   * enchanted stack is never mistaken for a plain one of the same type.
   */
  protected void runDispenserOperation(Block block, ItemStack dispensed, UnaryOperator<ItemStack> onFound) {
    Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
      // The dispenser may have been broken in the meantime
      if (!(block.getState() instanceof Dispenser dispenser)) {
        return;
      }

      Inventory inventory = dispenser.getSnapshotInventory();
      int index = indexOfSimilar(inventory.getContents(), dispensed);

      if (index != -1) {
        inventory.setItem(index, onFound.apply(inventory.getItem(index)));
        dispenser.update();
      }
    }, 0);
  }

  /** The first slot holding the same item as {@code dispensed}, whatever its count, or -1. */
  static int indexOfSimilar(ItemStack[] contents, ItemStack dispensed) {
    for (int index = 0; index < contents.length; index++) {
      if (contents[index] != null && contents[index].isSimilar(dispensed)) {
        return index;
      }
    }
    return -1;
  }

  /** One fewer of the stack, keeping its name, enchantments and other data. */
  static ItemStack withOneFewer(ItemStack item) {
    return item.getAmount() <= 1 ? null : item.subtract();
  }

  protected void removeItemInDispenser(Block block, ItemStack dispensed) {
    this.runDispenserOperation(block, dispensed, Dispensery::withOneFewer);
  }

  protected void updateItemInDispenser(Block block, ItemStack dispensed, Material newType) {
    this.runDispenserOperation(block, dispensed, (ItemStack item) -> item.withType(newType));
  }

  /**
   * Applies a point of durability to the tool that was dispensed, honoring
   * Unbreaking, and clears the slot with a break sound when it wears out.
   */
  protected void damageToolInDispenser(Block block, ItemStack dispensed) {
    this.runDispenserOperation(block, dispensed, (ItemStack item) -> {
      if (!(item.getItemMeta() instanceof Damageable meta)) {
        return item;
      }

      Random random = this.getPlugin().getRandom();
      if (random.nextFloat() >= 1.0f / (item.getEnchantmentLevel(Enchantment.UNBREAKING) + 1)) {
        return item;
      }

      int damage = meta.getDamage() + 1;
      if (damage >= item.getType().getMaxDurability()) {
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
        return new ItemStack(Material.AIR);
      }

      meta.setDamage(damage);
      item.setItemMeta(meta);
      return item;
    });
  }

  protected static Sound getFillSoundForBucket(Material bucketType) {
    return switch (bucketType) {
      case WATER_BUCKET -> Sound.ITEM_BUCKET_FILL;
      case LAVA_BUCKET -> Sound.ITEM_BUCKET_FILL_LAVA;
      case POWDER_SNOW_BUCKET -> Sound.ITEM_BUCKET_FILL_POWDER_SNOW;
      default -> Sound.ITEM_BUCKET_FILL;
    };
  }

  protected static Sound getEmptySoundForBucket(Material cauldronType) {
    return switch (cauldronType) {
      case WATER_CAULDRON -> Sound.ITEM_BUCKET_EMPTY;
      case LAVA_CAULDRON -> Sound.ITEM_BUCKET_EMPTY_LAVA;
      case POWDER_SNOW_CAULDRON -> Sound.ITEM_BUCKET_EMPTY_POWDER_SNOW;
      default -> Sound.ITEM_BUCKET_EMPTY;
    };
  }

  protected static Material getCauldronTypeForBucket(Material bucketType) {
    return switch (bucketType) {
      case WATER_BUCKET -> Material.WATER_CAULDRON;
      case LAVA_BUCKET -> Material.LAVA_CAULDRON;
      case POWDER_SNOW_BUCKET -> Material.POWDER_SNOW_CAULDRON;
      default -> Material.CAULDRON;
    };
  }

  protected static Material getBucketTypeForCauldron(Material cauldronType) {
    return switch (cauldronType) {
      case WATER_CAULDRON -> Material.WATER_BUCKET;
      case LAVA_CAULDRON -> Material.LAVA_BUCKET;
      case POWDER_SNOW_CAULDRON -> Material.POWDER_SNOW_BUCKET;
      default -> Material.BUCKET;
    };
  }

  protected boolean isFilledCauldron(Block cauldron) {
    Material type = cauldron.getType();

    if (type != Material.WATER_CAULDRON
        && type != Material.LAVA_CAULDRON
        && type != Material.POWDER_SNOW_CAULDRON) {
      return false;
    }

    BlockData data = cauldron.getBlockData();
    if (data instanceof Levelled levelled) {
      return levelled.getLevel() == levelled.getMaximumLevel();
    }

    return true;
  }

  @EventHandler
  public void onBlockDispense(BlockDispenseEvent event) {
    ItemStack item = event.getItem();
    if (item == null)
      return;

    Block block = event.getBlock();
    if (block.getType() != Material.DISPENSER)
      return;

    Directional directional = (Directional) block.getBlockData();
    Block relative = block.getRelative(directional.getFacing());
    Material itemType = item.getType();
    Material relativeType = relative.getType();

    if (placeableMaterials.contains(itemType) && relativeType.isAir()) {
      relative.setType(itemType);
      relative.getWorld().playSound(relative.getLocation(), relative.getBlockSoundGroup().getPlaceSound(), 1, 1);

      event.setCancelled(true);
      this.removeItemInDispenser(block, item);
      return;
    }

    if (itemType == Material.SHEARS && relativeType == Material.PUMPKIN) {
      BlockData carvedData = Material.CARVED_PUMPKIN.createBlockData();
      if (carvedData instanceof Directional carvedDirectional) {
        carvedDirectional.setFacing(directional.getFacing().getOppositeFace());
      }
      relative.setBlockData(carvedData);
      relative.getWorld().dropItemNaturally(relative.getLocation().add(0.5, 0.5, 0.5),
          new ItemStack(Material.PUMPKIN_SEEDS, 4));
      relative.getWorld().playSound(relative.getLocation(), Sound.BLOCK_PUMPKIN_CARVE, 1, 1);

      event.setCancelled(true);
      this.damageToolInDispenser(block, item);
      return;
    }

    if (!relative.getType().toString().contains("CAULDRON")) {
      return;
    }

    switch (itemType) {
      case WATER_BUCKET, LAVA_BUCKET, POWDER_SNOW_BUCKET -> {
        // Only an empty cauldron takes a bucket, as for a player; filling a full one
        // would pour away what was in it, so the bucket stays put
        if (relativeType != Material.CAULDRON) {
          event.setCancelled(true);
          return;
        }

        relative.getWorld().playSound(relative.getLocation(), getFillSoundForBucket(itemType), 1, 1);
        relative.setType(getCauldronTypeForBucket(itemType));

        BlockData data = relative.getBlockData();
        if (data instanceof Levelled levelled) {
          levelled.setLevel(levelled.getMaximumLevel());
          relative.setBlockData(data);
        }

        event.setCancelled(true);
        this.updateItemInDispenser(block, item, Material.BUCKET);
      }
      case BUCKET -> {
        if (this.isFilledCauldron(relative)) {
          relative.getWorld().playSound(relative.getLocation(), getEmptySoundForBucket(relative.getType()), 1, 1);
          relative.setType(Material.CAULDRON);

          event.setCancelled(true);
          this.updateItemInDispenser(block, item, getBucketTypeForCauldron(relativeType));
        }
      }
      default -> {
        // Other items do nothing
      }
    }
  }
}
