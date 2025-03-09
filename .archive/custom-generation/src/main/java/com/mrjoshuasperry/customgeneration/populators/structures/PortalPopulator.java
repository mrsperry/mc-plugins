package com.mrjoshuasperry.customgeneration.populators.structures;

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

public class PortalPopulator extends BlockPopulator {
    private int chance;
    private int radius;
    private int min;
    private int max;

    private List<Pair<Material, Byte>> place;
    private List<Pair<Material, Byte>> surface;
    private List<Pair<Material, Byte>> replace;

    public PortalPopulator(int chance, int radius, int min, int max,
            List<Pair<Material, Byte>> place,
            List<Pair<Material, Byte>> surface,
            List<Pair<Material, Byte>> replace) {
        this.chance = chance;
        this.radius = radius;
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

            // get a random direction
            boolean direction = random.nextBoolean();

            int x = direction ? 1 : 0;
            int z = !direction ? 1 : 0;

            // get a random offset
            int offset = utils.getNumber(random, this.min, this.max);

            // create the portal
            HashSet<Block> blocks = new HashSet<>();
            blocks.addAll(utils.getVerticalCircle(origin.getRelative(0, 2, 0), direction, this.radius));
            blocks.addAll(utils.getVerticalCircle(origin.getRelative(0, 1, 0), direction, this.radius + 1));
            blocks.addAll(utils.getVerticalCircle(origin.getRelative(z, 1, x), direction, this.radius + 1));
            blocks.addAll(utils.getVerticalCircle(origin.getRelative(-z, 1, -x), direction, this.radius + 1));
            blocks.addAll(utils.getVerticalCircle(origin.getRelative(0, 0, 0), direction, this.radius + 2));

            utils.setBlocks(this.replace, blocks, offset);
        }
    }
}
