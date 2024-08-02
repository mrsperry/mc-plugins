package com.mrjoshuasperry.miniadditions.additions.cobblegenerator;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFormEvent;

public class CobbleGeneratorListener extends Module {
    public CobbleGeneratorListener() {
        super("CobbleGenerator");
    }

    @Override
    public void init(YamlConfiguration config) {
        super.init(config);

        if (config.isConfigurationSection("blocks")) {
            CobbleGeneratorManager.loadConfig(config.getConfigurationSection("blocks"));
        }
    }

    @EventHandler
    public void onFromTo(BlockFormEvent event) {
        if (event.getNewState().getType().equals(Material.COBBLESTONE)) {
            event.getNewState().setType(CobbleGeneratorManager.getInstance().generateBlock());
            event.getNewState().getWorld().playSound(event.getNewState().getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1f,
                    1.5f);
        }
    }
}