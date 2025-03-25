package com.mrjoshuasperry.pocketplugins.modules.dispensery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

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

  protected void runDispenserOperation(Block block, Material materialToFind,
      Function<ItemStack, ItemStack> onMaterialFound) {
    Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
      Dispenser dispenser = (Dispenser) block.getState();
      Inventory inventory = dispenser.getSnapshotInventory();
      int index = inventory.first(materialToFind);

      if (index != -1) {
        inventory.setItem(index, onMaterialFound.apply(inventory.getItem(index)));
        dispenser.update();
      }
    }, 0);
  }

  protected void removeItemInDispenser(Block block, Material material) {
    this.runDispenserOperation(block, material,
        (ItemStack item) -> {
          int amount = item.getAmount() - 1;
          if (amount == 0) {
            return new ItemStack(Material.AIR);
          }

          return new ItemStack(item.getType(), item.getAmount() - 1);
        });
  }

  protected void updateItemInDispenser(Block block, Material oldType, Material newType) {
    this.runDispenserOperation(block, oldType, (ItemStack item) -> new ItemStack(newType, item.getAmount()));
  }

  protected Sound getFillSoundForBucket(Material bucketType) {
    return switch (bucketType) {
      case WATER_BUCKET -> Sound.ITEM_BUCKET_FILL;
      case LAVA_BUCKET -> Sound.ITEM_BUCKET_FILL_LAVA;
      case POWDER_SNOW_BUCKET -> Sound.ITEM_BUCKET_FILL_POWDER_SNOW;
      default -> Sound.ITEM_BUCKET_FILL;
    };
  }

  protected Sound getEmptySoundForBucket(Material cauldronType) {
    return switch (cauldronType) {
      case WATER_CAULDRON -> Sound.ITEM_BUCKET_EMPTY;
      case LAVA_CAULDRON -> Sound.ITEM_BUCKET_EMPTY_LAVA;
      case POWDER_SNOW_CAULDRON -> Sound.ITEM_BUCKET_EMPTY_POWDER_SNOW;
      default -> Sound.ITEM_BUCKET_EMPTY;
    };
  }

  protected Material getCauldronTypeForBucket(Material bucketType) {
    return switch (bucketType) {
      case WATER_BUCKET -> Material.WATER_CAULDRON;
      case LAVA_BUCKET -> Material.LAVA_CAULDRON;
      case POWDER_SNOW_BUCKET -> Material.POWDER_SNOW_CAULDRON;
      default -> Material.CAULDRON;
    };
  }

  protected Material getBucketTypeForCauldron(Material cauldronType) {
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
      this.removeItemInDispenser(block, itemType);
      return;
    }

    if (!relative.getType().toString().contains("CAULDRON")) {
      return;
    }

    switch (itemType) {
      case WATER_BUCKET, LAVA_BUCKET, POWDER_SNOW_BUCKET -> {
        relative.getWorld().playSound(relative.getLocation(), this.getFillSoundForBucket(itemType), 1, 1);
        relative.setType(this.getCauldronTypeForBucket(itemType));

        BlockData data = relative.getBlockData();
        if (data instanceof Levelled levelled) {
          levelled.setLevel(levelled.getMaximumLevel());
          relative.setBlockData(data);
        }

        event.setCancelled(true);
        this.updateItemInDispenser(block, itemType, Material.BUCKET);
      }
      case BUCKET -> {
        if (this.isFilledCauldron(relative)) {
          relative.getWorld().playSound(relative.getLocation(), this.getEmptySoundForBucket(relative.getType()), 1, 1);
          relative.setType(Material.CAULDRON);

          event.setCancelled(true);
          this.updateItemInDispenser(block, itemType, this.getBucketTypeForCauldron(relativeType));
        }
      }
      default -> {
        break;
      }
    }
  }
}
