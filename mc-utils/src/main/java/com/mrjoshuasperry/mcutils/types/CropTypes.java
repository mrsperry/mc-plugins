package com.mrjoshuasperry.mcutils.types;

import java.util.ArrayList;

import org.bukkit.Material;

import com.google.common.collect.Lists;

public class CropTypes {
    private static ArrayList<Material> harvestable = Lists.newArrayList(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);

    private static ArrayList<Material> breakable = Lists.newArrayList(
            Material.MELON, Material.PUMPKIN, Material.COCOA_BEANS, Material.SUGAR_CANE, Material.BAMBOO,
            Material.CACTUS, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM);

    private static ArrayList<Material> clickable = Lists.newArrayList(
            Material.SWEET_BERRY_BUSH);

    private static ArrayList<Material> seeds = Lists.newArrayList(
            Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS);

    private static ArrayList<Material> saplings = Lists.newArrayList(
            Material.ACACIA_SAPLING, Material.BIRCH_SAPLING, Material.DARK_OAK_SAPLING, Material.JUNGLE_SAPLING,
            Material.OAK_SAPLING,
            Material.SPRUCE_SAPLING);

    public static ArrayList<Material> getAllTypes() {
        ArrayList<Material> types = CropTypes.harvestable;
        types.addAll(CropTypes.breakable);
        types.addAll(CropTypes.clickable);
        types.addAll(CropTypes.seeds);
        types.addAll(CropTypes.saplings);

        return types;
    }

    public static ArrayList<Material> getHarvestableTypes() {
        return CropTypes.harvestable;
    }

    public static ArrayList<Material> getBreakableTypes() {
        return CropTypes.breakable;
    }

    public static ArrayList<Material> getClickableTypes() {
        return CropTypes.clickable;
    }

    public static ArrayList<Material> getSeedTypes() {
        return CropTypes.seeds;
    }

    public static ArrayList<Material> getSaplingTypes() {
        return CropTypes.saplings;
    }
}
