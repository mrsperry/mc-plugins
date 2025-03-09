package com.mrjoshuasperry.pocketplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class Module implements IModule, Listener {
    private final String name;
    private boolean enabled = false;

    public Module(String name) {
        this.name = name;
    }

    @Override
    public void init(YamlConfiguration config) {
        if (config != null && config.getBoolean("enabled")) {
            this.enableModule();
        } else {
            this.disableModule();
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

    public boolean isEnabled() {
        return this.enabled;
    }

    public void enableModule() {
        Bukkit.getLogger().info(this.getName() + " Initialized!");
        Bukkit.getServer().getPluginManager().registerEvents(this, PocketPlugins.getInstance());
        enabled = true;
    }

    public void disableModule() {
        Bukkit.getLogger().info(this.getName() + " Disabled!");
        HandlerList.unregisterAll(this);
        this.enabled = false;
    }
}
