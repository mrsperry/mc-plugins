package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class CraftingKeeperManager {
    private Map<Location, ItemStack[]> inventories;
    private static final CraftingKeeperManager self = new CraftingKeeperManager();

    private CraftingKeeperManager() {
        this.inventories = new HashMap<>();
    }

    public static CraftingKeeperManager getInstance() {
        return self;
    }

    public boolean isSaved(Location location) {
        return this.inventories.containsKey(location);
    }

    public ItemStack[] getSavedInventory(Location location) {
        return this.inventories.get(location);
    }

    public void saveInventory(Location location, ItemStack[] inventory) {
        this.inventories.put(location, inventory);
    }

    public void removeSaved(Location location) {
        this.inventories.remove(location);
    }

    /**
     * Writes every saved table into {@code section}, one numbered child each.
     * {@code Location} and {@code ItemStack} are both serialized by the config API
     * directly, so no {@code ConfigurationSerialization} registration is needed.
     * The caller is expected to pass a freshly created (empty) section.
     */
    public void save(ConfigurationSection section) {
        int index = 0;
        for (Map.Entry<Location, ItemStack[]> entry : this.inventories.entrySet()) {
            ConfigurationSection table = section.createSection(Integer.toString(index));
            table.set("location", entry.getKey());

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
        Map<Location, ItemStack[]> loaded = new HashMap<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection table = section.getConfigurationSection(key);
            if (table == null) {
                continue;
            }

            Location location = table.getLocation("location");
            if (location == null) {
                continue;
            }

            ItemStack[] contents = new ItemStack[table.getInt("size")];
            ConfigurationSection items = table.getConfigurationSection("contents");
            if (items != null) {
                for (String slot : items.getKeys(false)) {
                    contents[Integer.parseInt(slot)] = items.getItemStack(slot);
                }
            }

            loaded.put(location, contents);
        }

        this.inventories = loaded;
    }
}
