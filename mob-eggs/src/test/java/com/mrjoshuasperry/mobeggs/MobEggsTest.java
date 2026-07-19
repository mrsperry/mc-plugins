package com.mrjoshuasperry.mobeggs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import com.mrjoshuasperry.mobeggs.MobCapture.Denial;

class MobEggsTest {
  @Test
  void derivesSpawnEggForMobsThatHaveOne() {
    assertEquals(Material.CHICKEN_SPAWN_EGG, MobCapture.spawnEggMaterial(EntityType.CHICKEN));
    assertEquals(Material.CREEPER_SPAWN_EGG, MobCapture.spawnEggMaterial(EntityType.CREEPER));
  }

  @Test
  void returnsNullForEntitiesWithoutASpawnEgg() {
    assertNull(MobCapture.spawnEggMaterial(EntityType.PLAYER));
    assertNull(MobCapture.spawnEggMaterial(EntityType.ARMOR_STAND));
  }

  @Test
  void bossGuardIsNotRedundant() {
    // The ender dragon does have a spawn egg material, so the type lookup alone
    // would happily let it be captured - the explicit boss check is what stops it
    assertEquals(Material.ENDER_DRAGON_SPAWN_EGG, MobCapture.spawnEggMaterial(EntityType.ENDER_DRAGON));
    assertEquals(Denial.BOSS, MobCapture.denialForType(EntityType.ENDER_DRAGON, Set.of()));
  }

  @Test
  void prettifiesVariantNames() {
    assertEquals("Red Sandstone", MobCapture.prettify("RED_SANDSTONE"));
    assertEquals("Ashen", MobCapture.prettify("ashen"));
    assertEquals("", MobCapture.prettify(""));
  }

  @Test
  void describesBothKeyedAndEnumVariants() {
    // Enum-backed variant
    assertEquals("Creamy", MobCapture.describe(org.bukkit.entity.Horse.Color.CREAMY));
    // Registry-backed Keyed variant
    assertEquals("Armorer", MobCapture.describe(org.bukkit.entity.Villager.Profession.ARMORER));
    assertNull(MobCapture.describe(null));
  }

  @Test
  void formatsWholeHealthWithoutADecimal() {
    assertEquals("20", MobCapture.formatNumber(20.0));
    assertEquals("12.5", MobCapture.formatNumber(12.5));
  }

  @Test
  void refusesBossesAndPlayers() {
    // Bosses are rejected on type before the spawn-egg lookup would reject them
    assertEquals(Denial.BOSS, MobCapture.denialForType(EntityType.WITHER, Set.of()));
    assertEquals(Denial.BOSS, MobCapture.denialForType(EntityType.ENDER_DRAGON, Set.of()));
    assertEquals(Denial.PLAYER, MobCapture.denialForType(EntityType.PLAYER, Set.of()));
  }

  @Test
  void refusesBlacklistedAndEgglessTypes() {
    assertEquals(Denial.BLACKLISTED, MobCapture.denialForType(EntityType.VEX, Set.of(EntityType.VEX)));
    assertEquals(Denial.NO_SPAWN_EGG, MobCapture.denialForType(EntityType.ARMOR_STAND, Set.of()));
  }

  @Test
  void allowsOrdinaryMobs() {
    assertNull(MobCapture.denialForType(EntityType.CHICKEN, Set.of()));
    assertNull(MobCapture.denialForType(EntityType.CREEPER, Set.of(EntityType.VEX)));
  }
}
