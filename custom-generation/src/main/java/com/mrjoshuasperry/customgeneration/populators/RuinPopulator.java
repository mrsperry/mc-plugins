package com.mrjoshuasperry.customgeneration.populators;

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

public class RuinPopulator extends BlockPopulator {
    private int chance;
    private int featureChance;
    private int min;
    private int max;
    private int heightMin;
    private int heightMax;

    private List<Pair<Material, Byte>> floor;
    private List<Pair<Material, Byte>> features;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public RuinPopulator(int chance, int featureChance, int min, int max, int heightMin, int heightMax,
                         List<Pair<Material, Byte>> floor,
                         List<Pair<Material, Byte>> features,
                         List<Pair<Material, Byte>> place,
                         List<Pair<Material, Byte>> surface,
                         List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.featureChance = featureChance;
        this.min = min;
        this.max = max;
        this.heightMin = heightMin;
        this.heightMax = heightMax;

        this.floor = floor;
        this.features = features;

        this.place = place;
        this.surface = surface;
        this.replace = replace;
    }

    public void populate(World world, Random random, Chunk chunk) {
        if (random.nextInt(this.chance) == 0) {
            PopulatorUtils utils = new PopulatorUtils(world, this.place);

            Block origin = utils.getBase(world, random, chunk);
            if (!utils.isValidMaterial(origin, this.surface)) {
                return;
            }

            // get a random length, width, and height for the ruin
            int length = utils.getNumber(random, this.min, this.max);
            int width = utils.getNumber(random, this.min, this.max);
            int height = utils.getNumber(random, this.heightMin, this.heightMax);

            HashSet<Block> walls = new HashSet<>();

            int x = origin.getX();
            int z = origin.getZ();

            // create the ruin border
            for (int blockX = 0; blockX <= length; blockX++) {
                for (int blockZ = 0; blockZ <= width; blockZ++) {
                    if (((blockX == 0) || (blockX == length) || (blockZ == 0) || (blockZ == width))
                            && (random.nextInt(3) > 0)) {
                        Block block = world.getHighestBlockAt(x + blockX, z + blockZ);
                        walls.add(block);

                        // make sure the blocks aren't hovering
                        if (!utils.isValidMaterial(block, this.surface)) {
                            return;
                        }
                    }
                }
            }

            // create the ruin walls
            HashSet<Block> holder = new HashSet<>();
            for (Block block : walls) {
                for (int blockY = 1; blockY <= height; blockY++) {
                    if (random.nextInt(blockY + 1) == 0) {
                        Block current = block.getRelative(0, blockY, 0);
                        Block below = current.getRelative(0, -1, 0);

                        // make sure the walls aren't floating
                        if ((walls.contains(below)) || (holder.contains(below))) {
                            holder.add(current);
                        }

                        if (!utils.isValidMaterial(current, this.replace)) {
                            return;
                        }
                    }
                }
            }

            walls.addAll(holder);

            utils.setBlocks(this.replace, walls);

            // create the floor
            for (int blockX = 1; blockX < length; blockX++) {
                for (int blockY = 0; blockY < 2; blockY++) {
                    for (int blockZ = 1; blockZ < width; blockZ++) {
                        if (blockY == 0) {
                            // add the floor blocks
                            Block block = world.getHighestBlockAt(x + blockX, z + blockZ).getRelative(0, -1, 0);
                            Pair<Material, Byte> selected = this.floor.get(random.nextInt(this.floor.size()));

                            utils.setType(block.getX(), block.getY(), block.getZ(), selected.getKey(), selected.getValue());
                        } else if ((blockX == 1) || (blockX == length - 1) || (blockZ == 1) || (blockZ == width - 1)) {
                            // add the feature blocks
                            Block block = world.getHighestBlockAt(x + blockX, z + blockZ);

                            if ((block.getRelative(0, -1, 0).getType().isSolid())
                                    && (random.nextInt(this.featureChance) == 0)) {
                                Pair<Material, Byte> selected = this.features.get(random.nextInt(this.features.size()));

                                utils.setType(block.getX(), block.getY(), block.getZ(), selected.getKey(), selected.getValue());
                            }
                        }
                    }
                }
            }
        }
    }
}
