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

public class CavePopulator extends BlockPopulator {
    private int chance;
    private int wallChance;
    private int minY;
    private int maxY;
    private int minBlocks;
    private int maxBlocks;
    private int minDistance;
    private int maxDistance;
    private int chanceToBreak;

    private List<Pair<Material, Byte>> walls;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public CavePopulator(int chance, int wallChance, int minY, int maxY, int minBlocks, int maxBlocks, int minDistance, int maxDistance, int chanceToBreak,
                         List<Pair<Material, Byte>> walls,
                         List<Pair<Material, Byte>> place,
                         List<Pair<Material, Byte>> surface,
                         List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.wallChance = wallChance;
        this.minY = minY;
        this.maxY = maxY;
        this.minBlocks = minBlocks;
        this.maxBlocks = maxBlocks;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.chanceToBreak = chanceToBreak;

        this.walls = walls;

        this.place = place;
        this.surface = surface;
        this.replace = replace;
    }

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (random.nextInt(this.chance) == 0) {
            PopulatorUtils utils = new PopulatorUtils(world, this.place);

            Block origin = utils.getBase(world, random, chunk, this.minY, this.maxY);
            if (!utils.isValidMaterial(origin, this.surface)) {
                return;
            }

            int x = origin.getX();
            int y = origin.getY();
            int z = origin.getZ();

            HashSet<Block> blocks = new HashSet<>();

            boolean stop = false;
            // create a vein to expand on
            do {
                if (y > this.maxY) {
                    return;
                }

                blocks.add(world.getBlockAt(x, y, z));

                if (random.nextInt(this.chanceToBreak) != 0) {
                    switch (random.nextInt(5)) {
                        case 0:
                            x++;
                            break;
                        case 1:
                            y++;
                            break;
                        case 2:
                            z++;
                            break;
                        case 3:
                            x--;
                            break;
                        case 4:
                            y--;
                            break;
                        case 5:
                            z--;
                    }

                } else {
                    stop = true;
                }
            } while (((!stop) || (blocks.size() < this.minBlocks)) && (blocks.size() <= this.maxBlocks));

            // blow up the vein based on a distance
            for (Block block : blocks) {
                utils.setType(block.getX(), block.getY(), block.getZ(), Material.AIR, (byte) 0);

                x = block.getX();
                y = block.getY();
                z = block.getZ();

                // length from any block
                int length = utils.getNumber(random, this.minDistance, this.maxDistance);

                for (int blockX = -length; blockX <= length; blockX++) {
                    for (int blockY = -length; blockY <= length; blockY++) {
                        for (int blockZ = -length; blockZ <= length; blockZ++) {
                            Block current = world.getBlockAt(x + blockX, y + blockY, z + blockZ);
                            Material type = current.getType();

                            // get the distance
                            double distance = current.getLocation().distance(block.getLocation());

                            // remove all the blocks within the length
                            if (distance <= length) {
                                if (type != Material.AIR) {
                                    Pair<Material, Byte> material = this.place.get(random.nextInt(this.place.size()));
                                    utils.setType(current.getX(), current.getY(), current.getZ(), material.getKey(), material.getValue());
                                }
                            }

                            // add in the wall materials
                            if ((distance >= length) && (distance <= length + 1) &&
                                    (random.nextInt(this.wallChance) == 0) &&
                                    (utils.isValidMaterial(current, this.replace))) {
                                Pair<Material, Byte> material = this.walls.get(random.nextInt(this.walls.size()));
                                utils.setType(current.getX(), current.getY(), current.getZ(), material.getKey(), material.getValue());
                            }
                        }
                    }
                }
            }
        }
    }
}
