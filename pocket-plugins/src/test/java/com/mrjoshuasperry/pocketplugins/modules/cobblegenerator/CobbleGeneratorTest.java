package com.mrjoshuasperry.pocketplugins.modules.cobblegenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CobbleGeneratorTest {
  private static final List<Material> MATERIALS = List.of(Material.STONE, Material.DIRT);
  private static final List<Double> WEIGHTS = List.of(1.0, 2.0);

  @Test
  void rollInFirstBucketPicksFirstMaterial() {
    assertEquals(Material.STONE, CobbleGenerator.pick(MATERIALS, WEIGHTS, 0.5));
  }

  @Test
  void rollInSecondBucketPicksSecondMaterial() {
    assertEquals(Material.DIRT, CobbleGenerator.pick(MATERIALS, WEIGHTS, 1.5));
  }

  @Test
  void bucketBoundaryIsExclusiveOnTheLowerBucket() {
    // roll == weight[0] does not select the first bucket; it rolls into the next.
    assertEquals(Material.DIRT, CobbleGenerator.pick(MATERIALS, WEIGHTS, 1.0));
  }

  @Test
  void rollPastTheTotalFallsThroughToTheLastMaterial() {
    assertEquals(Material.DIRT, CobbleGenerator.pick(MATERIALS, WEIGHTS, 5.0));
  }

  @Test
  void singleMaterialAlwaysWins() {
    assertEquals(Material.STONE, CobbleGenerator.pick(List.of(Material.STONE), List.of(3.0), 2.0));
  }
}
