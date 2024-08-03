package com.mrjoshuasperry.autostack;

import io.github.mrsperry.mcutils.types.ToolTypes;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    private void searchForStack(Player player, ItemStack item) {
        // Only search for stacks if the item would be removed
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return;
        }

        // Get the player's inventory
        PlayerInventory inventory = player.getInventory();

        // Don't auto stack tools
        if (ToolTypes.getAllToolTypes().contains((item.getType()))) {
            return;
        }

        // Only auto stack when amount would equal zero
        if (item.getAmount() != 1) {
            return;
        }

        int handSlot = inventory.getHeldItemSlot();
        for (int index = 0; index < 36; index++) {
            if (index == handSlot) {
                continue;
            }

            // Get the current slot
            ItemStack current = inventory.getItem(index);
            // Skip over empty slots
            if (current == null) {
                continue;
            }

            // Guarantee items are the same type
            if (!current.isSimilar(item)) {
                continue;
            }

            inventory.setItemInMainHand(current);
            current.setAmount(0);
            return;
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) {
            this.searchForStack(event.getPlayer(), event.getItemInHand());
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        this.searchForStack(player, event.getItemDrop().getItemStack());
        player.updateInventory();
    }
}
