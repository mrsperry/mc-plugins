package com.mrjoshuasperry.customgeneration.populators.rocks;

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

public class BigRockPopulator extends BlockPopulator {
    private int chance;
    private int minHeight;
    private int maxHeight;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public BigRockPopulator(int chance, int minHeight, int maxHeight,
                            List<Pair<Material, Byte>> place,
                            List<Pair<Material, Byte>> surface,
                            List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;

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

            int x = origin.getX();
            int y = origin.getY();
            int z = origin.getZ();

            // get the height for the main section
            int height = utils.getNumber(random, this.minHeight, this.maxHeight);
            // get the height for the top/bottom
            int maxHeight = height + height / 2;

            HashSet<Block> blocks = new HashSet<>();

            // create the sections
            for (int pillarX = -2; pillarX <= 2; pillarX++) {
                for (int pillarZ = -2; pillarZ <= 2; pillarZ++) {
                    for (int pillarY = maxHeight; pillarY >= -maxHeight; pillarY--) {
                        if ((pillarY > -height) && (pillarY < height)) {
                            if (((pillarX == -2) || (pillarX == 2)) && ((pillarZ == -2) || (pillarZ == 2))) {
                                continue;
                            }

                            if (((pillarX != -2 && pillarX != 2) && (pillarZ == -1 || pillarZ == 1))
                                    || ((pillarX == -1 || pillarX == 1) && (pillarZ == -2 || pillarZ == 2))) {
                                if ((!blocks.contains(world.getBlockAt(x + pillarX, y + pillarY + 1, z + pillarZ)))
                                        && (random.nextInt(3) > 0)) {
                                    continue;
                                }
                            }
                        } else {
                            if ((pillarX == -2) || (pillarX == 2) || (pillarZ == -2) || (pillarZ == 2)) {
                                continue;
                            }

                            if (((pillarX == -1) || (pillarX == 1)) && ((pillarZ == -1) || (pillarZ == 1))
                                    && (!blocks.contains(world.getBlockAt(x + pillarX, y + pillarY + 1, z + pillarZ)))
                                    && (random.nextBoolean())) {
                                continue;
                            }
                        }

                        blocks.add(world.getBlockAt(x + pillarX, y + pillarY, z + pillarZ));
                    }
                }
            }


            // top off all the sections
            blocks.addAll(Arrays.asList(
                    world.getBlockAt(x + 2, y + height, z),
                    world.getBlockAt(x + 2, y - height, z),
                    world.getBlockAt(x - 2, y + height, z),
                    world.getBlockAt(x - 2, y - height, z),
                    world.getBlockAt(x, y + height, z + 2),
                    world.getBlockAt(x, y - height, z + 2),
                    world.getBlockAt(x, y + height, z - 2),
                    world.getBlockAt(x, y - height, z - 2),

                    world.getBlockAt(x, y + maxHeight + 1, z),
                    world.getBlockAt(x, y - maxHeight - 1, z),
                    world.getBlockAt(x + 1, y - maxHeight - 1, z),
                    world.getBlockAt(x + 1, y + maxHeight + 1, z),
                    world.getBlockAt(x - 1, y - maxHeight - 1, z),
                    world.getBlockAt(x - 1, y + maxHeight + 1, z),
                    world.getBlockAt(x, y - maxHeight - 1, z + 1),
                    world.getBlockAt(x, y + maxHeight + 1, z + 1),
                    world.getBlockAt(x, y - maxHeight - 1, z - 1),
                    world.getBlockAt(x, y + maxHeight + 1, z - 1),
                    world.getBlockAt(x, y - maxHeight - 2, z),
                    world.getBlockAt(x, y + maxHeight + 2, z)));


            utils.setBlocks(this.replace, blocks);
        }
    }
}
