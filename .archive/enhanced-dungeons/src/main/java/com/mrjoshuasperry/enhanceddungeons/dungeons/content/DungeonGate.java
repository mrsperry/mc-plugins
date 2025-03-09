package com.mrjoshuasperry.enhanceddungeons.dungeons.content;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class DungeonGate {
    /** The ID of this gate */
    private final String id;
    /** The start location of this gate */
    private final Location start;
    /** The end location of this gate */
    private final Location end;
    /** The material used to regenerate this gate */
    private final Material material;

    /** The world this gate resides in */
    private World world;
    /** A list of each row of blocks in this gate */
    private List<Set<Location>> blocks;
    /** The center location of this gate */
    private Location center;

    /** If this gate has triggered */
    private boolean hasTriggered;
    /** The Bukkit task ID assigned to this gate when opening */
    private int taskID;

    /**
     * Creates a new dungeon gate
     * @param id The ID of the gate
     * @param start The start location of the gate
     * @param end The end location of the gate
     */
    public DungeonGate(final String id, final Location start, final Location end, final Material material) {
        this.id = id;
        this.start = Utils.locationToBlockCoordinates(start);
        this.end = Utils.locationToBlockCoordinates(end);
        this.material = material;

        final World world = start.getWorld();
        if (world == null) {
            Utils.log(Level.SEVERE, "Gate world could not be found: " + this.start.toString());
            return;
        }
        this.world = world;

        this.calculateBlocks();

        this.hasTriggered = false;
        this.taskID = -1;
    }

    private void calculateBlocks() {
        this.blocks = new ArrayList<>();

        // Get the starting coordinates
        final int startX = start.getBlockX();
        final int startY = start.getBlockY();
        final int startZ = start.getBlockZ();

        // Get the ending coordinates
        final int endX = end.getBlockX();
        final int endY = end.getBlockY();
        final int endZ = end.getBlockZ();

        // Find the lower coordinate value between the start and end
        final int originX = Math.min(startX, endX);
        final int originY = Math.min(startY, endY);
        final int originZ = Math.min(startZ, endZ);

        // Get the total number of blocks to traverse in each direction
        final int rangeX = Math.abs(startX - endX) + 1;
        final int rangeY = Math.abs(startY - endY) + 1;
        final int rangeZ = Math.abs(startZ - endZ) + 1;

        // Set the center of the gate (used for sound)
        this.center = new Location(this.world,
                originX + (rangeX / 2f),
                originY + (rangeY / 2f),
                originZ + (rangeZ / 2f));

        // Go through each Y level
        for (int y = 0; y < rangeY; y++) {
            final Set<Location> row = new HashSet<>();

            // Add each block on this row
            for (int x = 0; x < rangeX; x++) {
                for (int z = 0; z < rangeZ; z++) {
                    row.add(new Location(this.world, originX + x, originY + y, originZ + z));
                }
            }

            this.blocks.add(row);
        }
    }

    /**
     * Triggers the gate to open
     * @return If the gate opened
     */
    public boolean trigger() {
        if (this.hasTriggered) {
            return false;
        }

        this.hasTriggered = true;

        // Start a new task to remove blocks
        final BukkitScheduler scheduler = Bukkit.getScheduler();
        this.taskID = scheduler.runTaskTimer(Main.getInstance(), new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                if (this.index == blocks.size()) {
                    Bukkit.getScheduler().cancelTask(taskID);
                    return;
                }

                // Play the sound at the center of the gate every Y level
                if (center != null) {
                    world.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1, 1);
                    scheduler.runTaskLater(Main.getInstance(), () ->
                            world.playSound(center, Sound.BLOCK_PISTON_CONTRACT, 1, 1), 3);
                }

                // Remove the blocks on this row
                for (final Location location : blocks.get(this.index)) {
                    world.getBlockAt(location).setType(Material.AIR);
                }

                this.index++;
            }
        }, 0, 7).getTaskId();

        return true;
    }

    /** Rebuilds this gate */
    public void regenerate() {
        this.hasTriggered = false;

        for (final Set<Location> rows : this.blocks) {
            for (final Location location : rows) {
                final Block block = this.world.getBlockAt(location);

                if (block.getType() != this.material) {
                    block.setType(this.material);
                }
            }
        }
    }

    /** @return The ID of this gate */
    public String getID() {
        return this.id;
    }
}
