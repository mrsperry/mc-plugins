package com.mrjoshuasperry.pocketplugins.modules.igneousgenerator;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.mrjoshuasperry.pocketplugins.utils.BlockUtils;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class IgneousGeneratorListener extends Module {
    private final List<Material> igneousMaterials = Arrays.asList(Material.DIORITE, Material.GRANITE,
            Material.ANDESITE);

    public IgneousGeneratorListener() {
        super("IgneousGenerator");
    }

    @EventHandler
    public void blockFromToEvent(BlockFromToEvent event) {
        Block to = event.getToBlock();
        if (event.getBlock().getType().equals(Material.WATER) && BlockUtils.isNextTo(to, Material.MAGMA_BLOCK)) {
            Material rock = this.igneousMaterials.get(this.getPlugin().getRandom().nextInt(3));
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    to.setType(rock);
                    to.getWorld().playSound(to.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.25f, 0.25f);
                }
            }.runTaskLater(this.getPlugin(), this.getPlugin().getRandom().nextInt(40));

        }
    }
}
