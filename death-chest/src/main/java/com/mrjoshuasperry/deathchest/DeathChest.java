package com.mrjoshuasperry.deathchest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathChest implements ConfigurationSerializable {
    private final JavaPlugin plugin;
    private final Location location;
    private final PlayerInventory inventory;

    public DeathChest(JavaPlugin plugin, Location location, PlayerInventory inventory) {
        this.plugin = plugin;
        this.location = location;
        this.inventory = inventory;

        this.saveToConfig();
    }

    private void saveToConfig() {
        FileConfiguration config = this.plugin.getConfig();
        Set<DeathChest> chests = DeathChest.deserialize(this.plugin);
        chests.add(this);

        config.set(Main.CHEST_CONFIG_KEY, chests);
        this.plugin.saveConfig();
    }

    private void deleteFromConfig() {
        FileConfiguration config = this.plugin.getConfig();
        Set<DeathChest> chests = DeathChest.deserialize(this.plugin);
        chests.remove(this);

        config.set(Main.CHEST_CONFIG_KEY, chests);
        this.plugin.saveConfig();
    }

    public void takeItems(PlayerInventory inventory) {
        for (ItemStack item : this.inventory.getContents()) {
            if (item == null) {
                continue;
            }

            inventory.addItem(item);
        }

        this.deleteFromConfig();
    }

    public void openInventory(Player player) {
        World world = player.getWorld();
        Block block = world.getBlockAt(this.location);
        
        player.openInventory(inventory);
        player.playSound(this.location, Sound.BLOCK_CHEST_OPEN, 1, 1);
    }

    public void spill() {
        World world = this.location.getWorld();
        if (world == null) {
            return;
        }

        block.setType(Material.AIR);

        for (ItemStack item : this.items) {
            this.location.getWorld().dropItemNaturally(this.location, item);
        }

        this.plugin.getLogger().info("Spilling death chest in \"" + this.location.getWorld().getName() + "\"" + " at ("
                + this.location.getBlockX() + ", "
                + this.location.getBlockY() + ", "
                + this.location.getBlockZ() + ")");
    }

    public Map<String, Object> serialize() {
        Map<String, Object> output = new HashMap<>();
        Map<Integer, Map<String, Object>> items = new HashMap<>();

        output.put(Main.LOCATION_CONFIG_KEY, this.location.serialize());

        ItemStack[] contents = this.inventory.getContents();
        for (int index = 0; index < contents.length; index++) {
            items.put(index, contents[index].serialize());
        }
        output.put(Main.ITEMS_CONFIG_KEY, items);

        return output;
    }

    public static Set<DeathChest> deserialize(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Set<DeathChest> output = new HashSet<>();

        if (!config.isList(Main.CHEST_CONFIG_KEY)) {
            return output;
        }

        for (Map<?, ?> map : config.getMapList(Main.CHEST_CONFIG_KEY)) {
            Location location;
            PlayerInventory inventory = (PlayerInventory) Bukkit.createInventory(null, InventoryType.PLAYER);

            Object locationObject = map.get(Main.LOCATION_CONFIG_KEY);
            if (!(locationObject instanceof Map)) {
                continue;
            }

            Map<String, Object> locationMap = new HashMap<>();
            for (Entry<?, ?> entry : ((Map<?, ?>) locationObject).entrySet()) {
                Object key = entry.getKey();

                if (!(key instanceof String)) {
                    throw new AssertionError("Invalid location key-value pair: " + entry.toString());
                }

                locationMap.put((String) key, entry.getValue());
            }

            location = Location.deserialize(locationMap);

            Map<?, ?> inventoryMap = (Map<?, ?>) map.get(Main.ITEMS_CONFIG_KEY);
            for (Entry<?, ?> item : inventoryMap.entrySet()) {
                Object key = item.getKey();
                Object value = item.getValue();
                if (!(key instanceof Integer) || !(value instanceof Map)) {
                    continue;
                }

                Map<String, Object> itemStackMap = new HashMap<>();
                for (Entry<?, ?> itemEntry : ((Map<?, ?>) value).entrySet()) {
                    Object itemKey = itemEntry.getKey();
                    Object itemValue = itemEntry.getValue();
                    if (!(itemKey instanceof String)) {
                        continue;
                    }

                    itemStackMap.put((String) itemKey, itemValue);
                }

                inventory.setItem(Integer.parseInt((String) key), ItemStack.deserialize(itemStackMap));
            }

            output.add(new DeathChest(plugin, location, inventory));
        }

        return output;
    }

    public Location getLocation() {
        return this.location;
    }
}
