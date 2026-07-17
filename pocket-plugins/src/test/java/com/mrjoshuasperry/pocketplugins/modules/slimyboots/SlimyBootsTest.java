package com.mrjoshuasperry.pocketplugins.modules.slimyboots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlimyBootsTest {
  @Test
  void zeroFallProducesNoLaunch() {
    assertEquals(0.0, SlimyBoots.launchVelocity(0f), 1e-9);
  }

  @Test
  void modestFallProducesAKnownLaunch() {
    // -0.0011*25 + 0.43529*5 = 2.14895 ; sqrt(0.32 * 2.14895) ~ 0.8293
    assertEquals(0.8293, SlimyBoots.launchVelocity(5f), 1e-3);
  }

  @Test
  void launchIncreasesWithFallDistanceInTheNormalRange() {
    assertTrue(SlimyBoots.launchVelocity(100f) > SlimyBoots.launchVelocity(50f));
  }

  @Test
  void extremeFallDistanceProducesNaN() {
    // Documents a latent edge: the tuned parabola turns negative past ~396 blocks,
    // so the sqrt yields NaN. Flagged for a follow-up decision, not fixed here.
    assertTrue(Double.isNaN(SlimyBoots.launchVelocity(500f)));
  }
}
