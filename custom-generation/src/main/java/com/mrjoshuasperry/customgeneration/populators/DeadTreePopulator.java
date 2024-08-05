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

public class DeadTreePopulator extends BlockPopulator {
    private int chance;
    private int min;
    private int max;
    private int branchMin;
    private int branchMax;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public DeadTreePopulator(int chance, int min, int max, int branchMin, int branchMax,
                             List<Pair<Material, Byte>> place,
                             List<Pair<Material, Byte>> surface,
                             List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.min = min;
        this.max = max;
        this.branchMin = branchMin;
        this.branchMax = branchMax;

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

            int x = origin.getX();
            int y = origin.getY();
            int z = origin.getZ();

            // get a random trunk height
            int height = utils.getNumber(random, min, max);

            // create the trunk
            for (int trunkY = y; trunkY <= y + height; trunkY++) {
                blocks.add(world.getBlockAt(x, trunkY, z));
            }

            int branchTries = 0;

            // create the branches and stumps
            for (int direction = 0; direction < 4; direction++) {
                int stumpX = x;
                int stumpY = y;
                int stumpZ = z;

                int branchX = x;
                int branchY = y + (int) Math.floor(height / 1.5D) + 1;
                int branchZ = z;

                // change direction
                switch (direction) {
                    case 0:
                        stumpX++;
                        branchX++;
                        break;
                    case 1:
                        stumpX--;
                        branchX--;
                        break;
                    case 2:
                        stumpZ++;
                        branchZ++;
                        break;
                    case 3:
                        stumpZ--;
                        branchZ--;
                }

                // create the stumps
                if (random.nextInt(5) > 0) {
                    // vary the height of the stumps
                    if (random.nextBoolean()) {
                        stumpY++;
                    }

                    // make sure the stumps touch the ground
                    do {
                        blocks.add(world.getBlockAt(stumpX, stumpY, stumpZ));
                        stumpY--;
                    } while ((world.getBlockAt(stumpX, stumpY, stumpZ).getType() == Material.AIR) && (stumpY >= 0));
                }


                // create the branches
                if ((random.nextInt(3) > 0) || ((direction == 3) && (branchTries == 0))) {
                    branchTries++;

                    // vary the start height of the branches
                    int initialY = random.nextInt(3);
                    if (initialY == 1) {
                        branchY++;
                    } else if (initialY == 2) {
                        branchY--;
                    }

                    if ((branchY == y + height) && (this.max != 1)) {
                        branchY--;
                    }

                    blocks.add(world.getBlockAt(branchX, branchY, branchZ));

                    for (int index = utils.getNumber(random, this.branchMin, this.branchMax); index < this.branchMax; index++) {
                        if (random.nextInt(5) == 0) {
                            branchY++;
                        }

                        if (random.nextBoolean()) {
                            if (random.nextBoolean()) {
                                branchX++;
                            } else {
                                branchZ++;
                            }
                        } else if (random.nextBoolean()) {
                            branchX--;
                        } else {
                            branchZ--;
                        }

                        blocks.add(world.getBlockAt(branchX, branchY, branchZ));
                    }
                }
            }

            utils.setBlocks(this.replace, blocks);
        }
    }
}
