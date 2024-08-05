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

public class RibcageFossilPopulator extends BlockPopulator {
    private int chance;
    private int length;
    private int amount;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public RibcageFossilPopulator(int chance, int length, int amount,
                                  List<Pair<Material, Byte>> place,
                                  List<Pair<Material, Byte>> surface,
                                  List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.length = length;
        this.amount = amount;

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

            // create an offset to bury the ribs
            int offset = random.nextBoolean() ? 1 : 2;

            // generate the spine
            HashSet<Block> blocks = new HashSet<>(SpineGenerator.generate(world, random, origin, this.length, !direction));

            Block highest = world.getHighestBlockAt(origin.getX(), origin.getZ());

            for (int index = 0; index < this.amount; index++) {
                Block selected = highest.getRelative(-(!direction ? index * 2 : 0), -1, -(direction ? index * 2 : 0));
                selected = world.getHighestBlockAt(selected.getX(), selected.getZ());

                // add the ribs
                blocks.addAll(Arrays.asList(
                        selected.getRelative(direction ? 1 : 0, 0, direction ? 0 : 1),
                        selected.getRelative(direction ? -1 : 0, 0, direction ? 0 : -1),
                        selected.getRelative(direction ? 2 : 0, 0, direction ? 0 : 2),
                        selected.getRelative(direction ? -2 : 0, 0, direction ? 0 : -2),
                        selected.getRelative(direction ? 2 : 0, 1, direction ? 0 : 2),
                        selected.getRelative(direction ? -2 : 0, 1, direction ? 0 : -2),
                        selected.getRelative(direction ? 3 : 0, 1, direction ? 0 : 3),
                        selected.getRelative(direction ? -3 : 0, 1, direction ? 0 : -3),
                        selected.getRelative(direction ? 3 : 0, 2, direction ? 0 : 3),
                        selected.getRelative(direction ? -3 : 0, 2, direction ? 0 : -3),
                        selected.getRelative(direction ? 3 : 0, 3, direction ? 0 : 3),
                        selected.getRelative(direction ? -3 : 0, 3, direction ? 0 : -3),
                        selected.getRelative(direction ? 2 : 0, 4, direction ? 0 : 2),
                        selected.getRelative(direction ? -2 : 0, 4, direction ? 0 : -2)));
            }

            utils.setBlocks(this.replace, blocks, offset);
        }
    }
}
