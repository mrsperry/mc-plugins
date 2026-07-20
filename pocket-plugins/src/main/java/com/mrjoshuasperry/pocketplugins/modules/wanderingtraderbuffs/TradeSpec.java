package com.mrjoshuasperry.pocketplugins.modules.wanderingtraderbuffs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

/**
 * A single configured trade, kept as an immutable description rather than a
 * ready-made {@link MerchantRecipe}.
 *
 * <p>
 * {@code MerchantRecipe} tracks how many times it has been used, so handing the
 * same instance to every trader that spawns would let one player's purchases
 * exhaust the trade for everyone. {@link #toRecipe(Random)} builds a fresh one
 * per trader instead, which is also where the stock roll happens.
 *
 * @author mrsperry
 */
record TradeSpec(ItemStack result, List<ItemStack> ingredients, int minUses, int maxUses, boolean givesExperience) {
  /** Vanilla merchants take one or two ingredients per trade. */
  private static final int MAX_INGREDIENTS = 2;

  private static final int DEFAULT_MAX_USES = 3;

  /**
   * Rolls this trade's stock in {@code [minUses, maxUses]}, so two traders
   * carrying the same trade need not stock the same amount of it. The random
   * source is a parameter rather than the plugin's so the roll is testable.
   */
  MerchantRecipe toRecipe(Random random) {
    int uses = this.minUses == this.maxUses ? this.maxUses : random.nextInt(this.minUses, this.maxUses + 1);

    MerchantRecipe recipe = new MerchantRecipe(this.result.clone(), uses);
    recipe.setExperienceReward(this.givesExperience);

    for (ItemStack ingredient : this.ingredients) {
      recipe.addIngredient(ingredient.clone());
    }

    return recipe;
  }

  /**
   * Parses one entry of the {@code trades} list. Throws rather than returning
   * null so the caller can log which trade was bad and why, and keep the rest.
   */
  static TradeSpec parse(Map<?, ?> raw) {
    if (!(raw.get("ingredients") instanceof List<?> rawIngredients) || rawIngredients.isEmpty()) {
      throw new IllegalArgumentException("`ingredients` must be a list of at least one item");
    }

    if (rawIngredients.size() > MAX_INGREDIENTS) {
      throw new IllegalArgumentException("a trade takes at most " + MAX_INGREDIENTS + " ingredients");
    }

    List<ItemStack> ingredients = new ArrayList<>();
    for (Object rawIngredient : rawIngredients) {
      ingredients.add(parseItem(rawIngredient, "ingredient"));
    }

    ItemStack result = parseItem(raw.get("result"), "result");

    int maxUses = DEFAULT_MAX_USES;
    if (raw.get("max-uses") instanceof Number rawMaxUses) {
      maxUses = rawMaxUses.intValue();
    }

    // Omitting `min-uses` pins the stock to `max-uses` rather than rolling it
    int minUses = maxUses;
    if (raw.get("min-uses") instanceof Number rawMinUses) {
      minUses = rawMinUses.intValue();
    }

    if (minUses < 1) {
      throw new IllegalArgumentException("`min-uses` must be at least 1");
    }

    if (minUses > maxUses) {
      throw new IllegalArgumentException("`min-uses` must not exceed `max-uses`");
    }

    boolean givesExperience = !(raw.get("gives-experience") instanceof Boolean rawGivesExperience)
        || rawGivesExperience;

    return new TradeSpec(result, List.copyOf(ingredients), minUses, maxUses, givesExperience);
  }

  /** Each item is a single {@code material: amount} pair. */
  private static ItemStack parseItem(Object raw, String label) {
    if (!(raw instanceof Map<?, ?> map) || map.size() != 1) {
      throw new IllegalArgumentException("each " + label + " must be a single `material: amount` pair");
    }

    Map.Entry<?, ?> entry = map.entrySet().iterator().next();
    String name = String.valueOf(entry.getKey());

    Material material = Material.matchMaterial(name);
    if (material == null || material.isAir() || !material.isItem()) {
      throw new IllegalArgumentException("unknown " + label + " material: " + name);
    }

    if (!(entry.getValue() instanceof Number rawAmount)) {
      throw new IllegalArgumentException("amount for " + label + " " + name + " must be a number");
    }

    int amount = rawAmount.intValue();
    if (amount < 1 || amount > material.getMaxStackSize()) {
      throw new IllegalArgumentException(
          "amount for " + label + " " + name + " must be between 1 and " + material.getMaxStackSize());
    }

    return new ItemStack(material, amount);
  }
}
