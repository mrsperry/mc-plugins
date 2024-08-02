package com.mrjoshuasperry.miniadditions.additions.woodpile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class WoodPile {
    private final List<Block> fuel;
    private final List<Block> covering;
    private List<Block> visited;
    static List<Material> validCovering = Arrays.asList(Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.MYCELIUM);
    static List<Material> validFuel = Arrays.asList(Material.ACACIA_LOG, Material.BIRCH_LOG, Material.DARK_OAK_LOG,
            Material.JUNGLE_LOG, Material.OAK_LOG, Material.SPRUCE_LOG);

    public WoodPile() {
        fuel = new ArrayList<>();
        covering = new ArrayList<>();
        visited = new ArrayList<>();
    }

    public void addFuelBlock(Block block) {
        this.fuel.add(block);
    }

    public void addCoveringBlock(Block block) {
        this.covering.add(block);
    }

    public boolean checkValid(Block start) {
        visited = new ArrayList<>();

        if (isValidCovering(start)) { // find fuel;
            if (isValidFuel(start.getRelative(BlockFace.UP))) {
                return recursiveCheck(start.getRelative(BlockFace.UP));
            }
            if (isValidFuel(start.getRelative(BlockFace.DOWN))) {
                return recursiveCheck(start.getRelative(BlockFace.DOWN));
            }
            if (isValidFuel(start.getRelative(BlockFace.EAST))) {
                return recursiveCheck(start.getRelative(BlockFace.EAST));
            }
            if (isValidFuel(start.getRelative(BlockFace.WEST))) {
                return recursiveCheck(start.getRelative(BlockFace.WEST));
            }
            if (isValidFuel(start.getRelative(BlockFace.NORTH))) {
                return recursiveCheck(start.getRelative(BlockFace.NORTH));
            }
            if (isValidFuel(start.getRelative(BlockFace.SOUTH))) {
                return recursiveCheck(start.getRelative(BlockFace.SOUTH));
            }
        }

        return false;
    }

    private boolean recursiveCheck(Block current) {
        if (this.visited.contains(current)) {
            return true;
        }

        if (!isValidCovering(current) && !isValidFuel(current)) {
            return false;
        }

        this.visited.add(current);

        if (isValidFuel(current)) {
            this.fuel.add(current);
            boolean result = recursiveCheck(current.getRelative(BlockFace.UP));
            result = result && recursiveCheck(current.getRelative(BlockFace.DOWN));
            result = result && recursiveCheck(current.getRelative(BlockFace.EAST));
            result = result && recursiveCheck(current.getRelative(BlockFace.WEST));
            result = result && recursiveCheck(current.getRelative(BlockFace.SOUTH));
            result = result && recursiveCheck(current.getRelative(BlockFace.NORTH));

            return result;
        } else if (current.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
            this.covering.add(current);
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
        return validFuel.contains(block.getType());
    }

    public static boolean isValidCovering(Block block) {
        return validCovering.contains(block.getType());
    }

}
