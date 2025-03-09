package com.mrjoshuasperry.enhanceddungeons.dungeons.content;

import org.bukkit.Effect;
import org.bukkit.Location;

public class DungeonEffect {
    /** The location the effect plays at */
    private final Location location;
    /** The effect to play */
    private final Effect effect;
    /** The number of blocks from the location a player must be for the effect to play (-1 plays instantly) */
    private final int proximity;

    /**
     * Creates a new DungeonEffect that will play when a player is within the proximity of the location
     * @param location The location the effect plays at
     * @param effect The effect to play
     * @param proximity The number of blocks from the location a player must be for the effect to play (-1 plays instantly)
     */
    public DungeonEffect(final Location location, final Effect effect, final int proximity) {
        this.location = location;
        this.effect = effect;
        this.proximity = proximity;
    }

    /** @return The location the effect plays at */
    public Location getLocation() {
        return this.location;
    }

    /** @return The effect to play */
    public Effect getEffect() {
        return this.effect;
    }

    /** @return The number of blocks from the location a player must be for the effect to play */
    public int getProximity() {
        return this.proximity;
    }
}
