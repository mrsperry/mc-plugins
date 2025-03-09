package com.mrjoshuasperry.enhanceddungeons;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;
import com.mrjoshuasperry.enhanceddungeons.dungeons.DungeonKey;
import com.mrjoshuasperry.enhanceddungeons.dungeons.DungeonHandler;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.logging.Level;

public class Main extends JavaPlugin {
    /** The main plugin instance */
    private static JavaPlugin instance;
    /** The main random number generator */
    private static Random random;

    @Override
    public void onEnable() {
        Main.instance = this;
        Main.random = new Random();

        this.saveDefaultConfig();

        final FileConfiguration config = this.getConfig();

        // Load dungeon worlds from file
        final ConfigurationSection dungeons = config.getConfigurationSection("dungeons");
        DungeonHandler.initialize(dungeons == null ? config.createSection("dungeons") : dungeons);

        // Initialize dungeon key config values
        final ConfigurationSection dungeonKey = config.getConfigurationSection("dungeon-keys");
        DungeonKey.initialize(dungeonKey == null ? config.createSection("dungeon-keys") : dungeonKey);

        // Initialize party config values
        final ConfigurationSection parties = config.getConfigurationSection("parties");
        PartyHandler.initialize(parties == null ? config.createSection("parties") : parties);

        // Register the main command
        final PluginCommand command = this.getCommand("dungeon");
        if (command == null) {
            Utils.log(Level.SEVERE, "Could not bind main command!");
            return;
        }
        command.setExecutor(new Commands());

        // Register events
        final PluginManager manager = this.getServer().getPluginManager();
        manager.registerEvents(new DungeonKey(), this);
    }

    @Override
    public void onDisable() {
        DungeonHandler.endAllInstances();
    }

    /** @return The main plugin instance */
    public static JavaPlugin getInstance() {
        return Main.instance;
    }

    /** @return The main random number generator */
    public static Random getRandom() {
        return Main.random;
    }
}
