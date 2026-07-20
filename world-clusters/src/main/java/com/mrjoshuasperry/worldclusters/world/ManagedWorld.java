package com.mrjoshuasperry.worldclusters.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A world under this plugin's management, plus everything needed to recreate it
 * on a fresh server: environment, world type and seed are persisted so a world
 * made with {@code /wc create} comes back after a restart.
 */
public class ManagedWorld {
    private final String name;
    private final World.Environment environment;
    private final WorldType worldType;
    private final String generator;
    private final Long seed;

    private WorldRole role;
    private DisplayItem displayItem;
    private boolean autoLoad;

    public ManagedWorld(String name, World.Environment environment, WorldType worldType, String generator,
            Long seed, WorldRole role, DisplayItem displayItem, boolean autoLoad) {
        this.name = name;
        this.environment = environment;
        this.worldType = worldType;
        this.generator = generator;
        this.seed = seed;
        this.role = role;
        this.displayItem = displayItem;
        this.autoLoad = autoLoad;
    }

    public static ManagedWorld fromConfig(String name, ConfigurationSection section) {
        World.Environment environment = parseEnvironment(section.getString("environment"));
        WorldType worldType = parseWorldType(section.getString("type"));

        return new ManagedWorld(
                name,
                environment,
                worldType,
                section.getString("generator"),
                // Absent means "let Bukkit pick", which is different from seed 0.
                section.contains("seed") ? section.getLong("seed") : null,
                WorldRole.fromName(section.getString("role"), WorldRole.fromEnvironment(environment)),
                DisplayItem.fromConfig(section.getConfigurationSection("display")),
                section.getBoolean("auto-load", true));
    }

    public void writeTo(ConfigurationSection section) {
        section.set("environment", this.environment.name());
        section.set("type", this.worldType.name());
        section.set("generator", this.generator);
        section.set("seed", this.seed);
        section.set("role", this.role.name());
        section.set("auto-load", this.autoLoad);

        ConfigurationSection display = section.getConfigurationSection("display");
        if (display == null) {
            display = section.createSection("display");
        }
        this.displayItem.writeTo(display);
    }

    private static World.Environment parseEnvironment(String name) {
        if (name == null) {
            return World.Environment.NORMAL;
        }

        try {
            return World.Environment.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return World.Environment.NORMAL;
        }
    }

    private static WorldType parseWorldType(String name) {
        if (name == null) {
            return WorldType.NORMAL;
        }

        WorldType type = WorldType.getByName(name.trim().toUpperCase());
        return type == null ? WorldType.NORMAL : type;
    }

    /** The live world, or null if it is not loaded. */
    public World getWorld() {
        return Bukkit.getWorld(this.name);
    }

    public boolean isLoaded() {
        return this.getWorld() != null;
    }

    public String getName() {
        return this.name;
    }

    public World.Environment getEnvironment() {
        return this.environment;
    }

    public WorldType getWorldType() {
        return this.worldType;
    }

    public String getGenerator() {
        return this.generator;
    }

    public Long getSeed() {
        return this.seed;
    }

    public WorldRole getRole() {
        return this.role;
    }

    public void setRole(WorldRole role) {
        this.role = role;
    }

    public DisplayItem getDisplayItem() {
        return this.displayItem;
    }

    public void setDisplayItem(DisplayItem displayItem) {
        this.displayItem = displayItem;
    }

    public boolean isAutoLoad() {
        return this.autoLoad;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }
}
