package com.mrjoshuasperry.customgeneration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.mrjoshuasperry.mcutils.classes.Pair;

public class PopulatorUtils {
    private World world;

    private List<Pair<Material, Byte>> materials;

    public PopulatorUtils(World world, List<Pair<Material, Byte>> materials) {
        this.world = world;

        this.materials = materials;
    }

    public void setType(int x, int y, int z) {
        Pair<Material, Byte> selected = this.materials.get(Main.getRandom().nextInt(this.materials.size()));
        this.setType(x, y, z, selected.getKey(), selected.getValue());
    }

    public void setType(int x, int y, int z, Material type, byte data) {
        Block current = this.world.getBlockAt(x, y, z);
        current.setType(type);
        // current.setData(data);
    }

    public boolean setBlocks(List<Pair<Material, Byte>> replace, HashSet<Block> blocks) {
        return this.setBlocks(replace, blocks, 0);
    }

    public boolean setBlocks(List<Pair<Material, Byte>> replace, HashSet<Block> blocks, int offset) {
        for (Block block : blocks) {
            boolean found = false;
            for (Pair<Material, Byte> material : replace) {
                if ((block.getType() == material.getKey()) && (block.getData() == material.getValue())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        for (Block block : blocks) {
            this.setType(block.getX(), block.getY() - offset, block.getZ());
        }

        return true;
    }

    public Block getBase(World world, Random random, Chunk chunk) {
        return world.getHighestBlockAt(random.nextInt(16) + 16 * chunk.getX(), random.nextInt(16) + 16 * chunk.getZ());
    }

    public Block getBase(World world, Random random, Chunk chunk, int min, int max) {
        return world.getBlockAt(random.nextInt(16) + 16 * chunk.getX(), this.getNumber(random, min, max),
                random.nextInt(16) + 16 * chunk.getZ());
    }

    public boolean isValidMaterial(Block origin, List<Pair<Material, Byte>> materials) {
        Block below = origin.getRelative(0, -1, 0);

        for (Pair<Material, Byte> material : materials) {
            if ((below.getType() == material.getKey()) && (below.getData() == material.getValue())) {
                return true;
            }
        }

        return false;
    }

    public HashSet<Block> getVerticalCircle(Block origin, boolean direction, int radius) {
        HashSet<Block> blocks = new HashSet<>();

        int x = origin.getX();
        int y = origin.getY() + radius;
        int z = origin.getZ();

        for (double radian = 0.0D; radian < Math.PI * 2; radian += 0.01D) {
            double blockX = 0.5D;
            double blockZ = 0.5D;

            if (direction) {
                blockX = radius * Math.cos(radian);
            } else {
                blockZ = radius * Math.cos(radian);
            }

            int blockY = (int) (0.5D + radius * Math.sin(radian));

            blocks.add(this.world.getBlockAt(x + (int) blockX, y + blockY, z + (int) blockZ));
        }

        return blocks;
    }

    public int getNumber(Random random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static List<Block> getNeighbors(Block block) {
        return Arrays.asList(
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 1, 0),
                block.getRelative(0, -1, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1));
    }
}
