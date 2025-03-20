package com.mrjoshuasperry.pocketplugins.modules.leadattacher;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LeashHitch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class LeadAttacherListener extends Module {
    public LeadAttacherListener() {
        super("LeadAttacherListener");
    }

    private ItemStack getItemInHand(EquipmentSlot handType, PlayerInventory playerInventory) {
        if (handType == EquipmentSlot.HAND) {
            return playerInventory.getItemInMainHand();
        } else {
            return playerInventory.getItemInOffHand();
        }
    }

    private void placeLead(ItemStack stack, Block block) {
        final World world = block.getWorld();

        for (LeashHitch hitch : world.getEntitiesByClass(LeashHitch.class)) {
            final Block hitchBlock = hitch.getLocation().getBlock();

            if (hitchBlock.getX() == block.getX() && hitchBlock.getY() == block.getY()
                    && hitchBlock.getZ() == block.getZ()) {
                return;
            }
        }

        world.spawn(block.getLocation(), LeashHitch.class);
        stack.setAmount(stack.getAmount() - 1);
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block block = event.getClickedBlock();

            if (block != null && block.getType().toString().endsWith("_FENCE")) {
                final PlayerInventory inventory = event.getPlayer().getInventory();
                final ItemStack stack = getItemInHand(event.getHand(), inventory);

                if (stack.getType() == Material.LEAD) {
                    placeLead(stack, block);
                }
            }
        }
    }
}
