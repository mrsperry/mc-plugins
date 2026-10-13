package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class CraftingKeeperManager {
    private Map<TableKey, ItemStack[]> inventories;
    private static final CraftingKeeperManager self = new CraftingKeeperManager();

    /**
     * A table's block, by world name rather than World, so a grid stored in a world
     * that isn't loaded is kept rather than dropped when the file is read or written.
     */
    record TableKey(String world, int x, int y, int z) {
        static TableKey of(Location location) {
            return new TableKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(),
                    location.getBlockZ());
        }
    }

    private CraftingKeeperManager() {
        this.inventories = new HashMap<>();
    }

    public static CraftingKeeperManager getInstance() {
        return self;
    }

    public boolean isSaved(Location location) {
        return this.inventories.containsKey(TableKey.of(location));
    }

    public ItemStack[] getSavedInventory(Location location) {
        return this.inventories.get(TableKey.of(location));
    }

    public void saveInventory(Location location, ItemStack[] inventory) {
        this.inventories.put(TableKey.of(location), inventory);
    }

    public void removeSaved(Location location) {
        this.inventories.remove(TableKey.of(location));
    }

    /** Removes and returns the table's stored grid, or null if it has none. */
    public ItemStack[] take(Location location) {
        return this.inventories.remove(TableKey.of(location));
    }

    /**
     * Writes every saved table into {@code section}, one numbered child each.
     * The caller is expected to pass a freshly created (empty) section.
     */
    public void save(ConfigurationSection section) {
        int index = 0;
        for (Map.Entry<TableKey, ItemStack[]> entry : this.inventories.entrySet()) {
            ConfigurationSection table = section.createSection(Integer.toString(index));
            TableKey key = entry.getKey();
            table.set("world", key.world());
            table.set("x", key.x());
            table.set("y", key.y());
            table.set("z", key.z());

            ItemStack[] contents = entry.getValue();
            table.set("size", contents.length);

            ConfigurationSection items = table.createSection("contents");
            // Empty slots are omitted; their index is restored from "size" on load
            for (int slot = 0; slot < contents.length; slot++) {
                if (contents[slot] != null) {
                    items.set(Integer.toString(slot), contents[slot]);
                }
            }

            index++;
        }
    }

    public void load(ConfigurationSection section) {
        Map<TableKey, ItemStack[]> loaded = new HashMap<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection table = section.getConfigurationSection(key);
            if (table == null) {
                continue;
            }

            TableKey tableKey = readKey(table);
            if (tableKey == null) {
                continue;
            }

            ItemStack[] contents = new ItemStack[table.getInt("size")];
            ConfigurationSection items = table.getConfigurationSection("contents");
            if (items != null) {
                for (String slot : items.getKeys(false)) {
                    contents[Integer.parseInt(slot)] = items.getItemStack(slot);
                }
            }

            loaded.put(tableKey, contents);
        }

        this.inventories = loaded;
    }

    private static TableKey readKey(ConfigurationSection table) {
        String world = table.getString("world");
        if (world != null) {
            return new TableKey(world, table.getInt("x"), table.getInt("y"), table.getInt("z"));
        }

        // Files written before 1.9.1 stored a Location, which only loads while its world does
        Location location = table.getLocation("location");
        return location == null || location.getWorld() == null ? null : TableKey.of(location);
    }
}
