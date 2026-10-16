package com.mrjoshuasperry.pocketplugins.modules.extendedblockplacement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class ExtendedBlockPlacementTest {
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void everyKindOfLeavesHoldsUpAHangingLantern() {
    for (Material leaves : Tag.LEAVES.getValues()) {
      assertTrue(ExtendedBlockPlacement.hangsFromLeaves(true, leaves), leaves.name());
    }
  }

  @Test
  void theLanternFallsOnceTheLeavesAreGone() {
    assertFalse(ExtendedBlockPlacement.hangsFromLeaves(true, Material.AIR));
    assertFalse(ExtendedBlockPlacement.hangsFromLeaves(true, Material.OAK_LOG), "anything else above is vanilla's call");
  }

  @Test
  void aStandingLanternIsLeftToVanilla() {
    assertFalse(ExtendedBlockPlacement.hangsFromLeaves(false, Material.OAK_LEAVES));
  }
}
