package com.mrjoshuasperry.miniadditions.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import com.mrjoshuasperry.miniadditions.MiniAdditions;

public class Module implements IModule, Listener {
    private final String name;

    public Module(String name) {
        this.name = name;
    }

    @Override
    public void init(YamlConfiguration config) {
        if (config != null && config.getBoolean("enabled")) {
            Bukkit.getLogger().info(this.getName() + " Initialized!");
            Bukkit.getServer().getPluginManager().registerEvents(this, MiniAdditions.getInstance());
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
    }
}
