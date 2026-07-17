package com.mrjoshuasperry.mcutils.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CropTypesTest {
  @Test
  void getSeedFromCropMapsKnownCrops() {
    assertSame(Material.WHEAT_SEEDS, CropTypes.getSeedFromCrop(Material.WHEAT));
    assertSame(Material.CARROT, CropTypes.getSeedFromCrop(Material.CARROTS));
  }

  @Test
  void getSeedFromCropReturnsNullForNonCrop() {
    assertNull(CropTypes.getSeedFromCrop(Material.STONE));
  }

  @Test
  void getCropFromSeedMapsKnownSeeds() {
    assertSame(Material.WHEAT, CropTypes.getCropFromSeed(Material.WHEAT_SEEDS));
    assertSame(Material.CARROTS, CropTypes.getCropFromSeed(Material.CARROT));
  }

  @Test
  void getCropFromSeedThrowsForUnknownSeed() {
    assertThrows(IllegalArgumentException.class, () -> CropTypes.getCropFromSeed(Material.STONE));
  }

  @Test
  void getAllTypesCombinesEveryCategory() {
    int expected = CropTypes.getHarvestableTypes().size()
        + CropTypes.getBreakableTypes().size()
        + CropTypes.getClickableTypes().size()
        + CropTypes.getSeedTypes().size()
        + CropTypes.getSaplingTypes().size();

    assertEquals(expected, CropTypes.getAllTypes().size());
  }

  @Test
  void getAllTypesDoesNotMutateSharedLists() {
    // Regression: getAllTypes() used to addAll onto the `harvestable` reference,
    // permanently growing the shared static list on every call.
    int harvestableBefore = CropTypes.getHarvestableTypes().size();
    int combinedBefore = CropTypes.getAllTypes().size();

    CropTypes.getAllTypes();
    CropTypes.getAllTypes();

    assertEquals(harvestableBefore, CropTypes.getHarvestableTypes().size());
    assertEquals(combinedBefore, CropTypes.getAllTypes().size());
  }
}
