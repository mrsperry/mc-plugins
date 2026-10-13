package com.mrjoshuasperry.pocketplugins.modules.craftingkeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.audit.Audit;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/**
 * Crafting tables keep whatever is left in their grid when closed, and hand it back
 * to whoever opens them next.
 *
 * <p>
 * A stored grid lives in exactly one place at a time: in this module's file while
 * the table is closed, or in the open grid once someone takes it. That's what stops
 * two players opening one table both getting the items, and the file is written on
 * every change so a crash can't lose them.
 *
 * @author TimPCunningham
 */
public class CraftingKeeper extends Module {
    /** Farther than this from the table they last clicked, a workbench isn't that table. */
    private static final double REACH_SQUARED = 8 * 8;

    /** The table each player last right-clicked, waiting for its grid to open. */
    protected Map<UUID, Location> clickedTables;
    /** The table behind each open grid, so closing it stores to the same place. */
    protected Map<UUID, Location> openTables;

    public CraftingKeeper(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.clickedTables = new HashMap<>();
        this.openTables = new HashMap<>();

        ConfigurationSection tables = this.getWritableConfig().getConfigurationSection("tables");
        if (tables != null) {
            CraftingKeeperManager.getInstance().load(tables);
        }
    }

    @Override
    public void onDisable() {
        // Module.onDisable saves the file
        this.copyTablesToConfig();
        super.onDisable();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && EquipmentSlot.HAND.equals(event.getHand())
                // A protection plugin that stops the table opening leaves no grid to fill
                && event.useInteractedBlock() != Event.Result.DENY) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType().equals(Material.CRAFTING_TABLE)) {
                this.clickedTables.put(event.getPlayer().getUniqueId(), block.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvOpen(InventoryOpenEvent event) {
        if (!event.getInventory().getType().equals(InventoryType.WORKBENCH)) {
            return;
        }

        HumanEntity player = event.getPlayer();
        Location loc = this.clickedTables.remove(player.getUniqueId());
        if (loc == null || !isTableFor(loc, player)) {
            return;
        }

        this.openTables.put(player.getUniqueId(), loc);

        ItemStack[] saved = CraftingKeeperManager.getInstance().take(loc);
        if (saved != null) {
            event.getInventory().setContents(saved);
            this.writeTables();
            Audit.event(this.getPlugin(), "crafting-grid-restored").with("player", asPlayer(player)).at(loc)
                    .with("items", saved).record();
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent event) {
        if (!event.getInventory().getType().equals(InventoryType.WORKBENCH)) {
            return;
        }

        Location loc = this.openTables.remove(event.getPlayer().getUniqueId());
        Inventory inventory = event.getInventory();
        CraftingKeeperManager manager = CraftingKeeperManager.getInstance();

        // If another player stored their grid here first, or the table has gone (which
        // is what closed the grid), this one goes back to its owner the vanilla way
        if (loc == null || isEmpty(inventory) || manager.isSaved(loc)
                || loc.getBlock().getType() != Material.CRAFTING_TABLE) {
            return;
        }

        manager.saveInventory(loc, inventory.getContents());
        this.writeTables();
        Audit.event(this.getPlugin(), "crafting-grid-stored").with("player", asPlayer(event.getPlayer())).at(loc)
                .with("items", inventory.getContents()).record();
        inventory.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.clickedTables.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.dropStoredGrid(event.getBlock(), "broken", event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        this.dropStoredGrid(event.getBlock(), "burnt", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            this.dropStoredGrid(block, "exploded", null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            this.dropStoredGrid(block, "exploded", null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (this.holdsStoredGrid(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (this.holdsStoredGrid(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    /** A pushed table would leave its grid behind at the old spot, so a table holding one stays put. */
    private boolean holdsStoredGrid(List<Block> blocks) {
        CraftingKeeperManager manager = CraftingKeeperManager.getInstance();
        return blocks.stream().anyMatch(
                block -> block.getType() == Material.CRAFTING_TABLE && manager.isSaved(block.getLocation()));
    }

    /** Spills a table's stored grid where it stood, since the table it belonged to is going. */
    private void dropStoredGrid(Block block, String cause, Player player) {
        if (block.getType() != Material.CRAFTING_TABLE) {
            return;
        }

        Location loc = block.getLocation();
        ItemStack[] saved = CraftingKeeperManager.getInstance().take(loc);
        if (saved == null) {
            return;
        }

        Location center = loc.clone().add(0.5, 0.5, 0.5);
        for (ItemStack item : saved) {
            if (item != null && !item.getType().isAir()) {
                block.getWorld().dropItemNaturally(center, item);
            }
        }

        this.writeTables();
        Audit.event(this.getPlugin(), "crafting-grid-dropped").with("player", player).at(loc).with("cause", cause)
                .with("items", saved).record();
    }

    /** The clicked table, if it's still a table and the player is still at it. */
    static boolean isTableFor(Location table, HumanEntity player) {
        return table.getBlock().getType() == Material.CRAFTING_TABLE
                && table.getWorld().equals(player.getWorld())
                && table.clone().add(0.5, 0.5, 0.5).distanceSquared(player.getLocation()) <= REACH_SQUARED;
    }

    /** Written on every change, so stored grids survive a crash. */
    private void writeTables() {
        this.copyTablesToConfig();
        this.saveConfig();
    }

    private void copyTablesToConfig() {
        // A fresh section keeps a single "tables" key present so the writable config
        // is never empty, which is the case Module.saveConfig() skips writing
        ConfigurationSection tables = this.getWritableConfig().createSection("tables");
        CraftingKeeperManager.getInstance().save(tables);
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
