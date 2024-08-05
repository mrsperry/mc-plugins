package com.mrjoshuasperry.mcutils.types;

import java.util.List;

import org.bukkit.Material;

import com.google.common.collect.Lists;

public class ToolTypes {
    private static List<Material> woodTypes = Lists.newArrayList(
            Material.WOODEN_SWORD, Material.WOODEN_PICKAXE, Material.WOODEN_SHOVEL, Material.WOODEN_AXE,
            Material.WOODEN_HOE);

    private static List<Material> stoneTypes = Lists.newArrayList(
            Material.STONE_SWORD, Material.STONE_PICKAXE, Material.STONE_SHOVEL, Material.STONE_AXE,
            Material.STONE_HOE);

    private static List<Material> ironTypes = Lists.newArrayList(
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_SHOVEL, Material.IRON_AXE, Material.IRON_HOE);

    private static List<Material> goldTypes = Lists.newArrayList(
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_SHOVEL, Material.GOLDEN_AXE,
            Material.GOLDEN_HOE);

    private static List<Material> diamondTypes = Lists.newArrayList(
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_AXE,
            Material.DIAMOND_HOE);

    private static List<Material> netheriteTypes = Lists.newArrayList(Material.NETHERITE_SWORD,
            Material.NETHERITE_PICKAXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_AXE, Material.NETHERITE_HOE);

    private static List<Material> swordTypes = Lists.newArrayList(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);

    private static List<Material> pickaxeTypes = Lists.newArrayList(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE);

    private static List<Material> shovelTypes = Lists.newArrayList(
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL);

    private static List<Material> axeTypes = Lists.newArrayList(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE,
            Material.NETHERITE_AXE);

    private static List<Material> hoeTypes = Lists.newArrayList(
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE,
            Material.NETHERITE_HOE);

    public static List<Material> getAllToolTypes() {
        List<Material> types = ToolTypes.woodTypes;
        types.addAll(ToolTypes.stoneTypes);
        types.addAll(ToolTypes.ironTypes);
        types.addAll(ToolTypes.goldTypes);
        types.addAll(ToolTypes.diamondTypes);
        types.addAll(ToolTypes.netheriteTypes);

        return types;
    }

    public static List<Material> getWoodTypes() {
        return ToolTypes.woodTypes;
    }

    public static List<Material> getStoneTypes() {
        return ToolTypes.stoneTypes;
    }

    public static List<Material> getIronTypes() {
        return ToolTypes.ironTypes;
    }

    public static List<Material> getGoldTypes() {
        return ToolTypes.goldTypes;
    }

    public static List<Material> getDiamondTypes() {
        return ToolTypes.diamondTypes;
    }

    public static List<Material> getSwordTypes() {
        return ToolTypes.swordTypes;
    }

    public static List<Material> getPickaxeTypes() {
        return ToolTypes.pickaxeTypes;
    }

    public static List<Material> getShovelTypes() {
        return ToolTypes.shovelTypes;
    }

    public static List<Material> getAxeTypes() {
        return ToolTypes.axeTypes;
    }

    public static List<Material> getHoeTypes() {
        return ToolTypes.hoeTypes;
    }
}
