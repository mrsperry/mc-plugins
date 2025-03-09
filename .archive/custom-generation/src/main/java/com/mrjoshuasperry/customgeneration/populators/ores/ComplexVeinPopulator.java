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

public class ComplexVeinPopulator extends BlockPopulator {
    private int chance;
    private int tries;
    private int min;
    private int max;
    private int minAmount;
    private int maxAmount;
    private int chanceToBreak;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public ComplexVeinPopulator(int chance, int tries, int min, int max, int minAmount, int maxAmount,
            int chanceToBreak,
            List<Pair<Material, Byte>> place,
            List<Pair<Material, Byte>> surface,
            List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.tries = tries;
        this.min = min;
        this.max = max;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chanceToBreak = chanceToBreak;

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

                    int x = origin.getX();
                    int y = origin.getY();
                    int z = origin.getZ();

                    boolean stop = false;

                    // record the last direction moved
                    // use this to determine which direction we CAN'T go
                    // this greatly increases the validity of the vein
                    int last = 0;
                    do {
                        blocks.add(world.getBlockAt(x, y, z));

                        // chance to stop the vein
                        if (random.nextInt(this.chanceToBreak) != 0) {
                            switch (random.nextInt(5)) {
                                case 0:
                                    if (last != 0) {
                                        x++;
                                        last = 0;
                                    }
                                    break;
                                case 1:
                                    if (last != 1) {
                                        y++;
                                        last = 1;
                                    }
                                    break;
                                case 2:
                                    if (last != 2) {
                                        z++;
                                        last = 2;
                                    }
                                    break;
                                case 3:
                                    if (last != 0) {
                                        x--;
                                        last = 0;
                                    }
                                    break;
                                case 4:
                                    if (last != 1) {
                                        y--;
                                        last = 1;
                                    }
                                    break;
                                case 5:
                                    if (last != 2) {
                                        z--;
                                        last = 2;
                                    }
                                    break;
                            }
                        } else {
                            stop = true;
                        }
                    } while (((!stop) || (blocks.size() < this.minAmount)) && (blocks.size() <= this.maxAmount));

                    utils.setBlocks(this.replace, blocks);
                }
            }
        }
    }
}
