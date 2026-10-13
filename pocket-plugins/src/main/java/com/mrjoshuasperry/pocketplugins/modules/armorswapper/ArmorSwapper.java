package com.mrjoshuasperry.pocketplugins.modules.armorswapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

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

  // Ignoring cancelled clicks keeps stands in claimed land out of reach
  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    Entity target = event.getRightClicked();

    if (!(target instanceof ArmorStand armorStand) || !isSwappable(armorStand)) {
      return;
    }

    Player player = event.getPlayer();

    if (!player.isSneaking()) {
      return;
    }

    event.setCancelled(true);

    // The click arrives once for each hand; swapping on both would swap it all straight back
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }

    List<EquipmentSlot> slots = List.of(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET);

    Map<String, ItemStack> given = new LinkedHashMap<>();
    Map<String, ItemStack> taken = new LinkedHashMap<>();
    for (EquipmentSlot slot : slots) {
      if (!canSwapSlot(armorStand, slot)) {
        continue;
      }

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

  /**
   * Invisible and marker stands are scenery, often another plugin's (a biome bomb in
   * flight rides one), rather than somewhere a player keeps armor.
   */
  static boolean isSwappable(ArmorStand armorStand) {
    return armorStand.isVisible() && !armorStand.isMarker();
  }

  /** A slot locked in any way is left alone, just as vanilla leaves it. */
  static boolean canSwapSlot(ArmorStand armorStand, EquipmentSlot slot) {
    return noLock((ArmorStand.LockType lock) -> armorStand.hasEquipmentLock(slot, lock));
  }

  static boolean noLock(Predicate<ArmorStand.LockType> isLocked) {
    for (ArmorStand.LockType lock : ArmorStand.LockType.values()) {
      if (isLocked.test(lock)) {
        return false;
      }
    }
    return true;
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
