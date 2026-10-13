package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.audit.Audit;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class CraftingKeeper extends Module {
    protected Map<UUID, Location> tableBlocks;

    public CraftingKeeper(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.tableBlocks = new HashMap<>();

        ConfigurationSection tables = this.getWritableConfig().getConfigurationSection("tables");
        if (tables != null) {
            CraftingKeeperManager.getInstance().load(tables);
        }
    }

    @Override
    public void onDisable() {
        // A fresh section keeps a single "tables" key present so the writable config
        // is never empty, which is the case Module.saveConfig() skips writing
        ConfigurationSection tables = this.getWritableConfig().createSection("tables");
        CraftingKeeperManager.getInstance().save(tables);
        super.onDisable();
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
                    Audit.event(this.getPlugin(), "crafting-grid-restored").with("player", asPlayer(event.getPlayer()))
                            .at(loc).with("items", manager.getSavedInventory(loc)).record();
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
                    // From here until the server stops, these items are held only in memory
                    Audit.event(this.getPlugin(), "crafting-grid-stored").with("player", asPlayer(event.getPlayer()))
                            .at(loc).with("items", inventory.getContents()).record();
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
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && EquipmentSlot.HAND.equals(event.getHand())) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType().equals(Material.CRAFTING_TABLE)) {
                this.tableBlocks.put(event.getPlayer().getUniqueId(), block.getLocation());
            }

        }
    }

    /** Nothing drops a stored grid when its table goes, so the record is all that's left of it. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        CraftingKeeperManager manager = CraftingKeeperManager.getInstance();
        if (event.getBlock().getType() == Material.CRAFTING_TABLE && manager.isSaved(loc)) {
            Audit.event(this.getPlugin(), "crafting-table-broken").with("player", event.getPlayer()).at(loc)
                    .with("items", manager.getSavedInventory(loc)).record();
        }
    }

    private static Player asPlayer(HumanEntity entity) {
        return entity instanceof Player player ? player : null;
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