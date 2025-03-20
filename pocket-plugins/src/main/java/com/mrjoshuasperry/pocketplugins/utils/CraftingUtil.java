package com.mrjoshuasperry.pocketplugins.utils;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class CraftingUtil {
    public static void addShapelessCrafting(String name, Map<Material, Integer> ingredients, ItemStack result) {
        NamespacedKey key = new NamespacedKey(PocketPlugins.getInstance(), name);
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        ingredients.forEach((mat, amount) -> recipe.addIngredient(amount, mat));
        recipe.setCategory(CraftingBookCategory.MISC);
        Bukkit.getServer().addRecipe(recipe);
        PocketPlugins.getInstance().addDiscoverableCraftingKey(key);
    }

    public static void addShapedCrafting(String name, Map<Character, Material> ingredients, ItemStack result,
            String... shape) {
        NamespacedKey key = new NamespacedKey(PocketPlugins.getInstance(), name);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        ingredients.forEach(recipe::setIngredient);
        recipe.setCategory(CraftingBookCategory.MISC);
        Bukkit.getServer().addRecipe(recipe);
        PocketPlugins.getInstance().addDiscoverableCraftingKey(key);
    }

    public static Material[] repeat(Material mat, int count) {
        Material[] result = new Material[count];

        Arrays.fill(result, mat);
        return result;
    }
}
