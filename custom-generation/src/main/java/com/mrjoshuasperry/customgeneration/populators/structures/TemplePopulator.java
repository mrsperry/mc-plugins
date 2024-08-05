package com.mrjoshuasperry.customgeneration.populators.structures;

import com.mrjoshuasperry.customgeneration.PopulatorUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javafx.util.Pair;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

public class TemplePopulator extends BlockPopulator {
    private int chance;

    private List<Pair<Material, Byte>> lights;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    // due to some data loss this class is a bit messy
    public TemplePopulator(int chance,
                           List<Pair<Material, Byte>> lights,
                           List<Pair<Material, Byte>> place,
                           List<Pair<Material, Byte>> surface,
                           List<Pair<Material, Byte>> replace) {
        this.chance = chance;

        this.lights = lights;

        this.place = place;
        this.surface = surface;
        this.replace = replace;
    }

    public void populate(World world, Random random, Chunk chunk) {
        if (random.nextInt(this.chance) == 0) {
            PopulatorUtils utils = new PopulatorUtils(world, this.place);

            Block origin = utils.getBase(world, random, chunk).getRelative(0, -2, 0);
            if (!utils.isValidMaterial(origin, this.surface)) {
                return;
            }

            HashSet<Block> blocks = new HashSet<>();
            HashSet<Block> lights = new HashSet<>();
            this.addPillar(origin.getRelative(-7, 0, -7), blocks, lights);
            this.addPillar(origin.getRelative(-7, 0, 7), blocks, lights);
            this.addPillar(origin.getRelative(7, 0, -7), blocks, lights);
            this.addPillar(origin.getRelative(7, 0, 7), blocks, lights);

            for (int blockX = -4; blockX <= 4; blockX++) {
                for (int blockZ = -4; blockZ <= 4; blockZ++) {
                    for (int blockY = 0; blockY <= 4; blockY++) {
                        Block block = origin.getRelative(blockX, blockY, blockZ);
                        blocks.add(block);

                        if (blockY == 0) {
                            int offset = -1;

                            do {
                                blocks.add(block.getRelative(0, offset, 0));
                                --offset;
                            } while (block.getRelative(0, offset, 0).getType() == Material.AIR);
                        }

                        if (blockY == 4 && (blockX == -2 || blockX == 2) && (blockZ == -2 || blockZ == 2)) {
                            lights.add(block);
                        }
                    }
                }
            }

            this.addStaircase(origin.getRelative(-8, 0, 0), blocks, 2, 3, true, false);
            this.addStaircase(origin.getRelative(0, 0, -8), blocks, 2, 3, false, false);
            this.addStaircase(origin.getRelative(8, 0, 0), blocks, 2, 3, true, true);
            this.addStaircase(origin.getRelative(0, 0, 8), blocks, 2, 3, false, true);
            this.addStaircase(origin.getRelative(-9, 1, -3), blocks, 0, 3, true, false);
            this.addStaircase(origin.getRelative(-9, 1, 3), blocks, 0, 3, true, false);
            this.addStaircase(origin.getRelative(9, 1, -3), blocks, 0, 3, true, true);
            this.addStaircase(origin.getRelative(9, 1, 3), blocks, 0, 3, true, true);
            this.addStaircase(origin.getRelative(-3, 1, -9), blocks, 0, 3, false, false);
            this.addStaircase(origin.getRelative(-3, 1, 9), blocks, 0, 3, false, true);
            this.addStaircase(origin.getRelative(3, 1, -9), blocks, 0, 3, false, false);
            this.addStaircase(origin.getRelative(3, 1, 9), blocks, 0, 3, false, true);

            for (int blockX = -4; blockX <= 4; blockX++) {
                for (int blockZ = -4; blockZ <= 4; blockZ++) {
                    for (int blockY = 0; blockY <= 13; blockY++) {
                        Block block = origin.getRelative(blockX, blockY + 4, blockZ);

                        if ((blockX != -4 && blockX != 4 || blockZ != -4 && blockZ != 4) && (blockX != -4 && blockX != 4 || blockZ != -3 && blockZ != 3) && (blockX != -3 && blockX != 3 || blockZ != -4 && blockZ != 4) && (blockY > 5 && blockY < 9 || blockX != -4 && blockX != 4 && blockZ != -4 && blockZ != 4) && (blockY < 6 || blockY > 8 || (blockX < -1 || blockX > 1 || blockZ != -4 && blockZ != 4) && (blockX != -4 && blockX != 4 || blockZ < -1 || blockZ > 1)) && ((blockY < 4 || blockY > 6) && blockY < 10 || blockX != -3 && blockX != 3 || blockZ != -3 && blockZ != 3)) {
                            if (blockX != 0 || blockZ != 0) {
                                if (blockY <= 7) {
                                    if (blockY == 7) {
                                        if ((blockX == -3 || blockX == 3) && blockZ == 0 || blockX == 0 && (blockZ == -3 || blockZ == 3) || (blockX == -1 || blockX == 1) && blockZ == 0 || blockX == 0 && (blockZ == -1 || blockZ == 1)) {
                                            continue;
                                        }
                                    } else if ((blockX == -3 || blockX == 3) && blockZ >= -1 && blockZ <= 1 || blockX >= -1 && blockX <= 1 && (blockZ == -3 || blockZ == 3)) {
                                        continue;
                                    }
                                }

                                if (blockX >= -2 && blockX <= 2 && blockZ >= -2 && blockZ <= 2 && (blockY <= 4 || blockY == 5 && (blockX != -2 && blockX != 2 || blockZ != -2 && blockZ != 2) || blockY == 6 && (blockX != -2 && blockX != 2 || !(blockZ == -2 | blockZ == 2)) && (blockX != -2 && blockX != 2 || blockZ != -1 && blockZ != 1) && (blockX != -1 && blockX != 1 || blockZ != -2 && blockZ != 2) || blockY >= 8 && blockY <= 10) || blockY == 11 && ((blockX == -1 || blockX == 1) && blockZ == 0 || blockX == 0 && (blockZ == -1 || blockZ == 1))) {
                                    continue;
                                }
                            }

                            if ((blockY == 2 || blockY == 8) && (blockX == -3 || blockX == 3) && (blockZ == -3 || blockZ == 3)) {
                                lights.add(block);
                            }

                            if (blockY >= 9) {
                                if (((blockX == -3 || blockX == 3) && blockZ <= 3 && blockZ != 0 || blockX <= 3 && (blockZ == -3 || blockZ == 3) && blockX != 0) && (blockX != -3 && blockX != 3 || blockZ != -3 && blockZ != 3)) {
                                    continue;
                                }

                                if (blockY == 10 && blockX == 0 && blockZ == 0) {
                                    lights.add(block);
                                }

                                if (blockY == 11 || blockY == 12) {
                                    if (blockX != 0 && blockZ != 0) {
                                        if (blockX != -3 && blockX != 3 && blockZ != -3 && blockZ == 3) {
                                            continue;
                                        }
                                        continue;
                                    }

                                    if (blockY == 12 && (blockX == -3 || blockX == 3 || blockZ == -3 || blockZ == 3)) {
                                        continue;
                                    }
                                }

                                if (blockY == 13 && (blockX != 0 || blockZ != 0)) {
                                    continue;
                                }
                            }

                            blocks.add(block);
                        }
                    }
                }
            }

            if (utils.setBlocks(this.replace, blocks)) {
                for (Block light : lights) {
                    Pair<Material, Byte> type = this.lights.get(random.nextInt(this.lights.size()));
                    utils.setType(light.getX(), light.getY(), light.getZ(), type.getKey(), type.getValue());
                }

                for (int blockX = 1; blockX <= 8; blockX++) {
                    Block block = origin.getRelative(0, blockX + 4, 0);
                    utils.setType(block.getX() - 1, block.getY(), block.getZ(), Material.LADDER, (byte) 4);
                    utils.setType(block.getX() + 1, block.getY(), block.getZ(), Material.LADDER, (byte) 5);
                    utils.setType(block.getX(), block.getY(), block.getZ() - 1, Material.LADDER, (byte) 2);
                    utils.setType(block.getX(), block.getY(), block.getZ() + 1, Material.LADDER, (byte) 3);
                }

                Block block = origin.getRelative(0, 12, 0);
                utils.setType(block.getX() - 3, block.getY(), block.getZ() - 2, Material.TRAPPED_CHEST, (byte) 5);
                utils.setType(block.getX() - 3, block.getY() - 1, block.getZ() - 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() - 3, block.getY(), block.getZ() + 2, Material.TRAPPED_CHEST, (byte) 5);
                utils.setType(block.getX() - 3, block.getY() - 1, block.getZ() + 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() + 3, block.getY(), block.getZ() - 2, Material.TRAPPED_CHEST, (byte) 4);
                utils.setType(block.getX() + 3, block.getY() - 1, block.getZ() - 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() + 3, block.getY(), block.getZ() + 2, Material.TRAPPED_CHEST, (byte) 4);
                utils.setType(block.getX() + 3, block.getY() - 1, block.getZ() + 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() - 2, block.getY(), block.getZ() - 3, Material.TRAPPED_CHEST, (byte) 3);
                utils.setType(block.getX() - 2, block.getY() - 1, block.getZ() - 3, Material.TNT, (byte) 0);
                utils.setType(block.getX() + 2, block.getY(), block.getZ() - 3, Material.TRAPPED_CHEST, (byte) 3);
                utils.setType(block.getX() + 2, block.getY() - 1, block.getZ() - 3, Material.TNT, (byte) 0);
                utils.setType(block.getX() - 2, block.getY(), block.getZ() + 3, Material.TRAPPED_CHEST, (byte) 0);
                utils.setType(block.getX() - 2, block.getY() - 1, block.getZ() + 3, Material.TNT, (byte) 0);
                utils.setType(block.getX() + 2, block.getY(), block.getZ() + 3, Material.TRAPPED_CHEST, (byte) 0);
                utils.setType(block.getX() + 2, block.getY() - 1, block.getZ() + 3, Material.TNT, (byte) 0);
                utils.setType(block.getX() - 2, block.getY() - 2, block.getZ() - 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() - 2, block.getY() - 2, block.getZ() + 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() + 2, block.getY() - 2, block.getZ() - 2, Material.TNT, (byte) 0);
                utils.setType(block.getX() + 2, block.getY() - 2, block.getZ() + 2, Material.TNT, (byte) 0);
            }
        }

    }

