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

import com.mrjoshuasperry.mcutils.audit.Audit;

public class Main extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        Audit.warnIfMissing(this);
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

        ItemStack placed = item.clone();
        int fromSlot = restackHand(inventory, item, hand);
        if (fromSlot >= 0) {
            Audit.event(this, "hand-restacked").with("player", player).at(event.getBlock().getLocation())
                    .with("hand", hand).with("placed", placed).with("from-slot", fromSlot)
                    .with("moved", inventory.getItem(hand)).record();
        }
    }

    /**
     * When the placed stack was the last of its kind in the given hand, refill that
     * hand from another matching stack elsewhere in the inventory and empty that
     * stack. Package-private and static so the inventory manipulation is unit-testable
     * without a BlockPlaceEvent.
     *
     * @return the slot the refill came from, or -1 if nothing matched
     */
    static int restackHand(PlayerInventory inventory, ItemStack placed, EquipmentSlot hand) {
        ItemStack[] contents = inventory.getContents();
        if (contents == null || contents.length == 0) {
            return -1;
        }

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
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
            return slot;
        }

        return -1;
    }
}
