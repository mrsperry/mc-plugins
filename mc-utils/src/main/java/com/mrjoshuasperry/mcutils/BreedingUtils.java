package com.mrjoshuasperry.mcutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import com.google.common.collect.Lists;

public class BreedingUtils {
    // Materials that activate love mode for an animal
    private static HashMap<EntityType, ArrayList<Material>> breedingMaterials = new HashMap<>(Map.ofEntries(
            Map.entry(EntityType.CAT, Lists.newArrayList(Material.COD, Material.SALMON)),
            Map.entry(EntityType.CHICKEN,
                    Lists.newArrayList(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS,
                            Material.PUMPKIN_SEEDS)),
            Map.entry(EntityType.COW, Lists.newArrayList(Material.WHEAT)),
            Map.entry(EntityType.DONKEY,
                    Lists.newArrayList(Material.GOLDEN_CARROT, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE)),
            Map.entry(EntityType.FOX, Lists.newArrayList(Material.SWEET_BERRIES)),
            Map.entry(EntityType.LLAMA, Lists.newArrayList(Material.HAY_BLOCK)),
            Map.entry(EntityType.MOOSHROOM, Lists.newArrayList(Material.WHEAT)),
            Map.entry(EntityType.OCELOT, Lists.newArrayList(Material.COD, Material.SALMON)),
            Map.entry(EntityType.PANDA, Lists.newArrayList(Material.BAMBOO)),
            Map.entry(EntityType.PARROT,
                    Lists.newArrayList(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS, Material.MELON_SEEDS,
                            Material.PUMPKIN_SEEDS)),
            Map.entry(EntityType.PIG, Lists.newArrayList(Material.CARROT, Material.POTATO, Material.BEETROOT)),
            Map.entry(EntityType.RABBIT,
                    Lists.newArrayList(Material.CARROT, Material.GOLDEN_CARROT, Material.DANDELION)),
            Map.entry(EntityType.SHEEP, Lists.newArrayList(Material.WHEAT)),
            Map.entry(EntityType.TRADER_LLAMA, Lists.newArrayList(Material.HAY_BLOCK)),
            Map.entry(EntityType.TURTLE, Lists.newArrayList(Material.SEAGRASS, Material.TALL_SEAGRASS)),
            Map.entry(EntityType.WOLF,
                    Lists.newArrayList(Material.CHICKEN, Material.COOKED_CHICKEN, Material.PORKCHOP,
                            Material.COOKED_PORKCHOP, Material.BEEF,
                            Material.COOKED_BEEF, Material.ROTTEN_FLESH, Material.MUTTON, Material.COOKED_MUTTON,
                            Material.RABBIT, Material.COOKED_RABBIT)),
            Map.entry(EntityType.HORSE, Lists.newArrayList(Material.GOLDEN_CARROT, Material.GOLDEN_APPLE,
                    Material.ENCHANTED_GOLDEN_APPLE))));

    // Materials that can be consumed without activating the love mode of the animal
    private static HashMap<EntityType, ArrayList<Material>> consumableMaterials = new HashMap<>(Map.ofEntries(
            Map.entry(EntityType.HORSE,
                    Lists.newArrayList(Material.SUGAR, Material.WHEAT, Material.APPLE, Material.HAY_BLOCK)),
            Map.entry(EntityType.PARROT, Lists.newArrayList(Material.COOKIE)),
            Map.entry(EntityType.WOLF, Lists.newArrayList(Material.BONE))));

    public static HashMap<EntityType, ArrayList<Material>> getBreedingMaterials() {
        return BreedingUtils.breedingMaterials;
    }

    /**
     * Checks if the given material can be used to breed the given entity type
     * 
     * @param type     The entity type to breed
     * @param material The material used to breed
     * @return If the entity type can be bred with the material
     */
    public static boolean checkBreedingMaterial(EntityType type, Material material) {
        ArrayList<Material> materials = BreedingUtils.getBreedingMaterials().getOrDefault(type, null);
        if (material != null) {
            return materials.contains(material);
        }

        return false;
    }

