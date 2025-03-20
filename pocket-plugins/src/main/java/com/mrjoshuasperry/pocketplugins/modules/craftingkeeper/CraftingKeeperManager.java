package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

@SerializableAs("CraftingKeeperManager")
public class CraftingKeeperManager implements ConfigurationSerializable {
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

    private void setMap(Map<Location, ItemStack[]> invs) {
        this.inventories = invs;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        List<String> ids = new ArrayList<>();

        for (Map.Entry<Location, ItemStack[]> entry : this.inventories.entrySet()) {
            String randId = UUID.randomUUID().toString().replace("-", "");
            Map<String, Object> craftingTable = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Location location = entry.getKey();

            for (ItemStack item : entry.getValue()) {
                contents.add(item.serialize());
            }

            craftingTable.put("location", location.serialize());
            craftingTable.put("contents", contents);
            result.put(randId, craftingTable);
            ids.add(randId);
        }

        result.put("ids", ids);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static CraftingKeeperManager deserialize(Map<String, Object> args) {
        CraftingKeeperManager manager = getInstance();
        Map<Location, ItemStack[]> savedTables = new HashMap<>();
        List<String> ids = (ArrayList<String>) args.get("ids");

        for (String id : ids) {
            Map<String, Object> craftingTable = (Map<String, Object>) args.get(id);
            Location loc = Location.deserialize((Map<String, Object>) craftingTable.get("location"));
            List<ItemStack> items = new ArrayList<>();
            List<Map<String, Object>> craftingContents = (List<Map<String, Object>>) craftingTable.get("contents");

            craftingContents.forEach(serializedItem -> items.add(ItemStack.deserialize(serializedItem)));
            savedTables.put(loc, items.toArray(new ItemStack[items.size()]));
        }

        manager.setMap(savedTables);
        return manager;
    }
}
