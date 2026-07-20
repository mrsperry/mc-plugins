package com.mrjoshuasperry.worldclusters.world;

import org.bukkit.World;

/**
 * A world's part in its cluster's vanilla progression, used to route portals
 * within the cluster instead of across it.
 *
 * <p>
 * A cluster has at most one world per role. A cluster with no {@link #NETHER}
 * simply has inert nether portals.
 */
public enum WorldRole {
    OVERWORLD,
    NETHER,
    END,
    /** Not part of portal routing — a standalone build or testing world. */
    NONE;

    public static WorldRole fromName(String name, WorldRole fallback) {
        if (name == null) {
            return fallback;
        }

        try {
            return WorldRole.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    /** The role that best matches a world's dimension, for defaulting. */
    public static WorldRole fromEnvironment(World.Environment environment) {
        return switch (environment) {
            case NORMAL -> OVERWORLD;
            case NETHER -> NETHER;
            case THE_END -> END;
            default -> NONE;
        };
    }
}
