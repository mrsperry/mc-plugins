package com.mrjoshuasperry.autostack;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.mcutils.types.ToolTypes;

public class Main extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack item = event.getItemInHand();
        EquipmentSlot hand = event.getHand();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Only stack if the amount will equal zero after placing
        if (item.getAmount() != 1) {
            return;
        }

        // Disallow stacking of tools
        if (ToolTypes.getAllToolTypes().contains((item.getType()))) {
            return;
        }

        for (ItemStack stack : inventory.getContents()) {
            if (stack == item) {
                continue;
            }

            if (stack.getType() != item.getType()) {
                continue;
            }

            if (hand == EquipmentSlot.HAND) {
                inventory.setItemInMainHand(stack.clone());
            } else {
                inventory.setItemInOffHand(stack.clone());
            }

            stack.setAmount(0);
            break;
        }
    }
}