    public void addPillar(Block origin, HashSet<Block> blocks, HashSet<Block> lights) {
        for (int blockX = -3; blockX <= 3; blockX++) {
            for (int blockZ = -3; blockZ <= 3; blockZ++) {
                if (blockX != -3 && blockX != 3 || blockZ != -3 && blockZ != 3) {
                    Block current = origin.getRelative(blockX, 0, blockZ);
                    int offset = 0;

                    do {
                        blocks.add(current.getRelative(0, offset, 0));
                        --offset;
                    } while(current.getRelative(0, offset, 0).getType() == Material.AIR);
                }
            }
        }

        for (int blockX = -2; blockX <= 2; blockX++) {
            for (int blockZ = -2; blockZ <= 2; blockZ++) {
                for(int blockY = 0; blockY <= 9; blockY++) {
                    Block current = origin.getRelative(blockX, blockY + 1, blockZ);

                    if ((blockX != -2 && blockX != 2 || blockZ != -2 && blockZ != 2) && (blockY < 1 || (blockX != -2 && blockX != 2 || blockZ != -1 && blockZ != 1) && (blockX != -1 && blockX != 1 || blockZ != -2 && blockZ != 2) && ((blockY < 4 || blockY > 7) && blockY != 9 || blockX != -1 && blockX != 1 || blockZ != -1 && blockZ != 1) && ((blockY < 5 || blockY > 7) && blockY != 9 || (blockX != -2 && blockX != 2 || blockZ != 0) && blockZ != -2 && blockZ != 2))) {
                        blocks.add(current);

                        if (blockY == 2 && (blockX == -1 || blockX == 1) && (blockZ == -1 || blockZ == 1)) {
                            lights.add(current);
                        }

                        if (blockY == 6 && ((blockX == -1 || blockX == 1) && blockZ == 0 || (blockZ == -1 || blockZ == 1) && blockX == 0)) {
                            lights.add(current);
                        }

                        if (blockY == 9 && blockX == 0 && blockZ == 0) {
                            lights.add(current);
                        }
                    }
                }
            }
        }
    }

