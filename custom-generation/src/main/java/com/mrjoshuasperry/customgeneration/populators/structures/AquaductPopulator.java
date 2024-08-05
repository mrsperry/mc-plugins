package com.mrjoshuasperry.customgeneration.populators.structures;

import com.mrjoshuasperry.customgeneration.PopulatorUtils;

import java.util.ArrayList;
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

public class AquaductPopulator extends BlockPopulator {
    private int chance;
    private int chestChance;
    private int maxY;
    private int minHeight;
    private int maxHeight;
    private int minLength;
    private int maxLength;
    private int minAmount;
    private int maxAmount;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public AquaductPopulator(int chance, int chestChance, int maxY, int minHeight, int maxHeight, int minLength, int maxLength, int minAmount, int maxAmount,
                             List<Pair<Material, Byte>> place,
                             List<Pair<Material, Byte>> surface,
                             List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.chestChance = chestChance;
        this.maxY = maxY;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;

        this.place = place;
        this.surface = surface;
        this.replace = replace;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (random.nextInt(this.chance) == 0) {
            PopulatorUtils utils = new PopulatorUtils(world, this.place);

            Block origin = utils.getBase(world, random, chunk).getRelative(0, -1, 0);
            if (!utils.isValidMaterial(origin, this.surface)) {
                return;
            }

            HashSet<Block> blocks = new HashSet<>();
            List<Block> chests = new ArrayList<>();

            // get a random direction
            boolean direction = random.nextBoolean();

            // get a random vertical direction
            boolean directionY = random.nextBoolean();

            // get a random support height
            int height = utils.getNumber(random, minHeight, maxHeight);

            // get a random section length
            int length = utils.getNumber(random, minLength, maxLength);

            // get a random section amount
            int amount = utils.getNumber(random, minAmount, maxAmount);

            // Y offset based on the Y direction
            int offset = 0;

            // edges of each side of the completed structure
            Block[] lower = new Block[6];

            int x;
            int y;
            for (int segment = 0; segment <= amount; segment++) {
                x = origin.getX() + segment * (direction ? length * 2 + 2 : 0);
                y = origin.getY() + offset;
                int z = origin.getZ() + segment * (!direction ? length * 2 + 2 : 0);

                // stop the segment if it gets too high
                if (y >= this.maxY) {
                    break;
                }

                // increase the Y offset
                offset += (directionY ? 1 : -1);

                // get the base of the supports
                List<Block> holder = new ArrayList<>(Arrays.asList(
                        world.getBlockAt(x + 2, y, z + 2),
                        world.getBlockAt(x + 2, y, z - 2),
                        world.getBlockAt(x - 2, y, z + 2),
                        world.getBlockAt(x - 2, y, z - 2)));

                // make sure the supports touch the ground
                for (Block support : holder) {
                    int blockY = 0;
                    do {
                        blocks.add(support.getRelative(0, blockY, 0));
                        blockY--;
                    } while (support.getRelative(0, blockY, 0).getType() == Material.AIR);
                }
                holder.clear();


                // make the Y sit on top of the pillar bases
                y++;

                // create the supports
                for (int blockY = y; blockY < y + height; blockY++) {
                    blocks.addAll(Arrays.asList(
                            world.getBlockAt(x + 2, blockY, z + 2),
                            world.getBlockAt(x + 2, blockY, z - 2),
                            world.getBlockAt(x - 2, blockY, z + 2),
                            world.getBlockAt(x - 2, blockY, z - 2)));
                }

                // make the Y equal to the top of the pillars
                y += height - 1;

                // create the pillar arch
                holder.addAll(Arrays.asList(
                        world.getBlockAt(direction ? x + 1 : x - 2, y, direction ? z + 2 : z + 1),
                        world.getBlockAt(direction ? x - 1 : x - 2, y, direction ? z + 2 : z - 1),
                        world.getBlockAt(direction ? x + 1 : x + 2, y, direction ? z - 2 : z + 1),
                        world.getBlockAt(direction ? x - 1 : x + 2, y, direction ? z - 2 : z - 1)));

                for (Block block : holder) {
                    blocks.add(block);
                    blocks.add(block.getRelative(0, 1, 0));
                }

                // make the Y equal to the top of the arch
                y++;

                // create the base of the top arch
                for (int blockY = y; blockY <= y + 4; blockY++) {
                    blocks.add(world.getBlockAt(direction ? x : x + 2, blockY, direction ? z + 2 : z));
                    blocks.add(world.getBlockAt(direction ? x : x - 2, blockY, direction ? z - 2 : z));
                }

                // make the Y equal to the top of the top arch
                y += 4;

                // create the top arch
                blocks.addAll(Arrays.asList(
                        world.getBlockAt(direction ? x : x - 1, y, direction ? z - 1 : z),
                        world.getBlockAt(direction ? x : x + 1, y, direction ? z + 1 : z),
                        world.getBlockAt(direction ? x : x - 1, y + 1, direction ? z - 1 : z),
                        world.getBlockAt(direction ? x : x + 1, y + 1, direction ? z + 1 : z),
                        world.getBlockAt(x, y + 1, z)));

                // make the Y equal to the trough height
                y -= 4;

                // create the troughs
                for (int index = -length; index <= length + 1; index++) {
                    Block left = world.getBlockAt(direction ? x - index : x + 1, y, direction ? z + 1 : z - index);
                    Block right = world.getBlockAt(direction ? x - index : x - 1, y, direction ? z - 1 : z - index);
                    Block center = world.getBlockAt(direction ? x - index : x, y - 1, direction ? z : z - index);

                    // add the edges of the structure
                    if ((segment == 0) && (index == length + 1)) {
                        lower[0] = left;
                        lower[1] = right;
                        lower[2] = center;
                    } else if (index == -length) {
                        lower[3] = left;
                        lower[4] = right;
                        lower[5] = center;
                    }

                    blocks.addAll(Arrays.asList(left, right, center));
                }

                // add the valid chest blocks
                chests.add(world.getBlockAt(x, y, z));
            }


            // randomly lower the edges
            for (Block block : lower) {
                if (random.nextInt(5) > 0) {
                    blocks.remove(block);
                    blocks.add(world.getBlockAt(block.getX(), block.getY() - 1, block.getZ()));
                }
            }

            // randomly add a chest
            if ((utils.setBlocks(this.replace, blocks)) && (random.nextInt(this.chestChance) == 0)) {
                Block block = chests.get(random.nextInt(chests.size()));
                utils.setType(block.getX(), block.getY(), block.getZ(), Material.CHEST, (byte) (direction ? 0 : 1));
            }
        }
    }
}