    /**
     * Adds a breedable type to the global map
     * 
     * @param type     The entity type that can be bred
     * @param material The material used to breed
     */
    public static void addBreedableType(EntityType type, Material material) {
        if (BreedingUtils.breedingMaterials.containsKey(type)) {
            BreedingUtils.addBreedingMaterial(type, material);
        } else {
            BreedingUtils.breedingMaterials.put(type, Lists.newArrayList(material));
        }
    }

    /**
     * Adds a breedable type to the global map
     * 
     * @param type      The entity type that can be bred
     * @param materials The materials used to breed
     */
    public static void addBreedableType(EntityType type, ArrayList<Material> materials) {
        if (BreedingUtils.breedingMaterials.containsKey(type)) {
            BreedingUtils.addBreedingMaterials(type, materials);
        } else {
            BreedingUtils.breedingMaterials.put(type, materials);
        }
    }

    /**
     * Adds a breeding material to the global map
     * 
     * @param type     The entity type this material can be used on
     * @param material The material to be added
     */
    public static void addBreedingMaterial(EntityType type, Material material) {
        ArrayList<Material> values = breedingMaterials.getOrDefault(type, null);
        if (values != null) {
            values.add(material);
        }
    }

    /**
     * Adds breeding materials to the global map
     * 
     * @param type      The entity type these materials can be used on
     * @param materials The materials to be added
     */
    public static void addBreedingMaterials(EntityType type, ArrayList<Material> materials) {
        ArrayList<Material> values = breedingMaterials.getOrDefault(type, null);
        if (values != null) {
            values.addAll(materials);
        }
    }

    public static HashMap<EntityType, ArrayList<Material>> getConsumableMaterials() {
        return BreedingUtils.consumableMaterials;
    }

    /**
     * Checks if the given material can be consumed by the given entity type
     * 
     * @param type     The entity type that will consume
     * @param material The material that can be consumed
     * @return If the entity type can consume the material
     */
    public static boolean checkConsumableMaterial(EntityType type, Material material) {
        ArrayList<Material> materials = BreedingUtils.getConsumableMaterials().getOrDefault(type, null);
        if (material != null) {
            return materials.contains(material);
        }

        return false;
    }

    /**
     * Adds a consumable type to the global map
     * 
     * @param type     The entity type that can consume
     * @param material The material that can be consumed
     */
    public static void addConsumableType(EntityType type, Material material) {
        if (BreedingUtils.consumableMaterials.containsKey(type)) {
            BreedingUtils.addConsumableMaterial(type, material);
        } else {
            BreedingUtils.consumableMaterials.put(type, Lists.newArrayList(material));
        }
    }

    /**
     * Adds consumable types to the global map
     * 
     * @param type      The entity type that can consume
     * @param materials The materials that can be consumed
     */
    public static void addConsumableType(EntityType type, ArrayList<Material> materials) {
        if (BreedingUtils.consumableMaterials.containsKey(type)) {
            BreedingUtils.addConsumableMaterials(type, materials);
        } else {
            BreedingUtils.consumableMaterials.put(type, materials);
        }
    }

    /**
     * Adds a consumable material to the global map
     * 
     * @param type     The entity type this material can be consumed by
     * @param material The material to be added
     */
    public static void addConsumableMaterial(EntityType type, Material material) {
        ArrayList<Material> values = consumableMaterials.getOrDefault(type, null);
        if (values != null) {
            values.add(material);
        }
    }

    /**
     * Adds consumable materials to the global map
     * 
     * @param type      The entity type these materials can be consumed by
     * @param materials The materials to be added
     */
    public static void addConsumableMaterials(EntityType type, ArrayList<Material> materials) {
        ArrayList<Material> values = consumableMaterials.getOrDefault(type, null);
        if (values != null) {
            values.addAll(materials);
        }
    }
}