    public void addStaircase(Block origin, HashSet<Block> blocks, int width, int maxY, boolean direction, boolean flip) {
        if (!flip) {
            for (int blockX = (direction ? -3 : -width); blockX <= (direction ? 4 : width); blockX++) {
                for (int blockZ = (direction ? -width : -3); blockZ <= (direction ? width : 4); blockZ++) {
                    for (int blockY = 0; blockY <= (direction ? blockX : blockZ); blockY++) {
                        Block current = origin.getRelative(blockX, blockY > maxY ? maxY : blockY, blockZ);
                        blocks.add(current);

                        if (blockY == 0) {
                            for (int offset = -1; current.getRelative(0, offset, 0).getType() == Material.AIR; offset--) {
                                blocks.add(current.getRelative(0, offset, 0));
                            }
                        }
                    }
                }
            }
        } else {
            for (int blockX = (direction ? 0 : width); blockX >= (direction ? -4 : -width); blockX--) {
                for (int blockZ = (direction ? width : 0); blockZ >= (direction ? -width : -4); blockZ--) {
                    for (int blockY = 0; blockY <= Math.abs(direction ? blockX : blockZ); blockY++) {
                        Block current = origin.getRelative(blockX, Math.abs(blockY > maxY ? maxY : blockY), blockZ);
                        blocks.add(current);

                        if (blockY == 0) {
                            for (int offset = -1; current.getRelative(0, offset, 0).getType() == Material.AIR; offset--) {
                                blocks.add(current.getRelative(0, offset, 0));
                            }
                        }
                    }
                }
            }
        }
    }
}
