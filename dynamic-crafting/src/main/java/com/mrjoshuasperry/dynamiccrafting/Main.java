package com.mrjoshuasperry.dynamiccrafting;

import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        Recipes.initialize(this);
        for (Recipe recipe : Recipes.getShapedRecipes()) {
            this.getServer().addRecipe(recipe);
        }
        for (Recipe recipe : Recipes.getShapelessRecipes()) {
            this.getServer().addRecipe(recipe);
        }
    }

    @Override
    public void onDisable() {

    }
}
