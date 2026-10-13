package com.mrjoshuasperry.pocketplugins.modules.armorswapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.audit.Audit;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class ArmorSwapper extends Module {
  public ArmorSwapper(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
  }

  @EventHandler
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    Entity target = event.getRightClicked();

    if (!(target instanceof ArmorStand armorStand)) {
      return;
    }

    Player player = event.getPlayer();

    if (!player.isSneaking()) {
      return;
    }

    event.setCancelled(true);

    List<EquipmentSlot> slots = List.of(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET);

    Map<String, ItemStack> given = new LinkedHashMap<>();
    Map<String, ItemStack> taken = new LinkedHashMap<>();
    for (EquipmentSlot slot : slots) {
      String name = slot.name().toLowerCase(Locale.ROOT);
      given.put(name, player.getInventory().getItem(slot).clone());
      taken.put(name, armorStand.getItem(slot).clone());
      this.swapItem(player, armorStand, slot);
    }

    boolean anything = given.values().stream().anyMatch(item -> !item.isEmpty())
        || taken.values().stream().anyMatch(item -> !item.isEmpty());
    if (anything) {
      Audit.event(this.getPlugin(), "armor-swapped").with("player", player).with("stand", armorStand)
          .at(armorStand.getLocation()).with("given", given).with("taken", taken).record();
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
