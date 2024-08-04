package com.mrjoshuasperry.pocketplugins.utils;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class CraftingUtil {

    private CraftingUtil() {
    }

    public static void addShapelessCrafting(String name, Map<Material, Integer> ingredients, ItemStack result) {
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(PocketPlugins.getInstance(), name), result);
        ingredients.forEach((mat, amount) -> recipe.addIngredient(amount, mat));
        Bukkit.getServer().addRecipe(recipe);
    }

    public static void addShapedCrafting(String name, Map<Character, Material> ingredients, ItemStack result,
            String... shape) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(PocketPlugins.getInstance(), name), result);
        recipe.shape(shape);
        ingredients.forEach(recipe::setIngredient);
        Bukkit.getServer().addRecipe(recipe);
    }

    public static Material[] repeat(Material mat, int count) {
        Material[] result = new Material[count];

        Arrays.fill(result, mat);
        return result;
    }
}
