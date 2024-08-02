package com.mrjoshuasperry.mcutils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemMetaHandler {
    public static boolean hasKey(ItemStack item, NamespacedKey key, PersistentDataType type) {
        if (item.getItemMeta() != null) {
            return item.getItemMeta().getPersistentDataContainer().has(key, type);
        }
        return false;
    }

    public static Object get(ItemStack item, NamespacedKey key, PersistentDataType type) {
        if (item.getItemMeta() != null) {
            return item.getItemMeta().getPersistentDataContainer().get(key, type);
        }
        return null;
    }

    public static <T> boolean set(ItemStack item, NamespacedKey key, PersistentDataType type, T value) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(key, type, value);
            item.setItemMeta(itemMeta);
            return true;
        }
        return false;
    }
}
