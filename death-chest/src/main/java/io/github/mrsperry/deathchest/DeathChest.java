package io.github.mrsperry.deathchest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DeathChest implements ConfigurationSerializable {
    private final Location location;

    private final List<ItemStack> items;

    public DeathChest(Location location, List<ItemStack> items) {
        this.location = location;

        this.items = items;
    }

    public void spill() {
        World world = this.location.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(this.location, Sound.BLOCK_CHEST_OPEN, 1, 1);

        Block block = world.getBlockAt(this.location);
        block.setType(Material.AIR);

        for (ItemStack item : this.items) {
            this.location.getWorld().dropItemNaturally(this.location, item);
        }

        Main.removeChest(this);

        Bukkit.getLogger().info("Spilling death chest in \"" + this.location.getWorld().getName() + "\"" + " at ("
                + this.location.getBlockX() + ", "
                + this.location.getBlockY() + ", "
                + this.location.getBlockZ() + ")");
    }

    public Map<String, Object> serialize() {
        LinkedHashMap<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        output.put("location", this.location.serialize());
        for (ItemStack item : this.items) {
            items.add(item.serialize());
        }
        output.put("items", items);

        return output;
    }

    public static HashSet<DeathChest> deserialize(FileConfiguration config) {
        HashSet<DeathChest> output = new HashSet<>();

        if (config.isList("chests")) {
            for (Map<?, ?> map : config.getMapList("chests")) {
                Location location = null;
                if (map.containsKey("location")) {
                    if (map.get("location") != null) {
                        location = Location.deserialize((Map<String, Object>) map.get("location"));
                    }
                }
                if (location == null) {
                    continue;
                }

                List<ItemStack> items = new ArrayList<>();
                if (map.containsKey("items")) {
                    List<Map<String, Object>> inventoryMap = (List<Map<String, Object>>) map.get("items");
                    for (Map<String, Object> item : inventoryMap) {
                        items.add(ItemStack.deserialize(item));
                    }
                }

                output.add(new DeathChest(location, items));
            }
        }

        return output;
    }

    public Location getLocation() {
        return this.location;
    }
}
