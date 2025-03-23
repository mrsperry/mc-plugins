package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class CraftingKeeper extends Module {
    protected Map<UUID, Location> tableBlocks;

    public CraftingKeeper(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.tableBlocks = new HashMap<>();
        ConfigurationSerialization.registerClass(CraftingKeeperManager.class, "CraftingKeeperManager");
        this.loadCrafting();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.saveCrafting();
    }

    // TODO: Update to use new config system
    private void saveCrafting() {
        CraftingKeeperManager manager = CraftingKeeperManager.getInstance();
        FileConfiguration config = new YamlConfiguration();

        config.set("tables", manager);
        try {
            config.save(new File(this.getPlugin().getDataFolder(), "crafting_tables.yml"));
        } catch (Exception e) {
            this.getPlugin().getLogger().warning("Error saving crafting tables!");
        }
    }

    // TODO: Update to use new config system
    private void loadCrafting() {
        try {
            FileConfiguration config = YamlConfiguration
                    .loadConfiguration(new File(this.getPlugin().getDataFolder(), "crafting_tables.yml"));
            config.get("tables");
        } catch (Exception e) {
            this.getPlugin().getLogger().warning("Error loading crafting tables!");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType().equals(InventoryType.WORKBENCH)) {
            UUID uuid = event.getPlayer().getUniqueId();
            if (this.tableBlocks.containsKey(uuid)) {
                CraftingKeeperManager manager = CraftingKeeperManager.getInstance();
                Location loc = this.tableBlocks.get(uuid);

                if (manager.isSaved(loc)) {
                    event.getInventory().setContents(manager.getSavedInventory(loc));
                }
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event) {
        if (event.getInventory().getType().equals(InventoryType.WORKBENCH)) {
            UUID uuid = event.getPlayer().getUniqueId();
            if (this.tableBlocks.containsKey(uuid)) {
                Inventory inventory = event.getInventory();
                Location loc = this.tableBlocks.get(uuid);
                CraftingKeeperManager manager = CraftingKeeperManager.getInstance();

                if (!isEmpty(inventory)) {
                    manager.saveInventory(loc, inventory.getContents());
                    event.getInventory().clear();
                } else if (manager.isSaved(loc)) { // Empty and saved, need to remove now
                    manager.removeSaved(loc);
                }
                this.tableBlocks.remove(uuid);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getHand() != null
                && event.getHand().equals(EquipmentSlot.HAND)) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType().equals(Material.CRAFTING_TABLE)) {
                this.tableBlocks.put(event.getPlayer().getUniqueId(), block.getLocation());
            }

        }
    }

    private boolean isEmpty(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().equals(Material.AIR)) {
                return false;
            }
        }
        return true;
    }
}