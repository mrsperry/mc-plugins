package com.mrjoshuasperry.deathchest.listeners;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.deathchest.DeathChest;
import com.mrjoshuasperry.deathchest.Main;

public class PlayerListener implements Listener {
    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        event.getDrops().clear();
        DeathChest.create(this.plugin, event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        BlockState state = block.getState();

        if (!(state instanceof Chest)) {
            return;
        }

        Chest chest = (Chest) state;

        boolean isDeathChest = chest.getPersistentDataContainer().getOrDefault(
                plugin.getDeathChestKey(),
                PersistentDataType.BOOLEAN,
                false);

        if (!isDeathChest) {
            return;
        }

        event.setCancelled(true);

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            DeathChest.takeItems(plugin, player, chest);
        } else {
            DeathChest.openInventory(plugin, player, chest);
        }
    }
}
