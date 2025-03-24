package com.mrjoshuasperry.pocketplugins.modules.concretemixer;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author Idea by TimPCunningham, implemented by mrsperry */
public class ConcreteMixer extends Module {
    protected Map<Material, Material> concrete = new HashMap<>();
    {
        concrete.put(Material.BLACK_CONCRETE_POWDER, Material.BLACK_CONCRETE);
        concrete.put(Material.CYAN_CONCRETE_POWDER, Material.CYAN_CONCRETE);
        concrete.put(Material.BLUE_CONCRETE_POWDER, Material.BLUE_CONCRETE);
        concrete.put(Material.BROWN_CONCRETE_POWDER, Material.BROWN_CONCRETE);
        concrete.put(Material.GRAY_CONCRETE_POWDER, Material.GRAY_CONCRETE);
        concrete.put(Material.GREEN_CONCRETE_POWDER, Material.GREEN_CONCRETE);
        concrete.put(Material.LIGHT_BLUE_CONCRETE_POWDER, Material.LIGHT_BLUE_CONCRETE);
        concrete.put(Material.LIGHT_GRAY_CONCRETE_POWDER, Material.LIGHT_GRAY_CONCRETE);
        concrete.put(Material.LIME_CONCRETE_POWDER, Material.LIME_CONCRETE);
        concrete.put(Material.MAGENTA_CONCRETE_POWDER, Material.MAGENTA_CONCRETE);
        concrete.put(Material.ORANGE_CONCRETE_POWDER, Material.ORANGE_CONCRETE);
        concrete.put(Material.PINK_CONCRETE_POWDER, Material.PINK_CONCRETE);
        concrete.put(Material.PURPLE_CONCRETE_POWDER, Material.PURPLE_CONCRETE);
        concrete.put(Material.RED_CONCRETE_POWDER, Material.RED_CONCRETE);
        concrete.put(Material.WHITE_CONCRETE_POWDER, Material.WHITE_CONCRETE);
        concrete.put(Material.YELLOW_CONCRETE_POWDER, Material.YELLOW_CONCRETE);
    }

    protected int waterUseChance;

    public ConcreteMixer(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.waterUseChance = readableConfig.getInt("water-use-chance", 5);

        Bukkit.getScheduler().runTaskTimer(this.getPlugin(), this::convertConcretePowder, 0, 5);
    }

    protected void convertConcretePowder() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Item.class)) {
                ItemStack item = ((Item) entity).getItemStack();
                if (item == null) {
                    continue;
                }

                Material itemType = item.getType();
                if (!this.concrete.keySet().contains(itemType)) {
                    continue;
                }

                Block block = entity.getLocation().getBlock();
                if (block.getType() != Material.WATER_CAULDRON) {
                    continue;
                }

                Levelled levelled = (Levelled) block.getBlockData();
                if (levelled.getLevel() == 0) {
                    continue;
                }

                block.getWorld().dropItem(block.getLocation().add(0.5, 1.5, 0.5),
                        new ItemStack(this.concrete.get(itemType)));
                item.subtract();

                if (this.getPlugin().getRandom().nextInt(100) > this.waterUseChance) {
                    continue;
                }

                int newLevel = levelled.getLevel() - 1;

                if (newLevel == 0) {
                    block.setType(Material.CAULDRON);
                    continue;
                }

                levelled.setLevel(levelled.getLevel() - 1);
                block.setBlockData(levelled);
            }
        }
    }
}
