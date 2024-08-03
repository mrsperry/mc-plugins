package com.mrjoshuasperry.deathchest.listeners;

import com.mrjoshuasperry.deathchest.DeathChest;
import com.mrjoshuasperry.deathchest.Main;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashSet;

public class BlockListener implements Listener {
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.checkSpill(event.getBlock())) {
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        HashSet<Block> remove = new HashSet<>();
        for (Block block : event.blockList()) {
            if (this.checkSpill(block)) {
                remove.add(block);
            }
        }

        for (Block block : remove) {
            event.blockList().remove(block);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        HashSet<Block> remove = new HashSet<>();
        for (Block block : event.blockList()) {
            if (this.checkSpill(block)) {
                remove.add(block);
            }
        }

        for (Block block : remove) {
            event.blockList().remove(block);
        }
    }

    private boolean checkSpill(Block block) {
        if (block.getType() == Material.CHEST) {
            for (DeathChest chest : new HashSet<>(Main.getChests())) {
                if (chest.getLocation().equals(block.getLocation())) {
                    chest.spill();

                    return true;
                }
            }
        }

        return false;
    }
}
