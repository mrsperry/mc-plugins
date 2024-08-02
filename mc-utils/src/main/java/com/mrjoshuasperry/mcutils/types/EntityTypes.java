package com.mrjoshuasperry.mcutils.types;

import java.util.ArrayList;

import org.bukkit.entity.EntityType;

import com.google.common.collect.Lists;

public class EntityTypes {
    private static ArrayList<EntityType> hostile = Lists.newArrayList(
            EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER,
            EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDER_DRAGON,
            EntityType.ENDERMAN, EntityType.EVOKER, EntityType.GHAST,
            EntityType.GIANT, EntityType.GUARDIAN, EntityType.HUSK,
            EntityType.HOGLIN, EntityType.ILLUSIONER, EntityType.MAGMA_CUBE,
            EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SHULKER,
            EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME,
            EntityType.SPIDER, EntityType.STRAY, EntityType.VEX,
            EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER,
            EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIE,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIE_VILLAGER);

    private static ArrayList<EntityType> neutral = Lists.newArrayList(
            EntityType.BEE, EntityType.BAT, EntityType.CAT,
            EntityType.COW, EntityType.CHICKEN, EntityType.COD,
            EntityType.DOLPHIN, EntityType.DONKEY, EntityType.FOX,
            EntityType.HORSE, EntityType.IRON_GOLEM, EntityType.LLAMA,
            EntityType.MULE, EntityType.MOOSHROOM, EntityType.OCELOT,
            EntityType.PANDA, EntityType.PARROT, EntityType.PIG,
            EntityType.POLAR_BEAR, EntityType.PUFFERFISH, EntityType.RABBIT,
            EntityType.SALMON, EntityType.SHEEP, EntityType.SNOW_GOLEM,
            EntityType.SQUID, EntityType.STRIDER, EntityType.TRADER_LLAMA,
            EntityType.TROPICAL_FISH, EntityType.TURTLE, EntityType.WANDERING_TRADER);

    public static ArrayList<EntityType> getAllTypes() {
        ArrayList<EntityType> types = hostile;
        types.addAll(neutral);
        return types;
    }

    public static ArrayList<EntityType> getHostileTypes() {
        return hostile;
    }

    public static ArrayList<EntityType> getNeutralTypes() {
        return neutral;
    }
}
