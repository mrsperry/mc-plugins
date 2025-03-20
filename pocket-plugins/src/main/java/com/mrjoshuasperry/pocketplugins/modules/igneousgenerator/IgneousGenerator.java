package com.mrjoshuasperry.pocketplugins.modules.igneousgenerator;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.BlockUtils;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class IgneousGenerator extends Module {
    private final List<Material> igneousMaterials = List.of(Material.DIORITE, Material.GRANITE, Material.ANDESITE);

    protected int maxConversionTime;

    public IgneousGenerator() {
        super("IgneousGenerator");
    }

    @Override
    public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super.initialize(readableConfig, writableConfig);

        this.maxConversionTime = readableConfig.getInt("max-conversion-time", 2) * 20;
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Block to = event.getToBlock();
        if (!(event.getBlock().getType().equals(Material.WATER) && BlockUtils.isNextTo(to, Material.MAGMA_BLOCK))) {
            return;
        }

        event.setCancelled(true);

        PocketPlugins plugin = this.getPlugin();
        Random random = plugin.getRandom();
        Material rock = this.igneousMaterials.get(random.nextInt(this.igneousMaterials.size()));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            to.setType(rock);
            to.getWorld().playSound(to.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.25f, 0.25f);
        }, random.nextInt(this.maxConversionTime));
    }
}
