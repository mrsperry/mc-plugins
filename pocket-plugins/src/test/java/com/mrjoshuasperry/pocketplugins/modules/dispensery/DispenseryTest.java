package com.mrjoshuasperry.pocketplugins.modules.dispensery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class DispenseryTest {
  // A mock server stands up the Sound registry the switch results resolve against.
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void mapsEachBucketToItsCauldron() {
    assertEquals(Material.WATER_CAULDRON, Dispensery.getCauldronTypeForBucket(Material.WATER_BUCKET));
    assertEquals(Material.LAVA_CAULDRON, Dispensery.getCauldronTypeForBucket(Material.LAVA_BUCKET));
    assertEquals(Material.POWDER_SNOW_CAULDRON, Dispensery.getCauldronTypeForBucket(Material.POWDER_SNOW_BUCKET));
    assertEquals(Material.CAULDRON, Dispensery.getCauldronTypeForBucket(Material.BUCKET));
  }

  @Test
  void mapsEachCauldronToItsBucket() {
    assertEquals(Material.WATER_BUCKET, Dispensery.getBucketTypeForCauldron(Material.WATER_CAULDRON));
    assertEquals(Material.LAVA_BUCKET, Dispensery.getBucketTypeForCauldron(Material.LAVA_CAULDRON));
    assertEquals(Material.POWDER_SNOW_BUCKET, Dispensery.getBucketTypeForCauldron(Material.POWDER_SNOW_CAULDRON));
    assertEquals(Material.BUCKET, Dispensery.getBucketTypeForCauldron(Material.CAULDRON));
  }

  @Test
  void bucketAndCauldronMappingsRoundTrip() {
    for (Material bucket : List.of(Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.POWDER_SNOW_BUCKET)) {
      assertEquals(bucket, Dispensery.getBucketTypeForCauldron(Dispensery.getCauldronTypeForBucket(bucket)));
    }
  }

  @Test
  void fillSoundsMatchTheBucketContents() {
    assertEquals(Sound.ITEM_BUCKET_FILL_LAVA, Dispensery.getFillSoundForBucket(Material.LAVA_BUCKET));
    assertEquals(Sound.ITEM_BUCKET_FILL_POWDER_SNOW, Dispensery.getFillSoundForBucket(Material.POWDER_SNOW_BUCKET));
    assertEquals(Sound.ITEM_BUCKET_FILL, Dispensery.getFillSoundForBucket(Material.BUCKET));
  }

  @Test
  void emptySoundsMatchTheCauldronContents() {
    assertEquals(Sound.ITEM_BUCKET_EMPTY_LAVA, Dispensery.getEmptySoundForBucket(Material.LAVA_CAULDRON));
    assertEquals(Sound.ITEM_BUCKET_EMPTY_POWDER_SNOW, Dispensery.getEmptySoundForBucket(Material.POWDER_SNOW_CAULDRON));
    assertEquals(Sound.ITEM_BUCKET_EMPTY, Dispensery.getEmptySoundForBucket(Material.CAULDRON));
  }
}
