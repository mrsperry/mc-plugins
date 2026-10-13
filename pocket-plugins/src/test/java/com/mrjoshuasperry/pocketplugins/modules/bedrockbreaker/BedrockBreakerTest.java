package com.mrjoshuasperry.pocketplugins.modules.bedrockbreaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

class BedrockBreakerTest {
  private WorldMock world;

  @BeforeEach
  void setUp() {
    world = MockBukkit.mock().addSimpleWorld("world");
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void bedrockIsBroken() {
    Block block = world.getBlockAt(0, 100, 0);
    block.setType(Material.BEDROCK);

    assertTrue(BedrockBreaker.breakIfBedrock(block));
    assertEquals(Material.AIR, block.getType());
  }

  @Test
  void whateverReplacedTheBedrockIsLeftAlone() {
    Block block = world.getBlockAt(0, 100, 0);
    block.setType(Material.CHEST);

    assertFalse(BedrockBreaker.breakIfBedrock(block));
    assertEquals(Material.CHEST, block.getType());
  }
}
