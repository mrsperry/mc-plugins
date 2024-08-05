package com.mrjoshuasperry.enhanceddungeons.dungeons;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class DungeonHandler {
    /** A list of all dungeon worlds */
    private static Map<String, DungeonWorld> worlds;
    /** A list of all active dungeon worlds */
    private static Map<String, DungeonInstance> instances;

    /** The file path to the dungeon world folder */
    private static String folderPath;

    /**
     * Loads dungeon worlds from file
     * @param config The section used for config values
     */
    public static void initialize(final ConfigurationSection config) {
        DungeonHandler.worlds = new HashMap<>();
        DungeonHandler.instances = new HashMap<>();

        // Get any custom dungeon world folder path
        String path = config.getString("folder-path");
        if (path == null) {
            path = "dungeons";
        }
        DungeonHandler.folderPath = path;

        // Get the dungeon worlds directory
        final File worldsFolder = new File(path);

        // If the directory doesn't exist, try to create it
        if (!worldsFolder.exists() && !worldsFolder.mkdirs()) {
            Utils.log(Level.WARNING, "Could not create directories to the dungeon world folder!");
            return;
        }

        // Get each subdirectory
        File[] folders = worldsFolder.listFiles();
        if (folders == null) {
            folders = new File[0];
        }

        // Try to load each subdirectory as a world
        for (final File folder : folders) {
            final String name = folder.getName();
            Utils.log(Level.INFO, "Loading dungeon world: " + name);

            // Create the world
            final World world = Bukkit.createWorld(new WorldCreator(path + "/" + name));
            if (world == null) {
                Utils.log(Level.SEVERE, "Could not load dungeon world: " + name);
                return;
            }

            // Parse the world's XML file
            final DungeonConfig dungeonConfig = DungeonHandler.loadDungeonConfig(world);
            if (dungeonConfig == null) {
                return;
            }

            // Create the dungeon world
            final DungeonWorld dungeon = new DungeonWorld(name, world, dungeonConfig);
            DungeonHandler.worlds.put(name, dungeon);
        }
    }

    private static DungeonConfig loadDungeonConfig(final World world) {
        DungeonConfig config = null;

        // Get the world folder and files
        final File folder = world.getWorldFolder();
        final File[] files = folder.listFiles();

        if (files != null) {
            // Search each file for the XML
            for (final File file : files) {
                if (file.getName().equalsIgnoreCase("dungeon.xml")) {
                    config = new DungeonConfig(world, file);
                    break;
                }
            }

            if (config == null) {
                Utils.log(Level.SEVERE, "Could not find dungeon.xml: " + folder.getName());
                return null;
            }
        } else {
            Utils.log(Level.SEVERE, "Could not find world folder files: " + folder.getName());
            return null;
        }

        return config;
    }

    /**
     * Creates a new dungeon world
     * @param id The world name of the dungeon
     * @param party The party participating in the dungeon
     * @return The created dungeon world or null if creation failed
     */
    public static DungeonInstance createDungeonInstance(final String id, final Party party) {
        final Player owner = party.getOwner();

        // Check if the ID is a valid dungeon world name
        if (!DungeonHandler.worlds.containsKey(id)) {
            Utils.log(Level.SEVERE, "Could not find dungeon world: " + id);

            owner.sendMessage(ChatColor.RED + "Could not find dungeon world: " + id);
            return null;
        }

        // Try to get the dungeon world
        final World world = Bukkit.getWorld(DungeonHandler.folderPath + "/" + id);
        if (world == null) {
            Utils.log(Level.SEVERE, "Could not find dungeon world: " + id);

            owner.sendMessage(ChatColor.RED + "An error occurred while trying to load the dungeon");
            return null;
        }

        // Check if the dungeon is currently active
        if (DungeonHandler.instances.containsKey(id)) {
            Utils.log(Level.SEVERE, "A group tried to join a dungeon in progress: " + id);

            owner.sendMessage(ChatColor.RED + "That dungeon is currently in progress with a different group");
            return null;
        }

        final DungeonWorld dungeonWorld = DungeonHandler.worlds.get(id);

        // Check if there are too many players for this dungeon
        final int members = party.getMembers().size();
        final int maxPlayers = dungeonWorld.getConfig().getMaxPlayers();

        if (members > maxPlayers) {
            Utils.log(Level.SEVERE, "A group of " + members + " tried to join a dungeon with a capacity of " + maxPlayers);

            owner.sendMessage(ChatColor.RED + "The maximum number of players for this dungeon is " + maxPlayers + "!");
            return null;
        }

        // Create the dungeon instance
        final DungeonInstance dungeon = new DungeonInstance(dungeonWorld, party);
        DungeonHandler.instances.put(id, dungeon);

        return dungeon;
    }

    /**
     * Gets a running dungeon instance
     * @param id The ID of the dungeon world
     * @return The running instance or null if it could not be found
     */
    public static DungeonInstance getDungeonInstance(final String id) {
        return DungeonHandler.instances.getOrDefault(id, null);
    }

    /**
     * Removes a loaded dungeon instance, allowing it to be played by other groups
     * @param id The ID of the dungeon layout
     */
    public static void removeDungeonInstance(final String id) {
        DungeonHandler.instances.remove(id);
    }

    /** Forcibly ends all dungeon instances */
    public static void endAllInstances() {
        for (final DungeonInstance instance : DungeonHandler.instances.values()){
            instance.end(true);
        }
    }

    /**
     * Reloads a dungeon config from file
     * @param id The ID of the dungeon world
     * @return If the world could be found
     */
    public static boolean reloadConfig(final String id) {
        final DungeonInstance instance = DungeonHandler.getDungeonInstance(id);
        if (instance != null) {
            instance.end(true);
        }

        final DungeonWorld world = DungeonHandler.worlds.getOrDefault(id, null);
        if (world == null) {
            return false;
        }

        world.setConfig(DungeonHandler.loadDungeonConfig(world.getWorld()));
        return true;
    }

    /** Reloads all dungeon configs from file*/
    public static void reloadAllConfigs() {
        for (final String id : DungeonHandler.worlds.keySet()) {
            DungeonHandler.reloadConfig(id);
        }
    }

    /** @return A random dungeon ID or null if none were found */
    public static String getRandomDungeonID() {
        final int size = DungeonHandler.worlds.size();
        if (size == 0) {
            return null;
        }

        return (String) DungeonHandler.worlds.keySet().toArray()[Main.getRandom().nextInt(size)];
    }

    /** @return A set of all registered dungeon IDs */
    public static Set<String> getDungeonIDs() {
        return DungeonHandler.worlds.keySet();
    }

    /** @return A collection of all registered dungeon worlds */
    public static Collection<DungeonWorld> getDungeonWorlds() {
        return DungeonHandler.worlds.values();
    }
}
