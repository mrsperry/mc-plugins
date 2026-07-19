package com.mrjoshuasperry.mcutils.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolTypesTest {
  @Test
  void categoriesHoldTheirTier() {
    assertTrue(ToolTypes.getWoodTypes().contains(Material.WOODEN_PICKAXE));
    assertTrue(ToolTypes.getSwordTypes().contains(Material.DIAMOND_SWORD));
  }

  @Test
  void getAllToolTypesCombinesEveryTier() {
    // Summed from the tier getters rather than written as a literal — a new tier
    // (copper arrived in 26.1.2) shifts the total, and the old literal 30 meant this
    // test failed on the upgrade instead of the table that was actually stale.
    int expected = ToolTypes.getWoodTypes().size()
        + ToolTypes.getStoneTypes().size()
        + ToolTypes.getCopperTypes().size()
        + ToolTypes.getIronTypes().size()
        + ToolTypes.getGoldTypes().size()
        + ToolTypes.getDiamondTypes().size()
        + ToolTypes.getNetheriteTypes().size();

    assertEquals(expected, ToolTypes.getAllToolTypes().size());
    assertTrue(ToolTypes.getAllToolTypes().contains(Material.NETHERITE_HOE));
  }

  @Test
  void getAllToolTypesDoesNotMutateSharedLists() {
    // Regression: getAllToolTypes() used to addAll onto the `woodTypes` reference,
    // permanently growing the shared static list on every call.
    int woodBefore = ToolTypes.getWoodTypes().size();
    int combinedBefore = ToolTypes.getAllToolTypes().size();

    ToolTypes.getAllToolTypes();
    ToolTypes.getAllToolTypes();

    assertEquals(woodBefore, ToolTypes.getWoodTypes().size());
    assertEquals(combinedBefore, ToolTypes.getAllToolTypes().size());
  }
}
