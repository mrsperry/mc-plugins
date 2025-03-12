package com.mrjoshuasperry.pocketplugins.additions.autoplanter2;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Beehive;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.mcutils.StringUtils;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

import net.kyori.adventure.text.Component;

public class HiveInventory implements InventoryHolder {
  private static final NamespacedKey HIVE_KEY = new NamespacedKey(PocketPlugins.getInstance(), "hive-inventory");

  private final Inventory inventory;
  private final Beehive hive;

  public HiveInventory(Beehive hive) {
    JavaPlugin plugin = PocketPlugins.getInstance();

    String inventoryName = StringUtils.capitalize(hive.getType().toString().replaceAll("_", " "));

    this.inventory = plugin.getServer().createInventory(this, 9, Component.text(inventoryName));
    this.hive = hive;

    byte[] serializedData = hive.getPersistentDataContainer().get(HIVE_KEY, PersistentDataType.BYTE_ARRAY);

    if (serializedData == null) {
      return;
    }

    this.inventory.setContents(ItemStack.deserializeItemsFromBytes(serializedData));
  }

  public void save() {
    byte[] serializedData = ItemStack.serializeItemsAsBytes(this.inventory.getContents());
    this.hive.getPersistentDataContainer().set(HiveInventory.HIVE_KEY, PersistentDataType.BYTE_ARRAY, serializedData);
    this.hive.update();
  }

  @Override
  public Inventory getInventory() {
    return this.inventory;
  }

  public Beehive getHive() {
    return this.hive;
  }
}
