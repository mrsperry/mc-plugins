package com.mrjoshuasperry.mcutils.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

/**
 * Spawnable mobs, split into hostile and neutral.
 *
 * <p>
 * Derived from the {@link EntityType} registry rather than hand-listed, so a new
 * Minecraft version's mobs are picked up on the next build instead of silently
 * falling out of every caller. The hand-maintained lists this replaced had drifted
 * 24 mobs behind by Paper 26.1.2 — everything from Allay through Warden.
 *
 * <p>
 * {@link Enemy} is the split: it reproduced the old hand-built hostile/neutral
 * classification exactly, including the entries that are not {@code Monster}s
 * (Ghast, Hoglin, Magma Cube, Phantom, Shulker, Slime). {@link Mob} is what makes
 * "alive and spawnable" mean an actual creature — it excludes Armor Stands and
 * Mannequins, which are {@code LivingEntity} but not mobs.
 */
public class EntityTypes {
    private static final List<EntityType> hostile = new ArrayList<>();
    private static final List<EntityType> neutral = new ArrayList<>();

    static {
        for (EntityType type : EntityType.values()) {
            Class<?> entityClass = type.getEntityClass();
            if (!type.isAlive() || !type.isSpawnable() || entityClass == null
                    || !Mob.class.isAssignableFrom(entityClass)) {
                continue;
            }

            if (Enemy.class.isAssignableFrom(entityClass)) {
                hostile.add(type);
            } else {
                neutral.add(type);
            }
        }
    }

    private EntityTypes() {
    }

    public static List<EntityType> getAllTypes() {
        List<EntityType> types = new ArrayList<>(hostile);
        types.addAll(neutral);

        return types;
    }

    // Defensive copies — callers used to receive the backing lists directly, so a
    // caller mutating the result would corrupt them for every other caller.
    public static List<EntityType> getHostileTypes() {
        return new ArrayList<>(hostile);
    }

    public static List<EntityType> getNeutralTypes() {
        return new ArrayList<>(neutral);
    }
}
