package com.mrjoshuasperry.deathchest.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.deathchest.Main;

public class BlockListener implements Listener {
    private final Main plugin;

    public BlockListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        BlockState state = event.getBlock().getState();

        if (!(state instanceof Chest)) {
            return;
        }

        Chest chest = (Chest) state;
        PersistentDataContainer container = chest.getPersistentDataContainer();
        boolean isDeathChest = container.getOrDefault(plugin.getDeathChestKey(), PersistentDataType.BOOLEAN, false);

        if (!isDeathChest) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.CHEST) {
            return;
        }

        BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
        for (BlockFace face : faces) {
            if (block.getRelative(face).getType() != Material.CHEST) {
                continue;
            }

            Chest chest = (Chest) block.getRelative(face).getState();
            PersistentDataContainer container = chest.getPersistentDataContainer();
            boolean isDeathChest = container.getOrDefault(plugin.getDeathChestKey(), PersistentDataType.BOOLEAN,
                    false);

            if (!isDeathChest) {
                continue;
            }

            event.setCancelled(true);
            return;
        }
    }
}
