package com.mrjoshuasperry.pocketplugins.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class Module implements Listener {
    private final String name;

    private PocketPlugins plugin;

    private ConfigurationSection readableConfig;
    private ConfigurationSection writableConfig;
    private boolean enabled;

    public Module(String name) {
        this.name = name;

        this.plugin = PocketPlugins.getInstance();
    }

    public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        this.readableConfig = readableConfig;
        this.writableConfig = writableConfig;
        this.enabled = readableConfig.getBoolean("enabled", true);

        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public void onEnable() {
        this.plugin.getLogger().info(this.name + " enabled!");
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    public void onDisable() {
        this.plugin.getLogger().info(this.name + " disabled!");
        HandlerList.unregisterAll(this);
        this.saveConfig();
    }

    public final void enableModule() {
        this.enabled = true;
        this.onEnable();
    }

    public final void disableModule() {
        this.enabled = false;
        this.onDisable();
    }

    public final void registerBasicCommand(String command, BasicCommand commandClass) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                (ReloadableRegistrarEvent<Commands> event) -> event.registrar().register(command, commandClass));
    }

    public final void saveConfig() {
        YamlConfiguration config = new YamlConfiguration();
        for (String key : this.writableConfig.getKeys(false)) {
            config.set(key, this.writableConfig.get(key));
        }

        try {
            config.save(this.plugin.getDataFolder() + "/configs/" + this.name.toLowerCase() + ".yml");
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Could not save " + this.name + " configuration!");
            ex.printStackTrace();
        }
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

    public final ConfigurationSection getReadableConfig() {
        return this.readableConfig;
    }

    public final ConfigurationSection getWritableConfig() {
        return this.writableConfig;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }
}
