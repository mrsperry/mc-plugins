package com.mrjoshuasperry.customgeneration.populators.fossils;

import com.mrjoshuasperry.customgeneration.PopulatorUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javafx.util.Pair;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;


public class HandFossilPopulator extends BlockPopulator {
    private int chance;
    private int chestChance;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public HandFossilPopulator(int chance, int chestChance,
                               List<Pair<Material, Byte>> place,
                               List<Pair<Material, Byte>> surface,
                               List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.chestChance = chestChance;

        this.place = place;
        this.surface = surface;
        this.replace = replace;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (random.nextInt(this.chance) == 0) {
            PopulatorUtils utils = new PopulatorUtils(world, this.place);

            Block origin = utils.getBase(world, random, chunk);
            if (!utils.isValidMaterial(origin, this.surface)) {
                return;
            }

            // get a random direction
            boolean direction = random.nextBoolean();

            // create an offset to bury the hand
            int offset = random.nextBoolean() ? 3 : 4;

            HashSet<Block> blocks = new HashSet<>();

            int x = origin.getX();
            int y = origin.getY();
            int z = origin.getZ();

            // create the palm
            for (int blockX = x - 2; blockX <= x + 2; blockX++) {
                for (int blockZ = z - 2; blockZ <= z + 2; blockZ++) {
                    if (direction ? (blockX == x - 2) && (blockZ == z) : (blockX != x) || (blockZ != z - 2)) {
                        if ((blockX == x + 2)
                                || (blockX == x - 2)
                                || (blockZ == z + 2)
                                || (blockZ == z - 2)) {
                            if (((blockX != x + 2) || (blockZ != z + 2))
                                    && ((blockX != x + 2) || (blockZ != z - 2))
                                    && ((blockX != x - 2) || (blockZ != z + 2))
                                    && ((blockX != x - 2) || (blockZ != z - 2))) {
                                blocks.add(world.getBlockAt(blockX, y + 1, blockZ));
                            }
                        } else {
                            blocks.add(world.getBlockAt(blockX, y, blockZ));
                        }
                    }
                }
            }

            // create the fingers
            blocks.addAll(Arrays.asList(
                    world.getBlockAt(direction ? x - 3 : x + 1, y + 1, !direction ? z - 3 : z + 1),
                    world.getBlockAt(direction ? x - 3 : x - 1, y + 1, !direction ? z - 3 : z - 1),
                    world.getBlockAt(direction ? x - 4 : x, y + 1, !direction ? z - 4 : z),
                    world.getBlockAt(direction ? x - 2 : x, y, !direction ? z - 2 : z),
                    world.getBlockAt(direction ? x - 3 : x, y, !direction ? z - 3 : z),
                    world.getBlockAt(!direction ? x - 3 : x, y + 2, direction ? z - 3 : z),
                    world.getBlockAt(!direction ? x - 3 : x + 1, y + 3, direction ? z - 3 : z + 1),
                    world.getBlockAt(!direction ? x + 3 : x, y + 2, direction ? z + 3 : z),
                    world.getBlockAt(!direction ? x + 3 : x + 1, y + 3, direction ? z + 3 : z + 1),
                    world.getBlockAt(direction ? x + 2 : x - 2, y + 2, z + 2),
                    world.getBlockAt(direction ? x + 3 : x + 2, y + 3, !direction ? z + 3 : z + 2),
                    world.getBlockAt(direction ? x + 3 : x + 2, y + 4, !direction ? z + 3 : z + 2),
                    world.getBlockAt(x + 2, y + 2, direction ? z - 2 : z + 2),
                    world.getBlockAt(direction ? x + 3 : x - 2, y + 3, !direction ? z + 3 : z - 2),
                    world.getBlockAt(direction ? x + 3 : x - 2, y + 4, !direction ? z + 3 : z - 2),
                    world.getBlockAt(direction ? x + 3 : x, y + 2, !direction ? z + 3 : z),
                    world.getBlockAt(direction ? x + 4 : x, y + 3, !direction ? z + 4 : z),
                    world.getBlockAt(direction ? x + 4 : x, y + 4, !direction ? z + 4 : z),
                    world.getBlockAt(direction ? x + 4 : x, y + 5, !direction ? z + 4 : z)));


            if (utils.setBlocks(this.replace, blocks, offset)) {
                // randomly add a chest on the palm
                if (random.nextInt(this.chestChance) == 0) {
                    Block block = origin.getRelative(0, 1, 0);
                    utils.setType(block.getX(), block.getY() - offset, block.getZ(), Material.CHEST, (byte) 0);
                }
            }
        }
    }
}
