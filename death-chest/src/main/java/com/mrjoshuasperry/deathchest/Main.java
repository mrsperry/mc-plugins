package com.mrjoshuasperry.deathchest;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.deathchest.listeners.BlockListener;
import com.mrjoshuasperry.deathchest.listeners.PlayerListener;

public class Main extends JavaPlugin {
    public static final String CHEST_CONFIG_KEY = "death-chests";
    public static final String LOCATION_CONFIG_KEY = "location";
    public static final String ITEMS_CONFIG_KEY = "items";

    private final Set<DeathChest> chests = new HashSet<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        chests.addAll(DeathChest.deserialize(this.getConfig()));

        PluginManager manager = this.getServer().getPluginManager();
        manager.registerEvents(new BlockListener(), this);
        manager.registerEvents(new PlayerListener(), this);
    }

    public Set<DeathChest> getChests() {
        return this.chests;
    }

    public void addChest(DeathChest chest) {
        this.chests.add(chest);
    }

    public void removeChest(DeathChest chest) {
        this.chests.remove(chest);
    }
}
