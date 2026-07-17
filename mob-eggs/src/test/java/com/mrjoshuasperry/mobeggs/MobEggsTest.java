package com.mrjoshuasperry.mobeggs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class MobEggsTest {
  @Test
  void derivesSpawnEggForMobsThatHaveOne() {
    assertEquals(Material.CHICKEN_SPAWN_EGG, Main.spawnEggMaterial(EntityType.CHICKEN));
    assertEquals(Material.CREEPER_SPAWN_EGG, Main.spawnEggMaterial(EntityType.CREEPER));
  }

  @Test
  void returnsNullForEntitiesWithoutASpawnEgg() {
    assertNull(Main.spawnEggMaterial(EntityType.PLAYER));
    assertNull(Main.spawnEggMaterial(EntityType.ARMOR_STAND));
  }
}
