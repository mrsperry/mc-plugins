package com.mrjoshuasperry.worldclusters.profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Per-player, per-cluster profile storage in {@code playerdata/<uuid>.yml}.
 *
 * <p>
 * Profiles for online players are held in memory and written through to disk, so
 * the boundary swap itself never blocks on IO. Writes are async; the in-memory
 * map is the source of truth while a player is connected.
 */
public class ProfileStore {
    /** How long shutdown waits for outstanding profile writes. */
    private static final int WRITE_SHUTDOWN_SECONDS = 10;

    private final JavaPlugin plugin;
    private final File directory;

    /**
     * Writes run on one thread so that successive saves of the same file land in
     * the order they were made. Bukkit's async pool gives no such guarantee.
     */
    private final ExecutorService writer = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, "world-clusters-profile-writer"));

    /** uuid to cluster id to profile, for online players. */
    private final Map<UUID, Map<String, PlayerProfile>> cache = new HashMap<>();

    /**
     * The cluster each player's live state belongs to. Persisted so that if a
     * player's world is deleted while they are offline and vanilla drops them at
     * the main spawn, we can tell their inventory belongs to the old cluster and
     * swap it out rather than letting it leak across the boundary.
     */
    private final Map<UUID, String> currentCluster = new HashMap<>();

    public ProfileStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "playerdata");

        if (!this.directory.exists() && !this.directory.mkdirs()) {
            plugin.getLogger().severe("Could not create the playerdata directory; profiles will not persist.");
        }
    }

    /** Reads every cluster's profile for a player into memory. Call on join. */
    public void load(UUID uuid) {
        Map<String, PlayerProfile> profiles = new HashMap<>();
        File file = this.fileFor(uuid);

        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection clusters = config.getConfigurationSection("clusters");

            if (clusters != null) {
                for (String clusterId : clusters.getKeys(false)) {
                    ConfigurationSection section = clusters.getConfigurationSection(clusterId);
                    if (section != null) {
                        profiles.put(clusterId, PlayerProfile.readFrom(section));
                    }
                }
            }

            String current = config.getString("current-cluster");
            if (current != null) {
                this.currentCluster.put(uuid, current);
            }
        }

        this.cache.put(uuid, profiles);
    }

    /** Drops a player's cached profiles. Call on quit, after a final save. */
    public void unload(UUID uuid) {
        this.cache.remove(uuid);
        this.currentCluster.remove(uuid);
    }

    /** The cluster a player's live state belongs to, or null if unknown. */
    public String getCurrentCluster(UUID uuid) {
        return this.currentCluster.get(uuid);
    }

    public void setCurrentCluster(UUID uuid, String clusterId) {
        this.currentCluster.put(uuid, clusterId);
    }

    public PlayerProfile get(UUID uuid, String clusterId) {
        return this.cache.getOrDefault(uuid, Map.of()).get(clusterId);
    }

    public boolean has(UUID uuid, String clusterId) {
        return this.get(uuid, clusterId) != null;
    }

    /** Stores a profile in memory. Call {@link #flush} once the swap is done. */
    public void put(UUID uuid, String clusterId, PlayerProfile profile) {
        this.cache.computeIfAbsent(uuid, key -> new HashMap<>()).put(clusterId, profile);
    }

    /**
     * Writes a player's profiles to disk.
     *
     * <p>
     * Mutators only touch memory; persisting is an explicit step so that a
     * boundary crossing — which updates both a profile and the current cluster —
     * produces exactly one write. Two writes for one crossing would race: they
     * carry different {@code current-cluster} values, and if the older landed
     * last the next login would see a cluster the player isn't in and perform a
     * spurious swap.
     */
    public void flush(UUID uuid) {
        // Serialize on the main thread — ItemStack and Location are not safe to
        // touch off it — then hand the finished YAML text to the writer.
        String contents = this.serialize(uuid);
        if (contents == null) {
            return;
        }

        File file = this.fileFor(uuid);

        if (this.plugin.isEnabled() && !this.writer.isShutdown()) {
            // A single-threaded executor rather than the Bukkit async pool: writes
            // to one file must land in the order they were made, and the pool
            // gives no such guarantee.
            this.writer.execute(() -> this.write(file, contents));
        } else {
            // During shutdown the executor is closing; write inline rather than
            // silently dropping the last save.
            this.write(file, contents);
        }
    }

    private String serialize(UUID uuid) {
        Map<String, PlayerProfile> profiles = this.cache.get(uuid);
        if (profiles == null) {
            return null;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("current-cluster", this.currentCluster.get(uuid));
        ConfigurationSection clusters = config.createSection("clusters");

        for (Map.Entry<String, PlayerProfile> entry : profiles.entrySet()) {
            entry.getValue().writeTo(clusters.createSection(entry.getKey()));
        }

        return config.saveToString();
    }

    private void write(File file, String contents) {
        try {
            Files.writeString(file.toPath(), contents);
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save profiles to " + file.getName(), ex);
        }
    }

    /** Flushes every cached player to disk and stops the writer. Call on disable. */
    public void saveAll() {
        for (UUID uuid : this.cache.keySet()) {
            this.flush(uuid);
        }

        this.writer.shutdown();
        try {
            // Profiles are the one thing worth blocking shutdown for; losing a
            // write here loses a player's inventory.
            if (!this.writer.awaitTermination(WRITE_SHUTDOWN_SECONDS, TimeUnit.SECONDS)) {
                this.plugin.getLogger().warning("Timed out waiting for profile writes to finish.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private File fileFor(UUID uuid) {
        return new File(this.directory, uuid + ".yml");
    }
}
