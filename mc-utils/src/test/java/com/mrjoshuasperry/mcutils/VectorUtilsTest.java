package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class VectorUtilsTest {
  private static final double EPS = 1e-9;

  @Test
  void getDirectionIsToMinusFrom() {
    Vector direction = VectorUtils.getDirection(
        new Location(null, 0, 0, 0),
        new Location(null, 1, 2, 3));

    assertEquals(1, direction.getX(), EPS);
    assertEquals(2, direction.getY(), EPS);
    assertEquals(3, direction.getZ(), EPS);
  }

  @Test
  void getDirectionHandlesNegativeDeltas() {
    Vector direction = VectorUtils.getDirection(
        new Location(null, 5, 5, 5),
        new Location(null, 2, 3, 4));

    assertEquals(-3, direction.getX(), EPS);
    assertEquals(-2, direction.getY(), EPS);
    assertEquals(-1, direction.getZ(), EPS);
  }
}
