package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class CraftingKeeperListener extends Module {
    Map<UUID, Location> tableBlocks;

    public CraftingKeeperListener() {
        super("CraftingKeeper");
        this.tableBlocks = new HashMap<>();
        ConfigurationSerialization.registerClass(CraftingKeeperManager.class, "CraftingKeeperManager");
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