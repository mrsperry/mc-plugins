package com.mrjoshuasperry.customgeneration.populators.fossils;

import com.mrjoshuasperry.customgeneration.PopulatorUtils;

import java.util.List;
import java.util.Random;

import com.mrjoshuasperry.mcutils.classes.Pair;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;

public class SpineFossilPopulator extends BlockPopulator {
    private int chance;
    private int min;
    private int max;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public SpineFossilPopulator(int chance, int min, int max,
            List<Pair<Material, Byte>> place,
            List<Pair<Material, Byte>> surface,
            List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.min = min;
        this.max = max;

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

            // create an offset to put the spine in the ground
            int offset = random.nextBoolean() ? 1 : 2;

            // get a random length
            int length = utils.getNumber(random, this.min, this.max);

            // get a random direction
            boolean direction = random.nextBoolean();

            // create the spine
            utils.setBlocks(this.replace, SpineGenerator.generate(world, random, origin, length, direction), offset);
        }
    }
}
