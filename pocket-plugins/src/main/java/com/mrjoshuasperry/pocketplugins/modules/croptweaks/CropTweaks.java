package com.mrjoshuasperry.pocketplugins.modules.croptweaks;

import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
  private final boolean canBlazePowderNetherWart;

  public CropTweaks(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.canBlazePowderNetherWart = readableConfig.getBoolean("can-blaze-powder-nether-wart", true);
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
      tryBonemealEquivalent(block, item, isCreative);
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

    Material cropToDrop = null;
    int amountToDrop = random.nextInt(2) + 1;
    Sound soundToPlay = Sound.BLOCK_CROP_BREAK;

    switch (cropType) {
      case NETHER_WART -> {
        cropToDrop = Material.NETHER_WART;
        soundToPlay = Sound.BLOCK_NETHER_WART_BREAK;
      }
      case WHEAT -> {
        cropToDrop = Material.WHEAT;
        amountToDrop = 1;
      }
      case CARROTS -> cropToDrop = Material.CARROT;
      case POTATOES -> cropToDrop = Material.POTATO;
      case BEETROOTS -> cropToDrop = Material.BEETROOT;
      default -> {
        // No action needed for other materials
      }
    }

    if (cropToDrop != null) {
      world.dropItemNaturally(location, new ItemStack(cropToDrop, amountToDrop));
      world.playSound(location, soundToPlay, 1, 1);
    }
  }

  private void tryBonemealEquivalent(Block block, ItemStack item, boolean isCreative) {
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
}
