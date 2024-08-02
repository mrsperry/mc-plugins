package com.mrjoshuasperry.mcutils.types;

import java.util.ArrayList;

import org.bukkit.Material;

import com.google.common.collect.Lists;

public class ToolTypes {
    private static ArrayList<Material> woodTypes = Lists.newArrayList(
            Material.WOODEN_SWORD, Material.WOODEN_PICKAXE, Material.WOODEN_SHOVEL, Material.WOODEN_AXE,
            Material.WOODEN_HOE);

    private static ArrayList<Material> stoneTypes = Lists.newArrayList(
            Material.STONE_SWORD, Material.STONE_PICKAXE, Material.STONE_SHOVEL, Material.STONE_AXE,
            Material.STONE_HOE);

    private static ArrayList<Material> ironTypes = Lists.newArrayList(
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_SHOVEL, Material.IRON_AXE, Material.IRON_HOE);

    private static ArrayList<Material> goldTypes = Lists.newArrayList(
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_SHOVEL, Material.GOLDEN_AXE,
            Material.GOLDEN_HOE);

    private static ArrayList<Material> diamondTypes = Lists.newArrayList(
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_AXE,
            Material.DIAMOND_HOE);

    private static ArrayList<Material> swordTypes = Lists.newArrayList(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD);

    private static ArrayList<Material> pickaxeTypes = Lists.newArrayList(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE);

    private static ArrayList<Material> shovedTypes = Lists.newArrayList(
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL);

    private static ArrayList<Material> axeTypes = Lists.newArrayList(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE);

    private static ArrayList<Material> hoeTypes = Lists.newArrayList(
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE);

    public static ArrayList<Material> getAllToolTypes() {
        ArrayList<Material> types = ToolTypes.woodTypes;
        types.addAll(ToolTypes.stoneTypes);
        types.addAll(ToolTypes.ironTypes);
        types.addAll(ToolTypes.goldTypes);
        types.addAll(ToolTypes.diamondTypes);

        return types;
    }

    public static ArrayList<Material> getWoodTypes() {
        return ToolTypes.woodTypes;
    }

    public static ArrayList<Material> getStoneTypes() {
        return ToolTypes.stoneTypes;
    }

    public static ArrayList<Material> getIronTypes() {
        return ToolTypes.ironTypes;
    }

    public static ArrayList<Material> getGoldTypes() {
        return ToolTypes.goldTypes;
    }

    public static ArrayList<Material> getDiamondTypes() {
        return ToolTypes.diamondTypes;
    }

    public static ArrayList<Material> getSwordTypes() {
        return ToolTypes.swordTypes;
    }

    public static ArrayList<Material> getPickaxeTypes() {
        return ToolTypes.pickaxeTypes;
    }

    public static ArrayList<Material> getShovelTypes() {
        return ToolTypes.shovedTypes;
    }

    public static ArrayList<Material> getAxeTypes() {
        return ToolTypes.axeTypes;
    }

    public static ArrayList<Material> getHoeTypes() {
        return ToolTypes.hoeTypes;
    }
}
