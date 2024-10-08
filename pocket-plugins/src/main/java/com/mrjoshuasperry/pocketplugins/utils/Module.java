package com.mrjoshuasperry.pocketplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class Module implements IModule, Listener {
    private final String name;

    public Module(String name) {
        this.name = name;
    }

    @Override
    public void init(YamlConfiguration config) {
        if (config != null && config.getBoolean("enabled")) {
            Bukkit.getLogger().info(this.getName() + " Initialized!");
            Bukkit.getServer().getPluginManager().registerEvents(this, PocketPlugins.getInstance());
        } else {
            Bukkit.getLogger().info(this.getName() + " Disabled!");
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onDisable() {
        // Its just that way ok
    }
}
