package com.mrjoshuasperry.pocketplugins.modules.woodpile;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class WoodPileListener extends Module {
    private final Map<WoodPile, BukkitRunnable> woodPiles;
    private int logConvertTime;

    public WoodPileListener() {
        super("WoodPile");
        woodPiles = new HashMap<>();
    }

    @Override
    public void initialize(YamlConfiguration config) {
        super.initialize(config);
        this.logConvertTime = config.getInt("log-convert-time", 5);
    }

    private BukkitRunnable createWoodPileRunnable(WoodPile woodPile, BlockPlaceEvent event) {
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

        if (replaced.equals(Material.FIRE) && WoodPile.isValidCovering(event.getBlock())) {
            WoodPile woodPile = new WoodPile();
            if (woodPile.checkValid(event.getBlock())) {
                woodPiles.put(woodPile, createWoodPileRunnable(woodPile, event));
                woodPiles.get(woodPile).runTaskTimer(PocketPlugins.getInstance(), 0, 2);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        for (Entry<WoodPile, BukkitRunnable> entry : woodPiles.entrySet()) {
            WoodPile woodPile = entry.getKey();
            if (woodPile.contains(event.getBlock())) {
                woodPiles.get(woodPile).cancel();
                woodPiles.remove(woodPile);
                event.getBlock().setType(Material.FIRE);
            }
        }
    }
}
