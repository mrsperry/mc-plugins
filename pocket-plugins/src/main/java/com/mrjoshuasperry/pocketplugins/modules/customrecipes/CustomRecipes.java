package com.mrjoshuasperry.pocketplugins.modules.customrecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import com.mrjoshuasperry.mcutils.StringUtils;
import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class CustomRecipes extends Module {
  public CustomRecipes(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    Logger logger = this.getPlugin().getLogger();

    ConfigurationSection recipes = writableConfig.getConfigurationSection("recipes");
    if (recipes == null) {
      return;
    }

    for (String recipeName : recipes.getKeys(false)) {
      try {
        String recipeType = recipes.getString(recipeName + ".type");
        CraftingRecipe recipe = null;

        if (recipeType == null) {
          logger.severe("Recipe '" + recipeName + "'' does not have a type.");
          continue;
        }

        ConfigurationSection recipeSection = recipes.getConfigurationSection(recipeName);
        switch (recipeType) {
          case "shaped":
            recipe = this.parseShapedRecipe(recipeSection);
            break;
          case "shapeless":
            recipe = this.parseShapelessRecipe(recipeSection);
            break;
        }

        if (recipe == null) {
          continue;
        }

        this.registerCraftingRecipe(recipe);
      } catch (Exception ex) {
        logger.severe("Error parsing recipe '" + recipeName + "': " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  protected ItemStack parseOutput(ConfigurationSection recipeSection) {
    Logger logger = this.getPlugin().getLogger();
    String recipeName = recipeSection.getName();

    Material outputType = Material.AIR;
    try {
      outputType = Material.valueOf(StringUtils.toEnumName(recipeSection.getString("output.type")));
    } catch (Exception ex) {
      logger.warning("Invalid output type for recipe: " + recipeName);
      return null;
    }

    if (outputType == Material.AIR) {
      logger.warning("Recipe '" + recipeName + "' does not have a valid output type");
      return null;
    }

    int outputAmount;
    try {
      outputAmount = Math.min(outputType.getMaxStackSize(), recipeSection.getInt("output.amount", 1));
    } catch (Exception e) {
      logger.warning("Invalid output amount for recipe: " + recipeName);
      return null;
    }

    return new ItemStack(outputType, outputAmount);
  }

  protected Material parseMaterial(String recipeName, String material) {
    Logger logger = this.getPlugin().getLogger();
    Material ingredientType = Material.AIR;

    try {
      ingredientType = Material.valueOf(StringUtils.toEnumName(material));
    } catch (Exception ex) {
      logger.warning("Invalid ingredient type for recipe '" + recipeName + "': " + material);
      return null;
    }

    if (ingredientType.isAir()) {
      logger.warning("Invalid ingredient type for recipe '" + recipeName + "': " + material);
      return null;
    }

    return ingredientType;
  }

  protected ShapedRecipe parseShapedRecipe(ConfigurationSection recipeSection) {
    Logger logger = this.getPlugin().getLogger();
    String recipeName = recipeSection.getName();

    List<String> shape = recipeSection.getStringList("shape");
    if (shape == null) {
      logger.warning("Recipe '" + recipeName + "' does not have a shape");
      return null;
    }

    if (shape.size() != 3) {
      logger.warning("Recipe shapes must have three rows");
      return null;
    }

    List<String> uniqueIngredientKeys = new ArrayList<>();
    for (String row : shape) {
      if (row.length() != 3) {
        logger.warning("Recipe rows must have exactly three columns");
        return null;
      }

      for (String character : row.split("")) {
        if (!uniqueIngredientKeys.contains(character)) {
          uniqueIngredientKeys.add(character);
        }
      }
    }

    ItemStack output = this.parseOutput(recipeSection);
    if (output == null) {
      return null;
    }

    ShapedRecipe recipe = new ShapedRecipe(this.createKey(recipeName), output);
    recipe.shape(shape.get(0), shape.get(1), shape.get(2));

    CraftingBookCategory category;
    try {
      category = CraftingBookCategory.valueOf(StringUtils.toEnumName(recipeSection.getString("category", "misc")));
    } catch (Exception ex) {
      logger.warning("Invalid category for recipe: " + recipeName);
      return null;
    }
    recipe.setCategory(category);

    ConfigurationSection ingredients = recipeSection.getConfigurationSection("ingredients");
    for (String key : ingredients.getKeys(false)) {
      if (key.length() != 1) {
        logger
            .warning("Recipe '" + recipeName + "' has an invalid ingredient key (must be a single character): " + key);
        return null;
      }

      if (uniqueIngredientKeys.contains(key)) {
        uniqueIngredientKeys.remove(key);
      } else {
        logger.warning("Recipe '" + recipeName + "' has a missing or duplicate ingredient key: " + key);
        return null;
      }

      Material ingredientType = this.parseMaterial(recipeName, ingredients.getString(key));
      if (ingredientType == null) {
        return null;
      }

      recipe.setIngredient(key.charAt(0), ingredientType);
    }

    return recipe;
  }

  protected ShapelessRecipe parseShapelessRecipe(ConfigurationSection recipeSection) {
    Logger logger = this.getPlugin().getLogger();
    String recipeName = recipeSection.getName();

    ItemStack output = this.parseOutput(recipeSection);
    if (output == null) {
      return null;
    }

    ShapelessRecipe recipe = new ShapelessRecipe(this.createKey(recipeName), output);

    CraftingBookCategory category;
    try {
      category = CraftingBookCategory.valueOf(StringUtils.toEnumName(recipeSection.getString("category", "misc")));
    } catch (Exception ex) {
      logger.warning("Invalid category for recipe: " + recipeName);
      return null;
    }
    recipe.setCategory(category);

    List<String> ingredients = recipeSection.getStringList("ingredients");
    if (ingredients == null || ingredients.isEmpty()) {
      logger.warning("Recipe '" + recipeName + "' does not have any ingredients");
      return null;
    }

    if (ingredients.size() > 9) {
      logger.warning("Recipe '" + recipeName + "' has too many ingredients (maximum of 9)");
      return null;
    }

    for (String ingredient : ingredients) {
      Material ingredientType = this.parseMaterial(recipeName, ingredient);
      if (ingredientType == null) {
        return null;
      }

      recipe.addIngredient(ingredientType);
    }

    return recipe;
  }
}
