package com.mrjoshuasperry.pocketplugins.modules.extendedbeacons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.mrjoshuasperry.pocketplugins.modules.extendedbeacons.ExtendedBeacons.LayerCount;
import com.mrjoshuasperry.pocketplugins.modules.extendedbeacons.ExtendedBeacons.LayerState;

class ExtendedBeaconsTest {
  private static final double EPS = 1e-9;

  /** Layer states by layer number, starting from layer 1. */
  private static IntFunction<LayerState> layers(LayerState... states) {
    List<LayerState> list = List.of(states);
    return layer -> list.get(layer - 1);
  }

  private static IntFunction<LayerState> allComplete() {
    return layer -> LayerState.COMPLETE;
  }

  @Test
  void defaultRangeMatchesVanillaForTheFirstFourLayers() {
    assertEquals(20, ExtendedBeacons.rangeFor(1, 10, 10), EPS);
    assertEquals(30, ExtendedBeacons.rangeFor(2, 10, 10), EPS);
    assertEquals(40, ExtendedBeacons.rangeFor(3, 10, 10), EPS);
    assertEquals(50, ExtendedBeacons.rangeFor(4, 10, 10), EPS);
  }

  @Test
  void rangeKeepsGrowingPastFourLayers() {
    assertEquals(110, ExtendedBeacons.rangeFor(10, 10, 10), EPS);
  }

  @Test
  void rangePerLayerIsConfigurable() {
    assertEquals(70, ExtendedBeacons.rangeFor(3, 10, 20), EPS);
  }

  @Test
  void countStopsAtTheFirstIncompleteLayer() {
    LayerCount count = ExtendedBeacons.countLayers(0, 10,
        layers(LayerState.COMPLETE, LayerState.COMPLETE, LayerState.INCOMPLETE, LayerState.COMPLETE));

    assertEquals(2, count.layers());
    assertFalse(count.truncated());
  }

  @Test
  void countStoppedByAnUnloadedChunkIsMarkedTruncated() {
    LayerCount count = ExtendedBeacons.countLayers(0, 10, layers(LayerState.COMPLETE, LayerState.UNLOADED));

    assertEquals(1, count.layers());
    assertTrue(count.truncated());
  }

  @Test
  void countWithNoCompleteLayerIsZero() {
    LayerCount count = ExtendedBeacons.countLayers(0, 10, layers(LayerState.INCOMPLETE));

    assertEquals(0, count.layers());
    assertFalse(count.truncated());
  }

  @Test
  void unlimitedCountGoesPastVanillasFourLayers() {
    assertEquals(50, ExtendedBeacons.countLayers(0, 50, allComplete()).layers());
  }

  @Test
  void countStopsAtMaxLayers() {
    LayerCount count = ExtendedBeacons.countLayers(4, 50, allComplete());

    assertEquals(4, count.layers());
    assertFalse(count.truncated());
  }

  @Test
  void countStopsAtTheWorldFloor() {
    assertEquals(3, ExtendedBeacons.countLayers(0, 3, allComplete()).layers());
    assertEquals(3, ExtendedBeacons.countLayers(10, 3, allComplete()).layers());
  }

  @Test
  void blockDirectlyUnderTheBeaconIsInTheFirstLayer() {
    assertTrue(ExtendedBeacons.isUnderPyramid(0, 1, 0, 0));
  }

  @Test
  void eachLayerWidensByOneBlockPerSide() {
    assertTrue(ExtendedBeacons.isUnderPyramid(3, 3, -3, 0));
    assertFalse(ExtendedBeacons.isUnderPyramid(4, 3, 0, 0));
    assertFalse(ExtendedBeacons.isUnderPyramid(0, 3, -4, 0));
  }

  @Test
  void blocksLevelWithOrAboveTheBeaconAreNotInThePyramid() {
    assertFalse(ExtendedBeacons.isUnderPyramid(0, 0, 0, 0));
    assertFalse(ExtendedBeacons.isUnderPyramid(0, -2, 0, 0));
  }

  @Test
  void maxLayersLimitsThePyramidFootprint() {
    assertTrue(ExtendedBeacons.isUnderPyramid(0, 4, 0, 4));
    assertFalse(ExtendedBeacons.isUnderPyramid(0, 5, 0, 4));
    assertTrue(ExtendedBeacons.isUnderPyramid(0, 500, 0, 0));
  }
}
