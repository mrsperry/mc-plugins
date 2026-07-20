package com.mrjoshuasperry.worldclusters.cluster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.mrjoshuasperry.worldclusters.world.ManagedWorld;
import com.mrjoshuasperry.worldclusters.world.WorldRole;

/**
 * The world-to-cluster map, loaded from config.
 *
 * <p>
 * Any world not named in the config falls into an implicit {@link #DEFAULT_ID}
 * cluster, so a fresh install behaves exactly like vanilla and forgetting to
 * register a world can't break anything.
 */
public class ClusterRegistry {
    public static final String DEFAULT_ID = "default";

    private final Logger logger;

    private final Map<String, Cluster> clusters = new LinkedHashMap<>();
    private final Map<String, ManagedWorld> worlds = new LinkedHashMap<>();
    /** Lowercased world name to cluster id, for lookups on the hot path. */
    private final Map<String, String> worldToCluster = new LinkedHashMap<>();

    public ClusterRegistry(Logger logger) {
        this.logger = logger;
        this.clusters.put(DEFAULT_ID, defaultCluster());
    }

    private static Cluster defaultCluster() {
        return new Cluster(DEFAULT_ID, "Default", GameMode.SURVIVAL, Set.of(), List.of());
    }

    /** Replaces everything from a {@code clusters:} section. */
    public void load(ConfigurationSection clustersSection) {
        this.clusters.clear();
        this.worlds.clear();
        this.worldToCluster.clear();
        this.clusters.put(DEFAULT_ID, defaultCluster());

        if (clustersSection == null) {
            return;
        }

        for (String clusterId : clustersSection.getKeys(false)) {
            ConfigurationSection section = clustersSection.getConfigurationSection(clusterId);
            if (section == null) {
                continue;
            }

            Cluster cluster = Cluster.fromConfig(clusterId, section);
            this.clusters.put(clusterId, cluster);

            ConfigurationSection worldsSection = section.getConfigurationSection("worlds");
            if (worldsSection == null) {
                continue;
            }

            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                if (worldSection == null) {
                    continue;
                }

                // A world in two clusters has no sane answer for which boundary
                // applies, so refuse it loudly rather than silently taking one.
                String existing = this.worldToCluster.get(worldName.toLowerCase());
                if (existing != null) {
                    this.logger.severe("World '" + worldName + "' is in both cluster '" + existing
                            + "' and '" + clusterId + "'; keeping '" + existing + "' and ignoring the second.");
                    cluster.removeWorld(worldName);
                    continue;
                }

                this.worlds.put(worldName.toLowerCase(), ManagedWorld.fromConfig(worldName, worldSection));
                this.worldToCluster.put(worldName.toLowerCase(), clusterId);
            }
        }
    }

    /** Writes the whole registry back into a {@code clusters:} section. */
    public void save(ConfigurationSection clustersSection) {
        // Clearing a key drops the comments attached to it, so the shipped
        // config's annotations would be stripped the first time /wc create ran.
        // Snapshot them and put back the ones whose key still exists.
        Map<String, List<String>> comments = new LinkedHashMap<>();
        for (String path : clustersSection.getKeys(true)) {
            List<String> attached = clustersSection.getComments(path);
            if (!attached.isEmpty()) {
                comments.put(path, attached);
            }
        }

        for (String key : clustersSection.getKeys(false)) {
            clustersSection.set(key, null);
        }

        for (Cluster cluster : this.clusters.values()) {
            // The implicit default cluster is a runtime concept; persisting it
            // would turn unregistered worlds into registered ones.
            if (DEFAULT_ID.equals(cluster.getId())) {
                continue;
            }

            ConfigurationSection section = clustersSection.createSection(cluster.getId());
            cluster.writeTo(section);

            ConfigurationSection worldsSection = section.createSection("worlds");
            for (String worldName : cluster.getWorlds()) {
                ManagedWorld world = this.worlds.get(worldName.toLowerCase());
                if (world != null) {
                    world.writeTo(worldsSection.createSection(worldName));
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : comments.entrySet()) {
            if (clustersSection.contains(entry.getKey())) {
                clustersSection.setComments(entry.getKey(), entry.getValue());
            }
        }
    }

    /** The cluster id for a world name, never null. */
    public String getClusterId(String worldName) {
        return this.worldToCluster.getOrDefault(worldName.toLowerCase(), DEFAULT_ID);
    }

    public String getClusterId(World world) {
        return this.getClusterId(world.getName());
    }

    /** The cluster for a world, never null. */
    public Cluster getCluster(World world) {
        return this.clusters.get(this.getClusterId(world));
    }

    public Cluster getClusterById(String id) {
        return this.clusters.get(id);
    }

    /** Whether moving between these two worlds crosses a boundary. */
    public boolean isBoundary(World from, World to) {
        return !this.getClusterId(from).equals(this.getClusterId(to));
    }

    /**
     * The world filling a role in a cluster, or null. Used to route portals
     * within the cluster.
     */
    public ManagedWorld getWorldWithRole(String clusterId, WorldRole role) {
        Cluster cluster = this.clusters.get(clusterId);
        if (cluster == null) {
            return null;
        }

        for (String worldName : cluster.getWorlds()) {
            ManagedWorld world = this.worlds.get(worldName.toLowerCase());
            if (world != null && world.getRole() == role) {
                return world;
            }
        }

        return null;
    }

    public ManagedWorld getManagedWorld(String worldName) {
        return this.worlds.get(worldName.toLowerCase());
    }

    public List<ManagedWorld> getManagedWorlds() {
        return List.copyOf(this.worlds.values());
    }

    public List<Cluster> getClusters() {
        return List.copyOf(this.clusters.values());
    }

    public Cluster createCluster(String id) {
        Cluster cluster = new Cluster(id, id, GameMode.SURVIVAL, Set.of(), List.of());
        this.clusters.put(id, cluster);
        return cluster;
    }

    /**
     * Removes a cluster and unregisters its worlds. The worlds stay on disk and
     * loaded; they simply fall back to the implicit default cluster.
     */
    public void deleteCluster(String id) {
        if (DEFAULT_ID.equals(id)) {
            throw new IllegalArgumentException("The default cluster cannot be deleted");
        }

        Cluster cluster = this.clusters.remove(id);
        if (cluster == null) {
            return;
        }

        // Drop the ManagedWorld entries too. Leaving them would half-remove the
        // world: still visible to the menu and to role lookups, but treated as
        // unregistered by the boundary logic.
        for (String worldName : cluster.getWorlds()) {
            this.worldToCluster.remove(worldName.toLowerCase());
            this.worlds.remove(worldName.toLowerCase());
        }
    }

    /** Registers a world into a cluster, moving it out of any previous one. */
    public void assign(ManagedWorld world, String clusterId) {
        Cluster target = this.clusters.get(clusterId);
        if (target == null) {
            throw new IllegalArgumentException("No such cluster: " + clusterId);
        }

        String previous = this.worldToCluster.get(world.getName().toLowerCase());
        if (previous != null) {
            Cluster old = this.clusters.get(previous);
            if (old != null) {
                old.removeWorld(world.getName());
            }
        }

        this.worlds.put(world.getName().toLowerCase(), world);
        this.worldToCluster.put(world.getName().toLowerCase(), clusterId);
        target.addWorld(world.getName());
    }

    /** Removes a world from management entirely. */
    public void unregister(String worldName) {
        String clusterId = this.worldToCluster.remove(worldName.toLowerCase());
        if (clusterId != null) {
            Cluster cluster = this.clusters.get(clusterId);
            if (cluster != null) {
                cluster.removeWorld(worldName);
            }
        }

        this.worlds.remove(worldName.toLowerCase());
    }

    /** Every cluster id that grants at least one permission node. */
    public List<String> getAllGrantedPermissions() {
        List<String> nodes = new ArrayList<>();
        for (Cluster cluster : this.clusters.values()) {
            nodes.addAll(cluster.getPermissions());
        }
        return nodes;
    }
}
