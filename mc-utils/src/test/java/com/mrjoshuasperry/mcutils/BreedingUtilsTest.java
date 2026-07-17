package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class BreedingUtilsTest {
  @Test
  void acceptsCorrectBreedingMaterial() {
    assertTrue(BreedingUtils.checkBreedingMaterial(EntityType.COW, Material.WHEAT));
    assertTrue(BreedingUtils.checkBreedingMaterial(EntityType.PANDA, Material.BAMBOO));
  }

  @Test
  void rejectsWrongBreedingMaterial() {
    assertFalse(BreedingUtils.checkBreedingMaterial(EntityType.COW, Material.DIAMOND));
  }

  @Test
  void rejectsNullBreedingMaterial() {
    assertFalse(BreedingUtils.checkBreedingMaterial(EntityType.COW, null));
  }

  @Test
  void unknownEntityTypeIsRejectedInsteadOfThrowing() {
    // Regression: types absent from the map produced a null list and threw NPE on
    // .contains(); they must simply return false.
    assertFalse(BreedingUtils.checkBreedingMaterial(EntityType.ZOMBIE, Material.WHEAT));
    assertFalse(BreedingUtils.checkConsumableMaterial(EntityType.COW, Material.WHEAT));
  }

  @Test
  void acceptsCorrectConsumableMaterial() {
    assertTrue(BreedingUtils.checkConsumableMaterial(EntityType.HORSE, Material.SUGAR));
    assertTrue(BreedingUtils.checkConsumableMaterial(EntityType.WOLF, Material.BONE));
  }
}
