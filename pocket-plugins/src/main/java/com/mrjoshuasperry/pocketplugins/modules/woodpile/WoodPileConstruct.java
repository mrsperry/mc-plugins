package com.mrjoshuasperry.pocketplugins.modules.woodpile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/** @author TimPCunningham */
public class WoodPileConstruct {
    private static final BlockFace[] SPREAD_FACES = { BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST,
            BlockFace.NORTH, BlockFace.SOUTH };

    private final List<Block> fuel;
    private final List<Block> covering;
    private Set<Block> visited;

    public WoodPileConstruct() {
        fuel = new ArrayList<>();
        covering = new ArrayList<>();
        visited = new HashSet<>();
    }

    public void addFuelBlock(Block block) {
        this.fuel.add(block);
    }

    public void addCoveringBlock(Block block) {
        this.covering.add(block);
    }

    public boolean checkValid(Block start) {
        visited = new HashSet<>();

        if (isValidCovering(start)) { // find fuel;
            for (BlockFace face : SPREAD_FACES) {
                Block neighbour = start.getRelative(face);
                if (isValidFuel(neighbour)) {
                    return spreadFromFuel(neighbour);
                }
            }
        }

        return false;
    }

    /**
     * Walks the pile outwards from a fuel block, collecting the fuel and the
     * covering that encloses it. Only fuel spreads further, so the covering forms
     * the boundary; anything else reachable means the pile is not sealed.
     *
     * <p>
     * The search is iterative rather than recursive because a pile is only bounded
     * by how much the player stacked, and one call per block overflowed the stack
     * on large ones.
     */
    private boolean spreadFromFuel(Block start) {
        Deque<Block> pending = new ArrayDeque<>();
        pending.push(start);

        while (!pending.isEmpty()) {
            Block current = pending.pop();

            if (this.visited.contains(current)) {
                continue;
            }

            boolean isFuel = isValidFuel(current);
            if (!isFuel && !isValidCovering(current)) {
                return false;
            }

            this.visited.add(current);

            if (isFuel) {
                this.fuel.add(current);
                for (BlockFace face : SPREAD_FACES) {
                    pending.push(current.getRelative(face));
                }
            } else if (current.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
                this.covering.add(current);
            }
        }

        return true;
    }

    public int getFuelSize() {
        return this.fuel.size();
    }

    public void convertFuel() {
        for (Block block : this.fuel) {
            block.setType(Material.COAL_BLOCK);
        }
    }

    public void showBurning(Particle particle) {
        for (Block block : this.covering) {
            block.getWorld().spawnParticle(particle, block.getLocation().clone().add(0.5, 1, 0.5), 1, 0.5, 0, 0.5, 0);
        }
    }

    public boolean contains(Block block) {
        return this.visited.contains(block);
    }

    public static boolean isValidFuel(Block block) {
        // Every overworld wood type, including its bark and stripped variants. The
        // nether stems are excluded by the tag itself, matching vanilla: they don't
        // burn, so they shouldn't char either
        return Tag.LOGS_THAT_BURN.isTagged(block.getType());
    }

    public static boolean isValidCovering(Block block) {
        // The vanilla dirt tag, for the same reason isValidFuel uses a tag: the
        // hand-listed version predated rooted dirt, mud, muddy mangrove roots and the
        // moss blocks, so sealing a pile with any of those silently failed to build one
        return Tag.DIRT.isTagged(block.getType());
    }

}
