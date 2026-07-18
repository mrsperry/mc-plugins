package com.mrjoshuasperry.pocketplugins.modules.slimyboots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  void largeFallsPlateauAtTheMaxInsteadOfNaN() {
    // Past the parabola's peak (~198 blocks) the bounce holds at its maximum instead
    // of curving back down and eventually producing a NaN.
    double plateau = SlimyBoots.launchVelocity(250f);

    assertFalse(Double.isNaN(SlimyBoots.launchVelocity(500f)));
    assertEquals(plateau, SlimyBoots.launchVelocity(500f), 1e-9);
    assertEquals(plateau, SlimyBoots.launchVelocity(1000f), 1e-9);
  }

  @Test
  void aBiggerFallNeverBouncesLess() {
    // Regression guard for the old downslope: a 300-block fall used to bounce weaker
    // than a 100-block fall; now farther never means weaker.
    assertTrue(SlimyBoots.launchVelocity(300f) >= SlimyBoots.launchVelocity(100f));
  }
}
