package com.mrjoshuasperry.pocketplugins.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryUtils {
  public static ItemStack getItemIfInHands(Player player, Material item) {
    ItemStack mainHandItem = player.getInventory().getItemInMainHand();
    ItemStack offHandItem = player.getInventory().getItemInOffHand();

    if (mainHandItem.getType() == item)
      return mainHandItem;
    if (offHandItem.getType() == item)
      return offHandItem;

    return null;
  }

  public static boolean hasPersistentData(ItemStack item, NamespacedKey key) {
    if (item == null)
      return false;

    ItemMeta meta = item.getItemMeta();
    if (meta == null)
      return false;

    return meta.getPersistentDataContainer().has(key);
  }

  public static List<ItemStack> getItemsByType(Inventory inventory, Material item) {
    List<ItemStack> items = new ArrayList<>();
    for (ItemStack itemStack : inventory.getContents()) {
      if (itemStack != null && itemStack.getType() == item)
        items.add(itemStack);
    }
    return items;
  }
}
