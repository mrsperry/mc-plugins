package com.mrjoshuasperry.pocketplugins.additions.autoplanter;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class AutoPlanter extends Module {
  protected JavaPlugin plugin;
  protected ArrayList<Item> seedsToPlant;
  protected Random random;

  protected double minDelay;
  protected double maxDelay;

  public AutoPlanter() {
    super("AutoPlanter");

    this.plugin = PocketPlugins.getInstance();
    this.seedsToPlant = new ArrayList<>();
    this.random = new Random();
  }

  @Override
  public void init(YamlConfiguration config) {
    super.init(config);

    this.minDelay = config.getDouble("min-delay", 0.25f);
    this.maxDelay = config.getDouble("max-delay", 2f);

    this.runNewPlantingCycle(0);
  }

  protected void runNewPlantingCycle(double delay) {
    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
      this.findSeedsToPlant();

      boolean didPlantSeed = false;
      do {
        if (this.seedsToPlant.isEmpty()) {
          break;
        }

        int index = this.random.nextInt(this.seedsToPlant.size());
        didPlantSeed = this.plantSeed(this.seedsToPlant.get(index));
        this.seedsToPlant.remove(index);

      } while (!didPlantSeed);

      this.runNewPlantingCycle(this.random.nextDouble(this.minDelay, this.maxDelay));
    }, Math.round(delay * 20));
  }

  protected void findSeedsToPlant() {
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntitiesByClass(Item.class)) {
        Item item = (Item) entity;

        if (!CropTypes.getSeedTypes().contains(item.getItemStack().getType())) {
          continue;
        }

        if (!item.isValid()) {
          continue;
        }

        this.seedsToPlant.add(item);
      }
    }
  }

  protected boolean plantSeed(Item seed) {
    if (!seed.isValid()) {
      return false;
    }

    Block block = seed.getLocation().getBlock();
    if (block.getType() != Material.FARMLAND) {
      return false;
    }

    Block above = block.getRelative(BlockFace.UP);
    Material aboveType = above.getType();
    if (aboveType != Material.AIR && aboveType != Material.CAVE_AIR) {
      return false;
    }

    ItemStack stack = seed.getItemStack();
    above.setType(CropTypes.getCropFromSeed(stack.getType()));
    stack.setAmount(stack.getAmount() - 1);

    return true;
  }
}
