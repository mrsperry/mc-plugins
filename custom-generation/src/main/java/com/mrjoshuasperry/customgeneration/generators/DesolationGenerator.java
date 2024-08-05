package com.mrjoshuasperry.customgeneration.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import com.mrjoshuasperry.customgeneration.populators.CavePopulator;
import com.mrjoshuasperry.customgeneration.populators.DeadTreePopulator;
import com.mrjoshuasperry.customgeneration.populators.RuinPopulator;
import com.mrjoshuasperry.customgeneration.populators.fossils.HandFossilPopulator;
import com.mrjoshuasperry.customgeneration.populators.fossils.RibcageFossilPopulator;
import com.mrjoshuasperry.customgeneration.populators.fossils.SpineFossilPopulator;
import com.mrjoshuasperry.customgeneration.populators.ores.ComplexVeinPopulator;
import com.mrjoshuasperry.customgeneration.populators.ores.SimpleVeinPopulator;
import com.mrjoshuasperry.customgeneration.populators.rocks.BigRockPopulator;
import com.mrjoshuasperry.customgeneration.populators.rocks.SmallRockPopulator;
import com.mrjoshuasperry.customgeneration.populators.structures.AquaductPopulator;
import com.mrjoshuasperry.customgeneration.populators.structures.PortalPopulator;
import com.mrjoshuasperry.customgeneration.populators.structures.TemplePopulator;
import com.mrjoshuasperry.mcutils.classes.Pair;

