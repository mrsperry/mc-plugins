package com.mrjoshuasperry.mcutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    // The owning plugin
    private JavaPlugin plugin;
    // All config names this manager handles
    private ArrayList<String> configNames;
    // All configs currently handled
    private HashMap<String, YamlConfiguration> configs;

    /**
     * Creates a new config manager and saves all default configs
     * 
     * @param plugin The owning plugin
     * @param names  All config names this manager handles (do not include file
     *               extensions ex: "players" NOT "players.yml")
     */
    public ConfigManager(JavaPlugin plugin, ArrayList<String> names) {
        this(plugin, names, true);
    }

    /**
     * Creates a new config manager
     * 
     * @param plugin      The owning plugin
     * @param names       All config names this manager handles (do not include file
     *                    extensions ex: "players" NOT "players.yml")
     * @param saveConfigs If default configs from the jar should be saved
     */
    public ConfigManager(JavaPlugin plugin, ArrayList<String> names, boolean saveConfigs) {
        // Set variables
        this.plugin = plugin;
        this.configNames = names;
        this.configs = new HashMap<>();

        // Check if default configs should be save
        if (saveConfigs) {
            this.plugin.getLogger().info("Saving default configs...");

            this.saveDefaultConfigs();
        }
    }

    /**
     * Saves default config files from the jar
     *
     * This creates a file if one does not exist and loads the contents of the file
     */
    public void saveDefaultConfigs() {
        // Get the config directory
        File directory = new File(this.plugin.getDataFolder().getAbsolutePath() + "/configs/");

        // Create the directory if it does not exist
        if (!directory.exists()) {
            this.plugin.getLogger().info("Config directory did not exist, creating...");
            directory.mkdirs();
        }

        // Search for configs that are handled by this manager
        for (String name : this.configNames) {
            name = name.toLowerCase() + ".yml";

            // Get the file in the config folder
            File file = new File(directory, name);
            try {
                // Check if the file exists
                if (!file.exists()) {
                    // If it doesn't exist, save a new file with contents from the jar
                    this.plugin.saveResource("configs/" + name, false);
                    this.plugin.getLogger().info("Creating missing config '" + name + "'");
                }
            } catch (IllegalArgumentException ex) {
                this.plugin.getLogger().severe("Could not find or create YML file: " + name);
                continue;
            }

            // Create a new config
            YamlConfiguration config = new YamlConfiguration();
            try {
                // Create a new scanner for the file
                Scanner reader = new Scanner(file);
                // Create a new builder for building the text from the file
                StringBuilder builder = new StringBuilder();

                // Loop through all lines in the file
                while (reader.hasNextLine()) {
                    builder.append(reader.nextLine()).append("\n");
                }
                reader.close();

                // Load the config from the read lines
                config.loadFromString(builder.toString().trim());
            } catch (NullPointerException | IOException | InvalidConfigurationException ex) {
                this.plugin.getLogger().severe("An error occurred while loading config '" + name + "'");
                ex.printStackTrace();
            }

            // Add the config (removing the file extension from its name)
            this.configs.put(name.substring(0, name.length() - 4), config);
        }
    }

    /**
     * Gets a config file this manager handles
     * 
     * @param name The name of the config file
     * @return The config file or null if it could not be found
     */
    public YamlConfiguration getConfig(String name) {
        return this.configs.getOrDefault(name, null);
    }

    /**
     * Saves a config file this manager handles
     * 
     * @param name The name of the config file
     */
    public void saveConfig(String name) {
        // Get the config file to save
        YamlConfiguration config = this.configs.getOrDefault(name, null);

        try {
            // Get the file this config will be saved to
            File file = new File(this.plugin.getDataFolder() + "/configs/" + name + ".yml");

            // Create a reader from the config
            Scanner reader = new Scanner(config.saveToString());
            // Create a writer for the file
            FileWriter writer = new FileWriter(file, file.createNewFile());

            // Loop through all lines in the config
            while (reader.hasNextLine()) {
                // Write the line to the file
                writer.write(reader.nextLine() + "\n");
            }
            reader.close();
            writer.close();
        } catch (IOException ex) {
            this.plugin.getLogger().severe("An error occurred while saving config '" + name + ".yml'");
            ex.printStackTrace();
        }
    }

    /**
     * Saves all config files handled by this manager
     */
    public void saveAllConfigs() {
        this.plugin.getLogger().info("Saving " + this.configs.size() + " config(s)");

        for (String name : this.configs.keySet()) {
            this.saveConfig(name);
        }
    }
}
