package com.mrjoshuasperry.worldclusters.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;

/**
 * Creates, loads, unloads and deletes worlds.
 *
 * <p>
 * Every method returns a {@link Result} rather than throwing, because all of
 * these are driven by commands where the failure reason has to reach the player.
 */
public class WorldManager {
    /**
     * @param success whether the operation happened
     * @param message why not, when it didn't
     */
    public record Result(boolean success, String message) {
        public static Result ok() {
            return new Result(true, null);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    private final JavaPlugin plugin;
    private final ClusterRegistry registry;

    public WorldManager(JavaPlugin plugin, ClusterRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /** Loads every managed world flagged auto-load that isn't up already. */
    public void loadManagedWorlds() {
        for (ManagedWorld world : this.registry.getManagedWorlds()) {
            if (!world.isAutoLoad() || world.isLoaded()) {
                continue;
            }

            if (!worldFolderExists(world.getName())) {
                this.plugin.getLogger().warning("Managed world '" + world.getName()
                        + "' has no folder on disk; skipping. Create it with /wc create.");
                continue;
            }

            this.load(world);
        }
    }

    /** Brings a managed world up from disk. */
    public Result load(ManagedWorld world) {
        if (world.isLoaded()) {
            return Result.fail("'" + world.getName() + "' is already loaded.");
        }

        WorldCreator creator = new WorldCreator(world.getName())
                .environment(world.getEnvironment())
                .type(world.getWorldType());

        if (world.getGenerator() != null) {
            creator.generator(world.getGenerator());
        }
        if (world.getSeed() != null) {
            creator.seed(world.getSeed());
        }

        return creator.createWorld() == null
                ? Result.fail("Bukkit refused to load '" + world.getName() + "'; check the server log.")
                : Result.ok();
    }

    /**
     * Creates a brand new world and registers it into a cluster.
     *
     * @param clusterId the cluster it joins, which decides its boundary
     */
    public Result create(ManagedWorld world, String clusterId) {
        if (Bukkit.getWorld(world.getName()) != null) {
            return Result.fail("A world named '" + world.getName() + "' is already loaded.");
        }
        if (worldFolderExists(world.getName())) {
            return Result.fail("A folder named '" + world.getName() + "' already exists; use /wc import instead.");
        }
        if (this.registry.getClusterById(clusterId) == null) {
            return Result.fail("No such cluster: '" + clusterId + "'.");
        }

        Result loaded = this.load(world);
        if (!loaded.success()) {
            return loaded;
        }

        this.registry.assign(world, clusterId);
        return Result.ok();
    }

    /** Registers a world folder that already exists on disk. */
    public Result importWorld(ManagedWorld world, String clusterId) {
        if (!worldFolderExists(world.getName())) {
            return Result.fail("No folder named '" + world.getName() + "' in the server directory.");
        }
        if (this.registry.getClusterById(clusterId) == null) {
            return Result.fail("No such cluster: '" + clusterId + "'.");
        }

        if (!world.isLoaded()) {
            Result loaded = this.load(world);
            if (!loaded.success()) {
                return loaded;
            }
        }

        this.registry.assign(world, clusterId);
        return Result.ok();
    }

    /**
     * Unloads a world, saving it first.
     *
     * <p>
     * Players inside are moved to their cluster's spawn — Bukkit refuses to
     * unload an occupied world, and dropping the request would leave the caller
     * wondering why nothing happened.
     */
    public Result unload(String name) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            return Result.fail("'" + name + "' is not loaded.");
        }
        if (isPrimary(world)) {
            return Result.fail("'" + name + "' is the server's primary world and cannot be unloaded.");
        }

        this.evacuate(world);

        return Bukkit.unloadWorld(world, true)
                ? Result.ok()
                : Result.fail("Bukkit refused to unload '" + name + "'; check the server log.");
    }

    /**
     * Unloads a world and deletes its folder. Irreversible.
     */
    public Result delete(String name) {
        World world = Bukkit.getWorld(name);

        if (world != null) {
            if (isPrimary(world)) {
                return Result.fail("'" + name + "' is the server's primary world and cannot be deleted.");
            }

            Result unloaded = this.unload(name);
            if (!unloaded.success()) {
                return unloaded;
            }
        }

        File folder = worldFolder(name);
        if (!folder.isDirectory()) {
            this.registry.unregister(name);
            return Result.fail("No folder named '" + name + "' to delete; unregistered it anyway.");
        }

        if (!deleteRecursively(folder.toPath(), this.plugin)) {
            return Result.fail("Could not fully delete '" + name + "'; check the server log.");
        }

        this.registry.unregister(name);
        return Result.ok();
    }

    /** Moves everyone out of a world to their own cluster's spawn. */
    public void evacuate(World world) {
        // Resolved once: the destination depends only on the world being emptied,
        // and resolving it walks the cluster's world list.
        Location destination = this.safeSpawnFor(world);

        for (Player player : List.copyOf(world.getPlayers())) {
            player.teleport(destination);
        }
    }

    /**
     * Where to send a player displaced from a world: their cluster's overworld
     * spawn if it has one, otherwise the server's primary world.
     */
    public Location safeSpawnFor(World world) {
        String clusterId = this.registry.getClusterId(world);
        ManagedWorld overworld = this.registry.getWorldWithRole(clusterId, WorldRole.OVERWORLD);

        if (overworld != null && overworld.isLoaded() && !overworld.getName().equals(world.getName())) {
            return overworld.getWorld().getSpawnLocation();
        }

        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    /** The first world Bukkit loaded, which it will not let us unload. */
    public static boolean isPrimary(World world) {
        List<World> worlds = Bukkit.getWorlds();
        return !worlds.isEmpty() && worlds.get(0).equals(world);
    }

    private static File worldFolder(String name) {
        return new File(Bukkit.getWorldContainer(), name);
    }

    private static boolean worldFolderExists(String name) {
        return worldFolder(name).isDirectory();
    }

    private static boolean deleteRecursively(Path root, JavaPlugin plugin) {
        try (Stream<Path> paths = Files.walk(root)) {
            // Deepest first, so a directory is always empty by the time we reach it.
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();

            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }

            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete world folder " + root, ex);
            return false;
        }
    }
}