public class DesolationGenerator extends ChunkGenerator {
        public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid grid) {
                ChunkData chunk = super.createChunkData(world);

                SimplexOctaveGenerator simplex = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
                simplex.setScale(0.01D);

                for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                                int blockX = x + chunkX * 16;
                                int blockZ = z + chunkZ * 16;

                                double value = simplex.noise(blockX, blockZ, 0.5D, 0.5D, true);
                                int y = (int) ((value + 1.0D) * 15.0D + 40.0D);

                                // set the top layer
                                chunk.setBlock(x, y, z, Material.WHITE_CONCRETE_POWDER);

                                // mix sand and concrete for the layers under the top
                                for (int index = y - 1; index > y - 3; index--) {
                                        chunk.setBlock(x, index, z,
                                                        this.getMaterial(random, Material.WHITE_CONCRETE_POWDER,
                                                                        30D, Material.SAND));
                                }

                                // transition into rocks
                                for (int index = y - 3; index > 30; index--) {
                                        chunk.setBlock(x, index, z, this.getMaterial(random, Material.SANDSTONE, 20D,
                                                        Material.STONE));
                                }

                                // lower levels have more stone than sandstone
                                for (int index = 30; index > 10; index--) {
                                        chunk.setBlock(x, index, z, this.getMaterial(random, Material.STONE, 15D,
                                                        Material.SANDSTONE));
                                }

                                // add a small mix of obsidian near the bottom
                                for (int index = 10; index > 2; index--) {
                                        chunk.setBlock(x, index, z, this.getMaterial(random, Material.STONE, 5D,
                                                        Material.OBSIDIAN));
                                }

                                // create the bedrock bottom
                                chunk.setBlock(x, 2, z,
                                                this.getMaterial(random, Material.BEDROCK, 75D, Material.STONE));
                                chunk.setBlock(x, 1, z,
                                                this.getMaterial(random, Material.BEDROCK, 25D, Material.STONE));
                                chunk.setBlock(x, 0, z, Material.BEDROCK);

                                grid.setBiome(x, z, Biome.DESERT);
                        }
                }

                return chunk;
        }

        public List<BlockPopulator> getDefaultPopulators(World world) {
                // top layer material
                List<Pair<Material, Byte>> surface = new ArrayList<>(
                                Collections.singleton(new Pair<>(Material.WHITE_CONCRETE_POWDER, (byte) 0)));

                // main underground blocks
                List<Pair<Material, Byte>> undergroundSurface = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.STONE, (byte) 0),
                                new Pair<>(Material.SANDSTONE, (byte) 0)));

                // stone types for rock populators
                List<Pair<Material, Byte>> stones = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.STONE, (byte) 3),
                                new Pair<>(Material.STONE, (byte) 4)));

                // all materials in the top most layers
                List<Pair<Material, Byte>> surfaceTop = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.WHITE_CONCRETE_POWDER, (byte) 0),
                                new Pair<>(Material.AIR, (byte) 0),
                                new Pair<>(Material.SAND, (byte) 0)));

                // flooring materials for ruins
                List<Pair<Material, Byte>> floor = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.SANDSTONE, (byte) 0),
                                new Pair<>(Material.SANDSTONE, (byte) 2),
                                new Pair<>(Material.WHITE_CONCRETE_POWDER, (byte) 0)));

                // feature blocks for ruins
                List<Pair<Material, Byte>> features = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.CAULDRON, (byte) 0),
                                new Pair<>(Material.CHEST, (byte) 0),
                                new Pair<>(Material.ENCHANTING_TABLE, (byte) 0),
                                new Pair<>(Material.BOOKSHELF, (byte) 0),
                                new Pair<>(Material.COBWEB, (byte) 0),
                                new Pair<>(Material.IRON_BARS, (byte) 0),
                                new Pair<>(Material.CRAFTING_TABLE, (byte) 0),
                                new Pair<>(Material.JUKEBOX, (byte) 0)));

                // most common blocks to replace
                List<Pair<Material, Byte>> replace = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.AIR, (byte) 0),
                                new Pair<>(Material.WHITE_CONCRETE_POWDER, (byte) 0),
                                new Pair<>(Material.SAND, (byte) 0),
                                new Pair<>(Material.SANDSTONE, (byte) 0),
                                new Pair<>(Material.STONE, (byte) 0)));

                // most underground blocks
                List<Pair<Material, Byte>> caves = new ArrayList<>(Arrays.asList(
                                new Pair<>(Material.STONE, (byte) 0),
                                new Pair<>(Material.SANDSTONE, (byte) 0),
                                new Pair<>(Material.LAVA, (byte) 0),
                                new Pair<>(Material.COAL_ORE, (byte) 0),
                                new Pair<>(Material.IRON_ORE, (byte) 0),
                                new Pair<>(Material.DIAMOND_ORE, (byte) 0),
                                new Pair<>(Material.SILVERFISH_SPAWN_EGG, (byte) 0)));

                return Arrays.asList(
                                // structures
                                new PortalPopulator(235, 8, 7, 15,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.OBSIDIAN, (byte) 0))),
                                                surface,
                                                replace),
                                new AquaductPopulator(235, 2, 75, 7, 13, 7, 10, 0, 9,
                                                stones,
                                                surface,
                                                replace),
                                new TemplePopulator(235,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.SEA_LANTERN, (byte) 0))),
                                                stones,
                                                surface,
                                                replace),

                                // fossils
                                new SpineFossilPopulator(150, 5, 12,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.BONE_BLOCK, (byte) 3))),
                                                surface,
                                                replace),
                                new RibcageFossilPopulator(150, 5, 3,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.BONE_BLOCK, (byte) 3))),
                                                surface,
                                                replace),
                                new HandFossilPopulator(150, 2,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.BONE_BLOCK, (byte) 3))),
                                                surface,
                                                replace),

                                // lava
                                new ComplexVeinPopulator(1, 3, 0, 40, 5, 60, 40,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.LAVA, (byte) 0))),
                                                undergroundSurface,
                                                undergroundSurface),

                                // ores
                                new ComplexVeinPopulator(1, 5, 0, 64, 10, 35, 40,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.COAL_ORE, (byte) 0))),
                                                undergroundSurface,
                                                undergroundSurface),
                                new SimpleVeinPopulator(1, 25, 0, 64,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.IRON_ORE, (byte) 0))),
                                                undergroundSurface,
                                                undergroundSurface),
                                new SimpleVeinPopulator(1, 4, 0, 10,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.DIAMOND_ORE, (byte) 0))),
                                                undergroundSurface,
                                                undergroundSurface),

                                // silverfish
                                new SimpleVeinPopulator(1, 25, 0, 64,
                                                new ArrayList<>(Collections.singleton(
                                                                new Pair<>(Material.SILVERFISH_SPAWN_EGG, (byte) 0))),
                                                undergroundSurface,
                                                undergroundSurface),
                                new ComplexVeinPopulator(1, 5, 0, 64, 10, 25, 50,
                                                new ArrayList<>(Collections.singleton(
                                                                new Pair<>(Material.SILVERFISH_SPAWN_EGG, (byte) 0))),
                                                undergroundSurface,
                                                undergroundSurface),

                                // caves
                                new CavePopulator(1, 50, 0, 40, 25, 50, 1, 4, 20,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.SEA_LANTERN, (byte) 0))),
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.AIR, (byte) 0))),
                                                caves,
                                                caves),

                                // ruins
                                new RuinPopulator(30, 20, 4, 8, 1, 4,
                                                floor,
                                                features,
                                                stones,
                                                surface,
                                                surfaceTop),

                                // trees
                                new DeadTreePopulator(40, 9, 12, 5, 10,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.OAK_LOG, (byte) 0))),
                                                surface,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.AIR, (byte) 0)))),

                                // rocks
                                new BigRockPopulator(15, 4, 8,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.STONE, (byte) 3))),
                                                surface,
                                                replace),
                                new SmallRockPopulator(7,
                                                new ArrayList<>(Collections
                                                                .singleton(new Pair<>(Material.STONE, (byte) 3))),
                                                surface,
                                                replace));
        }

        private Material getMaterial(Random random, Material first, double chance, Material... materials) {
                return random.nextDouble() * 100.0D > chance ? first : materials[random.nextInt(materials.length)];
        }
}
