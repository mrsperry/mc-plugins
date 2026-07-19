package com.mrjoshuasperry.mcutils.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.junit.jupiter.api.Test;

/**
 * Guards the hand-maintained content tables against the game registry.
 *
 * <p>
 * These tables enumerate game content by hand, so new mobs, tools and blocks do not
 * break the build — they just silently fall out of whatever the table feeds. That is
 * how the copper tool tier shipped in Paper 26.1.2 without copper hoes being able to
 * harvest crops, and how {@link EntityTypes} drifted 24 mobs behind.
 *
 * <p>
 * Each test derives the truth from the live registry and diffs the table against it,
 * so the failure arrives on the first build after a Paper bump rather than in-game.
 * A failure here is a prompt to classify the new content, not a bug in itself — see
 * UPGRADING.md.
 *
 * <p>
 * Note {@code ColorTypes} is deliberately absent: it is an HTML/CSS palette
 * (olive, teal, fuchsia), not a mirror of Minecraft's dye colors, so it cannot drift.
 */
class RegistryDriftTest {
  /** Alive, spawnable, and an actual creature — excludes Armor Stands and Mannequins. */
  private static boolean isMob(EntityType type) {
    Class<?> entityClass = type.getEntityClass();
    return type.isAlive() && type.isSpawnable() && entityClass != null
        && Mob.class.isAssignableFrom(entityClass);
  }

  private static Set<String> names(List<EntityType> types) {
    return types.stream().map(EntityType::name).collect(Collectors.toCollection(TreeSet::new));
  }

  @Test
  void everySpawnableMobIsClassified() {
    Set<String> expected = new TreeSet<>();
    for (EntityType type : EntityType.values()) {
      if (isMob(type)) {
        expected.add(type.name());
      }
    }

    assertEquals(expected, names(EntityTypes.getAllTypes()),
        "EntityTypes has drifted from the mob registry — add the new mobs to the hostile or neutral list");
  }

  @Test
  void hostileAndNeutralSplitFollowsTheEnemyInterface() {
    for (EntityType type : EntityTypes.getHostileTypes()) {
      assertTrue(Enemy.class.isAssignableFrom(type.getEntityClass()),
          type.name() + " is listed as hostile but is not an Enemy");
    }
    for (EntityType type : EntityTypes.getNeutralTypes()) {
      assertTrue(!Enemy.class.isAssignableFrom(type.getEntityClass()),
          type.name() + " is listed as neutral but is an Enemy");
    }
  }

  @Test
  void everyToolIsCategorized() {
    Set<String> expected = new TreeSet<>();
    for (Material material : Material.values()) {
      String name = material.name();
      boolean isTool = name.endsWith("_SWORD") || name.endsWith("_PICKAXE")
          || name.endsWith("_SHOVEL") || name.endsWith("_AXE") || name.endsWith("_HOE");
      // Legacy materials are the pre-1.13 enum entries kept only for deserialization.
      if (isTool && !material.isLegacy()) {
        expected.add(name);
      }
    }

    Set<String> actual = ToolTypes.getAllToolTypes().stream().map(Material::name)
        .collect(Collectors.toCollection(TreeSet::new));

    assertEquals(expected, actual,
        "ToolTypes has drifted — a tool tier was added or removed. Copper arrived this way in 26.1.2");
  }

  @Test
  void everyToolIsReachableByToolClass() {
    // The per-tier lists and the per-class lists are maintained separately; a tier
    // added to one but not the other is invisible until something indexes by class.
    // CropTweaks gates crop harvesting on getHoeTypes(), so a missing hoe is a bug.
    Set<String> byClass = new TreeSet<>();
    byClass.addAll(ToolTypes.getSwordTypes().stream().map(Material::name).toList());
    byClass.addAll(ToolTypes.getPickaxeTypes().stream().map(Material::name).toList());
    byClass.addAll(ToolTypes.getShovelTypes().stream().map(Material::name).toList());
    byClass.addAll(ToolTypes.getAxeTypes().stream().map(Material::name).toList());
    byClass.addAll(ToolTypes.getHoeTypes().stream().map(Material::name).toList());

    Set<String> byTier = ToolTypes.getAllToolTypes().stream().map(Material::name)
        .collect(Collectors.toCollection(TreeSet::new));

    assertEquals(byTier, byClass, "The per-tier and per-class tool lists disagree");
  }

  @Test
  void everySaplingIsListed() {
    Set<String> expected = new TreeSet<>();
    for (Material material : Material.values()) {
      String name = material.name();
      boolean isSapling = name.endsWith("_SAPLING") || name.endsWith("_PROPAGULE");
      // POTTED_* are the placed flowerpot blocks, not the plantable item. Bamboo is
      // modelled as a crop rather than a sapling, so BAMBOO_SAPLING stays out too.
      if (isSapling && !material.isLegacy() && !name.startsWith("POTTED_")
          && !name.equals("BAMBOO_SAPLING")) {
        expected.add(name);
      }
    }

    Set<String> actual = CropTypes.getSaplingTypes().stream().map(Material::name)
        .collect(Collectors.toCollection(TreeSet::new));

    assertEquals(expected, actual, "CropTypes saplings have drifted — a new wood type was added");
  }
}
