package com.mrjoshuasperry.deathmessage;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Location location = event.getEntity().getLocation();
        String coords = "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
        event.getEntity().sendMessage(ChatColor.GRAY + "You died at " + ChatColor.RED + coords);
        Bukkit.getLogger().info(event.getEntity().getName() + " died at " + coords);
    }
}
