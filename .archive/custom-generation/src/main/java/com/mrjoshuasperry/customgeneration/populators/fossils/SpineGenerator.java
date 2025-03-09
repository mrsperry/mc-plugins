package com.mrjoshuasperry.customgeneration.populators.fossils;

import com.mrjoshuasperry.customgeneration.PopulatorUtils;

import java.util.HashSet;

import org.bukkit.block.Block;

public class SpineGenerator {
    public static HashSet<Block> generate(org.bukkit.World world, java.util.Random random, Block origin, int length, boolean direction) {
        HashSet<Block> blocks = new HashSet<>();

        int x = origin.getX();
        int z = origin.getZ();

        // create a spine
        for (int index = -length; index < length; index++) {
            int y = world.getHighestBlockYAt(x + (direction ? index : 0), z + (!direction ? index : 0));

            // create random height variation
            if (random.nextInt(3) == 0) {
                if (random.nextBoolean()) {
                    y++;
                } else {
                    y--;
                }
            }

            Block current = world.getBlockAt(x + (direction ? index : 0), y, z + (!direction ? index : 0));

            if (index % 2 == 0) {
                // add the current block and all neighbors
                blocks.add(current);
                blocks.addAll(PopulatorUtils.getNeighbors(current));
            }
        }

        return blocks;
    }
}
