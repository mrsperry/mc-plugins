package com.mrjoshuasperry.customgeneration.populators.ores;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

import com.mrjoshuasperry.customgeneration.PopulatorUtils;
import com.mrjoshuasperry.mcutils.classes.Pair;

public class SimpleVeinPopulator extends BlockPopulator {
    private int chance;
    private int tries;
    private int min;
    private int max;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public SimpleVeinPopulator(int chance, int tries, int min, int max,
            List<Pair<Material, Byte>> place,
            List<Pair<Material, Byte>> surface,
            List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.tries = tries;
        this.min = min;
        this.max = max;

        this.place = place;
        this.surface = surface;
        this.replace = replace;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        for (int index = 0; index < this.tries; index++) {
            if (random.nextInt(this.chance) == 0) {
                PopulatorUtils utils = new PopulatorUtils(world, this.place);

                Block origin = utils.getBase(world, random, chunk, this.min, this.max);
                if (utils.isValidMaterial(origin, this.surface)) {
                    HashSet<Block> blocks = new HashSet<>();

                    // get the chance for a vein type
                    int chance = random.nextInt(100);

                    int x = origin.getX();
                    int y = origin.getY();
                    int z = origin.getZ();

                    if (chance < 15) {
                        // create a two block vein
                        if (random.nextBoolean()) {
                            if (random.nextBoolean()) {
                                blocks.add(origin.getRelative(1, 0, 0));
                            } else {
                                blocks.add(origin.getRelative(0, 1, 0));
                            }
                        } else {
                            blocks.add(origin.getRelative(0, 0, 1));
                        }

                        blocks.add(origin);
                    } else if ((chance > 15) && (chance < 45)) {
                        // create a four block vein
                        if (random.nextBoolean()) {
                            if (random.nextBoolean()) {
                                for (int blockX = 0; blockX < 2; blockX++) {
                                    for (int blockY = 0; blockY < 2; blockY++) {
                                        blocks.add(world.getBlockAt(blockX, blockY, z));
                                    }
                                }
                            } else {
                                for (int blockX = 0; blockX < 2; blockX++) {
                                    for (int blockZ = 0; blockZ < 2; blockZ++) {
                                        blocks.add(world.getBlockAt(x + blockX, y, z + blockZ));
                                    }
                                }
                            }
                        } else {
                            for (int blockX = 0; blockX < 2; blockX++) {
                                for (int blockY = 0; blockY < 2; blockY++) {
                                    blocks.add(world.getBlockAt(x + blockX, y + blockY, z + (blockX == 1 ? 1 : 0)));
                                }
                            }
                        }
                    } else if ((chance > 45) && (chance < 75)) {
                        // create a six block vein
                        int pattern = random.nextInt(3);
                        if (pattern == 0) {
                            for (int blockX = 0; blockX < 2; blockX++) {
                                for (int blockY = 0; blockY < 2; blockY++) {
                                    for (int blockZ = 0; blockZ < 2; blockZ++) {
                                        if ((blockX != 1) || (blockY != 1)) {
                                            blocks.add(world.getBlockAt(x + blockX, y + blockY, z + blockZ));
                                        }
                                    }
                                }
                            }
                        } else if (pattern == 1) {
                            for (int blockX = 0; blockX < 2; blockX++) {
                                for (int blockY = 0; blockY < 2; blockY++) {
                                    for (int blockZ = 0; blockZ < 2; blockZ++) {
                                        if ((blockX != 1) || (blockZ != 1)) {
                                            blocks.add(world.getBlockAt(x + blockX, y + blockY, z + blockZ));
                                        }
                                    }
                                }
                            }
                        } else {
                            for (int blockX = 0; blockX < 2; blockX++) {
                                for (int blockY = 0; blockY < 2; blockY++) {
                                    for (int blockZ = 0; blockZ < 2; blockZ++) {
                                        if ((blockY != 1) || (blockZ != 1)) {
                                            blocks.add(world.getBlockAt(x + blockX, y + blockY, z + blockZ));
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // create an eight block vein
                        for (int blockX = 0; blockX < 2; blockX++) {
                            for (int blockY = 0; blockY < 2; blockY++) {
                                for (int blockZ = 0; blockZ < 2; blockZ++) {
                                    blocks.add(world.getBlockAt(x + blockX, y + blockY, z + blockZ));
                                }
                            }
                        }
                    }

                    utils.setBlocks(this.replace, blocks);
                }
            }
        }
    }
}
