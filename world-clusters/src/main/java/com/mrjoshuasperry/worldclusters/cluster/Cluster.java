package com.mrjoshuasperry.worldclusters.cluster;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A group of worlds that share one player state.
 *
 * <p>
 * Crossing between two clusters is a <b>boundary</b>: inventory, XP, health and
 * everything else is swapped out. Moving between worlds <i>within</i> a cluster
 * changes nothing, which is what keeps overworld/nether/end travel vanilla.
 *
 * <p>
 * Membership, not game mode, is what separates worlds — a cluster can hold
 * worlds of mixed game modes, and two survival worlds in different clusters
 * still get a boundary between them.
 */
public class Cluster {
    private final String id;

    private String displayName;
    private GameMode defaultGameMode;
    private final Set<String> permissions;
    private final List<String> worlds;

    public Cluster(String id, String displayName, GameMode defaultGameMode,
            Set<String> permissions, List<String> worlds) {
        this.id = id;
        this.displayName = displayName;
        this.defaultGameMode = defaultGameMode;
        this.permissions = new LinkedHashSet<>(permissions);
        this.worlds = new ArrayList<>(worlds);
    }

    public static Cluster fromConfig(String id, ConfigurationSection section) {
        ConfigurationSection worldsSection = section.getConfigurationSection("worlds");

        return new Cluster(
                id,
                section.getString("display-name", id),
                parseGameMode(section.getString("default-gamemode")),
                new LinkedHashSet<>(section.getStringList("permissions")),
                worldsSection == null ? List.of() : new ArrayList<>(worldsSection.getKeys(false)));
    }

    public void writeTo(ConfigurationSection section) {
        section.set("display-name", this.displayName);
        section.set("default-gamemode", this.defaultGameMode.name());
        section.set("permissions", new ArrayList<>(this.permissions));
    }

    private static GameMode parseGameMode(String name) {
        if (name == null) {
            return GameMode.SURVIVAL;
        }

        try {
            return GameMode.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return GameMode.SURVIVAL;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** The game mode a player gets on their first ever entry to this cluster. */
    public GameMode getDefaultGameMode() {
        return this.defaultGameMode;
    }

    public void setDefaultGameMode(GameMode defaultGameMode) {
        this.defaultGameMode = defaultGameMode;
    }

    /** Permission nodes granted while a player is inside this cluster. */
    public Set<String> getPermissions() {
        return Set.copyOf(this.permissions);
    }

    public void addPermission(String node) {
        this.permissions.add(node);
    }

    public void removePermission(String node) {
        this.permissions.remove(node);
    }

    /** World names in this cluster, in the order they appear in the menu. */
    public List<String> getWorlds() {
        return List.copyOf(this.worlds);
    }

    public void addWorld(String worldName) {
        if (!this.worlds.contains(worldName)) {
            this.worlds.add(worldName);
        }
    }

    public void removeWorld(String worldName) {
        this.worlds.remove(worldName);
    }

    public boolean contains(String worldName) {
        return this.worlds.contains(worldName);
    }
}
