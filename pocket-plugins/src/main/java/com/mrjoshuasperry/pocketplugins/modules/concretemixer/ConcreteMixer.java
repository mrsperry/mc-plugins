package com.mrjoshuasperry.pocketplugins.modules.concretemixer;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class ConcreteMixer extends Module {
    private final List<Material> concrete = List.of(
            Material.BLACK_CONCRETE_POWDER,
            Material.CYAN_CONCRETE_POWDER,
            Material.BLUE_CONCRETE_POWDER,
            Material.BROWN_CONCRETE_POWDER,
            Material.GRAY_CONCRETE_POWDER,
            Material.GREEN_CONCRETE_POWDER,
            Material.LIGHT_BLUE_CONCRETE_POWDER,
            Material.LIGHT_GRAY_CONCRETE_POWDER,
            Material.LIME_CONCRETE_POWDER,
            Material.MAGENTA_CONCRETE_POWDER,
            Material.ORANGE_CONCRETE_POWDER,
            Material.PINK_CONCRETE_POWDER,
            Material.PURPLE_CONCRETE_POWDER,
            Material.RED_CONCRETE_POWDER,
            Material.WHITE_CONCRETE_POWDER,
            Material.YELLOW_CONCRETE_POWDER);

    private int waterUseChance;

    public ConcreteMixer(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.waterUseChance = readableConfig.getInt("water-use-chance", 5);
    }

    private void handleConcreteHarden(Player player, ItemStack item, Block block) {
        Material type = block.getType();
        Levelled levelled = (Levelled) block.getBlockData();
        Material result = Material.valueOf(type.name().substring(0, type.name().length() - 7));
        block.getWorld().dropItem(block.getLocation().add(0.5, 1.5, 0.5), new ItemStack(result));

        if (item.getAmount() == 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        if (this.getPlugin().getRandom().nextInt(100) <= this.waterUseChance) {
            levelled.setLevel(levelled.getLevel() - 1);
            block.setBlockData(levelled);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        if (block.getType() != Material.CAULDRON) {
            return;
        }

        Levelled levelled = (Levelled) block.getBlockData();
        if (levelled.getLevel() == 0) {
            return;
        }

        Material type = item.getType();
        if (!concrete.contains(type)) {
            return;
        }

        event.setCancelled(true);
        this.handleConcreteHarden(event.getPlayer(), item, block);
    }
}
