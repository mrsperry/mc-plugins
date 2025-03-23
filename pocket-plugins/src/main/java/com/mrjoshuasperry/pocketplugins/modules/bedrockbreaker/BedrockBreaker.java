package com.mrjoshuasperry.pocketplugins.modules.bedrockbreaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class BedrockBreaker extends Module {
  protected List<Location> bedrockToRemove;

  public BedrockBreaker(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.bedrockToRemove = new ArrayList<>();
  }

  @Override
  public void onDisable() {
    super.onDisable();

    for (Location location : this.bedrockToRemove) {
      Block block = location.getBlock();

      if (block.getType() == Material.BEDROCK) {
        block.setType(Material.AIR);
      }
    }
  }

  @EventHandler
  public void onEntityInteract(PlayerInteractEvent event) {
    if (event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    if (!player.isSneaking()) {
      return;
    }

    PlayerInventory inventory = player.getInventory();
    ItemStack item = inventory.getItemInMainHand();
    if (item.getType() != Material.NETHERITE_INGOT) {
      return;
    }

    Block beacon = event.getClickedBlock();
    if (beacon == null) {
      return;
    }

    if (beacon.getType() != Material.BEACON) {
      return;
    }

    if (((Beacon) beacon.getState()).getTier() == 0) {
      return;
    }

    Block bedrock = this.getBedrockAbove(beacon);
    if (bedrock == null) {
      return;
    }

    GameMode gameMode = player.getGameMode();
    if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
      item.setAmount(item.getAmount() - 1);
    }

    event.setCancelled(true);
    this.bedrockToRemove.add(bedrock.getLocation());
    this.runRemovalAnimation(beacon.getLocation().clone().add(0, 1, 0), bedrock.getY());
  }

  public Block getBedrockAbove(Block beacon) {
    World world = beacon.getWorld();

    for (int y = beacon.getY() + 1; y <= world.getMaxHeight(); y++) {
      Block block = world.getBlockAt(beacon.getX(), y, beacon.getZ());

      if (this.bedrockToRemove.contains(block.getLocation())) {
        continue;
      }

      Material type = block.getType();
      if (type == Material.BEDROCK) {
        return block;
      }

      if (!type.isAir()) {
        return null;
      }
    }

    return null;
  }

  public void runRemovalAnimation(Location location, int targetY) {
    Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
      World world = location.getWorld();
      Random random = this.getPlugin().getRandom();

      if (location.getY() == targetY) {
        this.bedrockToRemove.remove(location);
        location.getBlock().breakNaturally();
        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5);
        world.playSound(location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2, 0);
        return;
      }

      world.spawnParticle(Particle.FLAME, location.clone().add(0.5, 0.5, 0.5), 20, 0.25, 0.25, 0.25, 0.05);
      world.playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1, random.nextFloat(0, 1));

      this.runRemovalAnimation(location.clone().add(0, 1, 0), targetY);
    }, 1);
  }
}
