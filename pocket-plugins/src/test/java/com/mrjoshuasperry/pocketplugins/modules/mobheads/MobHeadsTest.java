package com.mrjoshuasperry.pocketplugins.modules.mobheads;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobHeadsTest {
  private static final double EPS = 1e-9;

  @Test
  void noLootingLeavesBaseChanceAlone() {
    assertEquals(2.5, MobHeads.chanceFor(2.5, 0, 1.0), EPS);
  }

  @Test
  void lootingAddsBonusPerLevel() {
    assertEquals(5.5, MobHeads.chanceFor(2.5, 3, 1.0), EPS);
  }

  @Test
  void chanceClampsToOneHundred() {
    assertEquals(100.0, MobHeads.chanceFor(99.0, 3, 5.0), EPS);
  }

  @Test
  void negativeBonusCannotDriveChanceBelowZero() {
    assertEquals(0.0, MobHeads.chanceFor(1.0, 3, -5.0), EPS);
  }

  @Test
  void zeroBonusIgnoresLootingEntirely() {
    assertEquals(2.5, MobHeads.chanceFor(2.5, 5, 0.0), EPS);
  }

  @Test
  void singleWordHeadNameIsCapitalized() {
    assertEquals("Cow Head", MobHeads.headName("cow"));
  }

  @Test
  void multiWordHeadNameCapitalizesEachWord() {
    assertEquals("Zombie Villager Head", MobHeads.headName("zombie_villager"));
  }

  @Test
  void variantPrefixBecomesPartOfTheName() {
    assertEquals("Warm Cow Head", MobHeads.headName("warm_cow"));
  }

  @Test
  void multiWordVariantAndSpeciesEachCapitalize() {
    assertEquals("Light Blue Sheep Head", MobHeads.headName("light_blue_sheep"));
  }
}
