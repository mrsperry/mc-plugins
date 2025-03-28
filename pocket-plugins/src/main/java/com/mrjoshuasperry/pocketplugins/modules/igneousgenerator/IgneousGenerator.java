package com.mrjoshuasperry.pocketplugins.modules.igneousgenerator;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;

import com.mrjoshuasperry.pocketplugins.utils.BlockUtils;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class IgneousGenerator extends Module {
    private final List<Material> igneousMaterials = List.of(Material.DIORITE, Material.GRANITE, Material.ANDESITE);

    protected int maxConversionTime;

    public IgneousGenerator(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.maxConversionTime = readableConfig.getInt("max-conversion-time", 2) * 20;
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();

        if (!this.isWet(from) || !BlockUtils.isNextTo(to, Material.MAGMA_BLOCK)) {
            return;
        }

        Material rock = this.igneousMaterials.get(this.getPlugin().getRandom().nextInt(this.igneousMaterials.size()));

        Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
            Material type = to.getType();
            if (!this.isWet(from) || !BlockUtils.isNextTo(to, Material.MAGMA_BLOCK)
                    || (!type.isAir() && type != Material.WATER)) {
                return;
            }

            to.setType(rock);
            to.getWorld().playSound(to.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.25f, 0.25f);
        }, this.getPlugin().getRandom().nextInt(this.maxConversionTime));

    }

    private boolean isWet(Block block) {
        return block.getType() == Material.WATER || (block.getBlockData() instanceof Waterlogged);
    }
}
