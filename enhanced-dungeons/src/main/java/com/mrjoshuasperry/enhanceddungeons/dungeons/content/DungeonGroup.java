package com.mrjoshuasperry.enhanceddungeons.dungeons.content;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class DungeonGroup {
    /** The location of the group proximity triggers */
    private Location trigger;
    /** The number of blocks away a player must be for this group to spawn */
    private int proximity;
    /** The number of blocks away a player must be for this group to have AI */
    private int aiProximity;
    /** A list of all mobs in this group */
    private Set<Set<DungeonMob>> mobs;

    /** A list of entities that have spawned */
    private final Set<LivingEntity> entities;
    /** If this group has been spawned */
    private boolean hasSpawned;

    public DungeonGroup() {
        this.trigger = null;
        this.proximity = -1;
        this.aiProximity = -1;
        this.mobs = null;

        this.entities = new HashSet<>();
        this.hasSpawned = false;
    }

    /**
     * Checks all proximity detectors for the group's trigger location
     * @param player The player that should be checked against
     */
    public void checkProximities(final Player player) {
        if (this.proximity == -1) {
            this.spawn();
        }

        if (this.aiProximity == -1) {
            this.trigger();
        }

        if (player != null) {
            final double distance = player.getLocation().distance(this.trigger);

            if (distance <= this.proximity) {
                this.spawn();
            }

            if (distance <= this.aiProximity) {
                this.trigger();
            }
        }
    }

    /** Spawns the mob group */
    private void spawn() {
        if (this.hasSpawned) {
            return;
        }

        this.hasSpawned = true;

        for (final Set<DungeonMob> template : this.mobs) {
            for (final DungeonMob mob : template) {
                final Entity entity = mob.spawn();
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                final LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.setAI(this.aiProximity == -1);

                this.entities.add(livingEntity);
            }
        }
    }

    /** Adds AI to every living entity that has been spawned */
    private void trigger() {
        for (final LivingEntity entity : this.entities) {
            entity.setAI(true);
        }
    }

    /** Removes all mobs spawned by this group */
    public void remove() {
        for (final Set<DungeonMob> template : this.mobs) {
            for (final DungeonMob mob : template) {
                mob.remove();
            }
        }

        this.hasSpawned = false;
    }

    /** @param trigger The location of the group proximity triggers */
    public void setTrigger(final Location trigger) {
        this.trigger = trigger;
    }

    /** @param proximity The number of blocks away a player must be for this group to spawn */
    public void setProximity(final int proximity) {
        this.proximity = proximity;
    }

    /** @param aiProximity The number of blocks away a player must be for this group to have AI */
    public void setAIProximity(final int aiProximity) {
        this.aiProximity = aiProximity;
    }

    /** @param mobs A list of all dungeon mobs this group contains */
    public void setMobs(final Set<Set<DungeonMob>> mobs) {
        this.mobs = mobs;
    }

    /** @return A set containing all mobs this group handles */
    public Set<DungeonMob> getMobs() {
        final Set<DungeonMob> mobs = new HashSet<>();
        for (final Set<DungeonMob> set : this.mobs) {
            mobs.addAll(set);
        }

        return mobs;
    }
}
