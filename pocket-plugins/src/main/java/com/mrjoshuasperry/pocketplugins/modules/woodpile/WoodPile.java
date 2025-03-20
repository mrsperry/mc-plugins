package com.mrjoshuasperry.pocketplugins.modules.woodpile;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class WoodPile extends Module {
    private final Map<WoodPileConstruct, BukkitRunnable> woodPiles;
    private int logConvertTime;

    public WoodPile() {
        super("WoodPile");
        woodPiles = new HashMap<>();
    }

    @Override
    public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super.initialize(readableConfig, writableConfig);
        this.logConvertTime = readableConfig.getInt("log-convert-time", 5);
    }

    private BukkitRunnable createWoodPileRunnable(WoodPileConstruct woodPile, BlockPlaceEvent event) {
        return new BukkitRunnable() {
            final int lifespan = woodPile.getFuelSize() * logConvertTime * 20;
            int age = 0;

            @Override
            public void run() {
                if (age > lifespan) {
                    woodPile.convertFuel();
                    this.cancel();
                    woodPiles.remove(woodPile);
                    event.getBlock().getWorld().playSound(event.getBlock().getLocation(),
                            Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
                }

                if (age % 10 == 0) {
                    woodPile.showBurning(Particle.LARGE_SMOKE);
                }

                if (age % 40 == 0 && PocketPlugins.getInstance().getRandom().nextBoolean()) {
                    event.getBlock().getWorld().playSound(event.getBlock().getLocation(),
                            Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f);
                }
                age += 2;
            }
        };
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material replaced = event.getBlockReplacedState().getType();

        if (replaced.equals(Material.FIRE) && WoodPileConstruct.isValidCovering(event.getBlock())) {
            WoodPileConstruct woodPile = new WoodPileConstruct();
            if (woodPile.checkValid(event.getBlock())) {
                woodPiles.put(woodPile, createWoodPileRunnable(woodPile, event));
                woodPiles.get(woodPile).runTaskTimer(PocketPlugins.getInstance(), 0, 2);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        for (Entry<WoodPileConstruct, BukkitRunnable> entry : woodPiles.entrySet()) {
            WoodPileConstruct woodPile = entry.getKey();
            if (woodPile.contains(event.getBlock())) {
                woodPiles.get(woodPile).cancel();
                woodPiles.remove(woodPile);
                event.getBlock().setType(Material.FIRE);
            }
        }
    }
}
