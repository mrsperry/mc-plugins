package com.mrjoshuasperry.pocketplugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class Module implements Listener {
    private final String name;

    private PocketPlugins plugin;
    private YamlConfiguration config;
    private boolean enabled;

    public Module(String name) {
        this.name = name;

        this.plugin = PocketPlugins.getInstance();
    }

    public void initialize(YamlConfiguration config) {
        this.config = config;
        this.enabled = config.getBoolean("enabled", false);

        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public void onEnable() {
        Bukkit.getLogger().info(this.name + " enabled!");
        Bukkit.getServer().getPluginManager().registerEvents(this, PocketPlugins.getInstance());
    }

    public void onDisable() {
        Bukkit.getLogger().info(this.name + " disabled!");
        HandlerList.unregisterAll(this);
    }

    public final void enableModule() {
        this.enabled = true;
        this.onEnable();
    }

    public final void disableModule() {
        this.enabled = false;
        this.onDisable();
    }

    public final NamespacedKey createKey(String name) {
        return new NamespacedKey(this.plugin, name);
    }

    public final String getModuleName() {
        return this.name;
    }

    public final PocketPlugins getPlugin() {
        return this.plugin;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }
}
