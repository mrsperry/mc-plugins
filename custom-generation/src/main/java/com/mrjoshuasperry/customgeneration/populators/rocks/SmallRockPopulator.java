package com.mrjoshuasperry.customgeneration.populators.rocks;

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

public class SmallRockPopulator extends BlockPopulator {
    private int chance;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public SmallRockPopulator(int chance,
                              List<Pair<Material, Byte>> place,
                              List<Pair<Material, Byte>> surface,
                              List<Pair<Material, Byte>> replace) {
        this.chance = chance;

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

            HashSet<Block> blocks = new HashSet<>();

            // get a random height
            int height = random.nextBoolean() ? 2 : 3;

            // add the top
            blocks.add(origin.getRelative(0, height + 1, 0));

            // create the main structure
            for (int blockX = -1; blockX < 2; blockX++) {
                for (int blockZ = -1; blockZ < 2; blockZ++) {
                    for (int blockY = height; blockY >= -1; blockY--) {
                        Block block = origin.getRelative(blockX, blockY, blockZ);

                        if (((blockX != -1) && (blockX != 1)) || ((blockZ != -1) && (blockZ != 1))) {
                            if (blockY == height) {
                                if (random.nextBoolean()) {
                                    blocks.add(block);
                                }
                            } else if (blockY == 0) {
                                blocks.add(block);
                            }
                        }

                        if ((blockY != height) && (random.nextInt(5) == 0)) {
                            blocks.add(block);
                        }

                        if (blocks.contains(block.getRelative(0, 1, 0))) {
                            blocks.add(block);
                        }
                    }
                }
            }

            utils.setBlocks(this.replace, blocks);
        }
    }
}
