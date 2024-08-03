package com.mrjoshuasperry.deathchest.listeners;

import com.mrjoshuasperry.deathchest.DeathChest;
import com.mrjoshuasperry.deathchest.Main;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!event.getKeepInventory()) {
            ArrayList<ItemStack> drops = new ArrayList<>(event.getDrops());
            if (drops.size() > 0) {
                Block block = event.getEntity().getLocation().getBlock();
                int highest;
                int lowest;

                switch (event.getEntity().getWorld().getEnvironment()) {
                    case NETHER:
                    case THE_END:
                        highest = 255;
                        lowest = 0;
                        break;
                    default:
                        highest = 320;
                        lowest = -64;
                        break;
                }

                int y = block.getY();
                boolean add = y < highest;

                while (block.getType().isSolid() || !(y < highest && y > lowest)) {
                    block = block.getRelative(0, (add ? 1 : -1), 0);
                    y += (add ? 1 : -1);
                }

                Player player = event.getEntity();
                Location location = player.getLocation();
                String chestLocation = " in world \"" + player.getWorld().getName() + "\" at "
                        + "(" + location.getBlockX() + ", "
                        + location.getBlockY() + ", "
                        + location.getBlockZ() + ")";

                Bukkit.getLogger().info("Creating death chest for " + player.getName() + chestLocation);
                player.sendMessage(ChatColor.RED + "Your death chest is" + chestLocation);

                block.setType(Material.CHEST);

                Chest data = (Chest) block.getBlockData();
                data.setType(Chest.Type.SINGLE);

                Main.addChest(new DeathChest(block.getLocation(), drops));

                event.getDrops().clear();
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            if (event.getHand() == EquipmentSlot.HAND) {
                Block block = event.getClickedBlock();
                if (block != null && block.getType() == Material.CHEST) {
                    for (DeathChest chest : new HashSet<>(Main.getChests())) {
                        if (chest.getLocation().equals(block.getLocation())) {
                            event.setCancelled(true);

                            chest.spill();
                        }
                    }
                }
            }
        }
    }
}
