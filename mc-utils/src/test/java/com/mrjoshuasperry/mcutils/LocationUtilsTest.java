package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class LocationUtilsTest {
  @Test
  void randomizeStaysWithinEachAxisRadius() {
    Location base = new Location(null, 100, 64, 100);

    for (int i = 0; i < 1000; i++) {
      Location result = LocationUtils.randomize(base, 2, 3, 4);

      assertTrue(result.getX() >= 98 && result.getX() <= 102, "x out of range: " + result.getX());
      assertTrue(result.getY() >= 61 && result.getY() <= 67, "y out of range: " + result.getY());
      assertTrue(result.getZ() >= 96 && result.getZ() <= 104, "z out of range: " + result.getZ());
    }
  }

  @Test
  void randomizeDoesNotMutateTheSource() {
    Location base = new Location(null, 100, 64, 100);
    LocationUtils.randomize(base, 5);

    assertEquals(100, base.getX());
    assertEquals(64, base.getY());
    assertEquals(100, base.getZ());
  }

  @Test
  void setXyzOverwritesCoordinates() {
    Location location = new Location(null, 0, 0, 0);
    LocationUtils.setXYZ(location, 1, 2, 3);

    assertEquals(1, location.getX());
    assertEquals(2, location.getY());
    assertEquals(3, location.getZ());
  }
}
