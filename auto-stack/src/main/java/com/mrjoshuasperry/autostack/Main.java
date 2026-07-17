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

        if (hand == EquipmentSlot.OFF_HAND) {
            item = inventory.getItemInOffHand();
        }

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Only stack if the amount will equal zero after placing
        if (item.getAmount() != 1) {
            return;
        }

        restackHand(inventory, item, hand);
    }

    /**
     * When the placed stack was the last of its kind in the given hand, refill that
     * hand from another matching stack elsewhere in the inventory and empty that
     * stack. Package-private and static so the inventory manipulation is unit-testable
     * without a BlockPlaceEvent.
     */
    static void restackHand(PlayerInventory inventory, ItemStack placed, EquipmentSlot hand) {
        ItemStack[] contents = inventory.getContents();
        if (contents == null || contents.length == 0) {
            return;
        }

        for (ItemStack stack : contents) {
            if (stack == null) {
                continue;
            }

            if (stack.equals(placed)) {
                continue;
            }

            if (stack.getType() != placed.getType()) {
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
