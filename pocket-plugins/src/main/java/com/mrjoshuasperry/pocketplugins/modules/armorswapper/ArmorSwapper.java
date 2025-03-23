package com.mrjoshuasperry.pocketplugins.modules.armorswapper;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class ArmorSwapper extends Module {
  public ArmorSwapper(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
  }

  @EventHandler
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    Entity target = event.getRightClicked();

    if (!(target instanceof ArmorStand)) {
      return;
    }

    Player player = event.getPlayer();

    if (!player.isSneaking()) {
      return;
    }

    event.setCancelled(true);

    ArmorStand armorStand = (ArmorStand) target;
    List<EquipmentSlot> slots = List.of(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET);

    for (EquipmentSlot slot : slots) {
      this.swapItem(player, armorStand, slot);
    }
  }

  protected void swapItem(Player player, ArmorStand armorStand, EquipmentSlot slot) {
    ItemStack playerItem = player.getInventory().getItem(slot);
    ItemStack armorStandItem = armorStand.getItem(slot);

    if ((playerItem == null || playerItem.getType().isAir())
        && (armorStandItem == null || armorStandItem.getType().isAir())) {
      return;
    }

    player.getInventory().setItem(slot, armorStandItem);
    armorStand.setItem(slot, playerItem);
  }
}
