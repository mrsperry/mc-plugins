package com.mrjoshuasperry.pocketplugins.modules.wanderingtraderbuffs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.inventory.MerchantRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/** A mock server stands up the registries {@code ItemStack} resolves against. */
class TradeSpecTest {
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private static Map<?, ?> trade(Object ingredients, Object result, Object maxUses) {
    return Map.of("ingredients", ingredients, "result", result, "max-uses", maxUses);
  }

  @Test
  void parsesATwoIngredientTrade() {
    TradeSpec spec = TradeSpec.parse(
        trade(List.of(Map.of("emerald", 32), Map.of("book", 1)), Map.of("elytra", 1), 1));

    assertEquals(2, spec.ingredients().size());
    assertEquals(Material.EMERALD, spec.ingredients().get(0).getType());
    assertEquals(32, spec.ingredients().get(0).getAmount());
    assertEquals(Material.BOOK, spec.ingredients().get(1).getType());
    assertEquals(Material.ELYTRA, spec.result().getType());
    assertEquals(1, spec.maxUses());
  }

  @Test
  void defaultsUsesAndExperienceWhenAbsent() {
    TradeSpec spec = TradeSpec.parse(
        Map.of("ingredients", List.of(Map.of("emerald", 4)), "result", Map.of("apple", 2)));

    assertEquals(3, spec.maxUses());
    assertEquals(3, spec.minUses());
    assertTrue(spec.givesExperience());
  }

  @Test
  void readsMinUsesAndExperience() {
    TradeSpec spec = TradeSpec.parse(Map.of(
        "ingredients", List.of(Map.of("emerald", 4)),
        "result", Map.of("apple", 2),
        "min-uses", 2,
        "max-uses", 6,
        "gives-experience", false));

    assertEquals(2, spec.minUses());
    assertEquals(6, spec.maxUses());
    assertFalse(spec.givesExperience());
  }

  @Test
  void rejectsMalformedTrades() {
    assertThrows(IllegalArgumentException.class,
        () -> TradeSpec.parse(trade(List.of(), Map.of("apple", 1), 1)));
    assertThrows(IllegalArgumentException.class, () -> TradeSpec.parse(
        trade(List.of(Map.of("emerald", 1), Map.of("book", 1), Map.of("paper", 1)), Map.of("apple", 1), 1)));
    assertThrows(IllegalArgumentException.class,
        () -> TradeSpec.parse(trade(List.of(Map.of("not_a_material", 1)), Map.of("apple", 1), 1)));
    assertThrows(IllegalArgumentException.class,
        () -> TradeSpec.parse(trade(List.of(Map.of("emerald", "eight")), Map.of("apple", 1), 1)));
    assertThrows(IllegalArgumentException.class,
        () -> TradeSpec.parse(trade(List.of(Map.of("emerald", 0)), Map.of("apple", 1), 1)));
    assertThrows(IllegalArgumentException.class,
        () -> TradeSpec.parse(trade(List.of(Map.of("emerald", 65)), Map.of("apple", 1), 1)));
    assertThrows(IllegalArgumentException.class,
        () -> TradeSpec.parse(trade(List.of(Map.of("emerald", 1)), Map.of("apple", 1), 0)));
  }

  @Test
  void rejectsAMinUsesAboveMaxUses() {
    assertThrows(IllegalArgumentException.class, () -> TradeSpec.parse(Map.of(
        "ingredients", List.of(Map.of("emerald", 1)),
        "result", Map.of("apple", 1),
        "min-uses", 5,
        "max-uses", 4)));
  }

  @Test
  void buildsARecipeMatchingTheSpec() {
    TradeSpec spec = TradeSpec.parse(
        trade(List.of(Map.of("emerald", 8)), Map.of("diamond", 1), 5));
    MerchantRecipe recipe = spec.toRecipe(new Random());

    assertEquals(Material.DIAMOND, recipe.getResult().getType());
    assertEquals(1, recipe.getIngredients().size());
    assertEquals(Material.EMERALD, recipe.getIngredients().get(0).getType());
    assertEquals(8, recipe.getIngredients().get(0).getAmount());
    assertEquals(5, recipe.getMaxUses());
    assertTrue(recipe.hasExperienceReward());
  }

  @Test
  void rollsStockWithinTheConfiguredRange() {
    TradeSpec spec = TradeSpec.parse(Map.of(
        "ingredients", List.of(Map.of("emerald", 1)),
        "result", Map.of("apple", 1),
        "min-uses", 2,
        "max-uses", 6));
    Random random = new Random(1234);

    // Both bounds are inclusive, so a wide sample should reach each of them
    boolean sawMin = false;
    boolean sawMax = false;
    for (int roll = 0; roll < 500; roll++) {
      int uses = spec.toRecipe(random).getMaxUses();

      assertTrue(uses >= 2 && uses <= 6, "rolled " + uses + " outside [2, 6]");
      sawMin |= uses == 2;
      sawMax |= uses == 6;
    }

    assertTrue(sawMin);
    assertTrue(sawMax);
  }

  /** Sharing one recipe across traders would let one trader exhaust the trade for all of them. */
  @Test
  void buildsAFreshRecipeEachCall() {
    TradeSpec spec = TradeSpec.parse(
        trade(List.of(Map.of("emerald", 8)), Map.of("diamond", 1), 5));
    Random random = new Random();

    MerchantRecipe first = spec.toRecipe(random);
    MerchantRecipe second = spec.toRecipe(random);
    first.setUses(4);

    assertNotSame(first, second);
    assertEquals(0, second.getUses());
  }
}
