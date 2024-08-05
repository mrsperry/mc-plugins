package com.mrjoshuasperry.dynamiccrafting;

import com.google.common.collect.Lists;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Recipes {
    private static HashSet<ShapedRecipe> shapedRecipes = new HashSet<ShapedRecipe>();
    private static HashSet<ShapelessRecipe> shapelessRecipes = new HashSet<ShapelessRecipe>();

    private static FileConfiguration config;
    private static Logger logger;

    public static void initialize(JavaPlugin main) {
        config = main.getConfig();
        logger = main.getLogger();

        if (config.isConfigurationSection("recipes")) {
            for (String path : config.getConfigurationSection("recipes").getKeys(false)) {
                String key = "recipes." + path + ".";

                ItemStack stack = getOutputStack(key, path);
                if (stack == null) {
                    continue;
                }

                String typeString = config.getString(key + "recipe-type", "null");
                if (!typeString.equals("shaped") && !typeString.equals("shapeless")) {
                    logger.severe("Could not find a valid recipe shape: " + typeString + " in " + path);
                } else if (typeString.equals("shaped")) {
                    ShapedRecipe recipe = setShapedRecipe(new ShapedRecipe(stack), key, path);
                    if (recipe != null) {
                        shapedRecipes.add(recipe);
                    }
                } else {
                    ShapelessRecipe recipe = setShapelessRecipe(new ShapelessRecipe(stack), key, path);
                    if (recipe != null) {
                        shapelessRecipes.add(recipe);
                    }
                }
            }
        }
    }

    // Handles output materials: data values, colored named, enchantments and lore
    private static ItemStack getOutputStack(String key, String path) {
        // Material data
        MaterialData data = getMaterialData(config.getString(key + "output-material"), path);
        if (data == null) {
            return null;
        }

        // Amount
        int amount = config.getInt(key + "output-amount", 1);

        // Create item stack and get meta
        ItemStack output = new ItemStack(data.getItemType(), amount);
        output.setData(data);
        ItemMeta meta = output.getItemMeta();

        // Name
        String name = ChatColor.RESET + getColoredString(config.getString(key + ".output-name", "null"), path);
        if (!name.equals(ChatColor.RESET + "null")) {
            meta.setDisplayName(name);
        }

        // Lore
        if (config.isList(key + "output-lore")) {
            List<String> lore = Lists.newArrayList();
            for (String item : config.getStringList(key + "output-lore")) {
                lore.add(getColoredString(item, key));
            }
            meta.setLore(lore);
        }

        // Set meta
        output.setItemMeta(meta);

        // Enchantments
        if (config.isList(key + "output-enchantments")) {
            for (String enchantString : config.getStringList(key + "output-enchantments")) {
                int level = 1;
                if (enchantString.contains(":")) {
                    level = getDataValue(enchantString, path);
                    if (level == -1) {
                        logger.severe("Could not parse enchantment level: " + enchantString + " in " + path + ".output-enchantments");
                        return null;
                    }
                    enchantString = enchantString.substring(0, enchantString.indexOf(":"));
                }
                try {
                    output.addUnsafeEnchantment(Enchantment.getByName(enchantString.toUpperCase()), level);
                } catch (Exception ex) {
                    logger.severe("Could not add enchantment: " + enchantString + " in " + path + ".output-enchantments");
                    return null;
                }
            }
        }

        return output;
    }

    private static ShapedRecipe setShapedRecipe(ShapedRecipe recipe, String key, String path) {
        List<String> shapes;
        if (config.isList(key + "input-shape")) {
            shapes = config.getStringList(key + "input-shape");
            if (shapes.size() != 3) {
                logger.severe("Invalid shapes: " + shapes.size() + " in " + path + ".input-shape");
                return null;
            }
        } else {
            logger.severe("Could not find recipe shape: " + path + ".input-shape");
            return null;
        }

        HashMap<Character, MaterialData> replacements = new HashMap<Character, MaterialData>();
        if (config.isConfigurationSection(key + "input-reference")) {
            for (String reference : config.getConfigurationSection(key + "input-reference").getKeys(false)) {
                MaterialData data = getMaterialData(config.getString(key + "input-reference." + reference), path);
                if (data == null) {
                    return null;
                }
                replacements.put(reference.toCharArray()[0], data);
            }
        }

        try {
            recipe.shape(shapes.get(0), shapes.get(1), shapes.get(2));
            for (char character : replacements.keySet()) {
                recipe.setIngredient(character, replacements.get(character));
            }
        } catch (Exception ex) {
            logger.severe("Could not create recipe or set ingredients: " + path);
            return null;
        }

        return recipe;
    }

    private static ShapelessRecipe setShapelessRecipe(ShapelessRecipe recipe, String key, String path) {
        path += ".input-materials";
        if (config.isList(key + "input-materials")) {
            for (String material : config.getStringList(key + "input-materials")) {
                String[] split = material.split(" ");
                if (split.length != 2) {
                    logger.severe("Invalid material arguments:  " + material + " in " + path);
                }

                int amount;
                try {
                    amount = Integer.parseInt(split[0]);
                } catch (Exception ex) {
                    logger.severe("Could not parse material amount: " + material + " in " + path);
                    return null;
                }

                MaterialData data = getMaterialData(split[1], path);
                if (data == null) {
                    logger.severe("Could not parse material data: " + material + " in " + path);
                    return null;
                }

                recipe.addIngredient(amount, data);
            }
        } else {
            logger.severe("Could not find recipe materials: " + path);
            return null;
        }

        return recipe;
    }

    private static String getColoredString(String input, String path) {
        Pattern pattern = Pattern.compile("\\{(\\w+)}");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            try {
                input = input.replace(matcher.group(0) + " ", ChatColor.RESET + "" + ChatColor.valueOf(matcher.group(1).toUpperCase()));
            } catch (Exception ex) {
                logger.severe("Could not parse color: " + matcher.group(0) + " in " + path);
            }
        }
        return input;
    }

    private static MaterialData getMaterialData(String input, String path) {
        if (input == null) {
            logger.severe("Material is null: " + path);
            return null;
        }

        int data = 0;
        if (input.contains(":")) {
            data = getDataValue(input, path);
            if (data == -1) {
                logger.severe("Could not parse material data: " + input + " in " + path);
                return null;
            }
            input = input.substring(0, input.indexOf(":"));
        }

        Material material;
        try {
            material = Material.valueOf(input.toUpperCase());
        } catch (Exception ex) {
            logger.severe("Could not parse material: " + input + " in " + path);
            return null;
        }

        return new MaterialData(material, (byte) data);
    }

    private static int getDataValue(String input, String path) {
        try {
            return Integer.parseInt(input.split(":")[1]);
        } catch (Exception ex) {
            logger.severe("Could not parse data value: " + input + " in " + path);
            return -1;
        }
    }

    public static HashSet<ShapedRecipe> getShapedRecipes() {
        return shapedRecipes;
    }

    public static HashSet<ShapelessRecipe> getShapelessRecipes() {
        return shapelessRecipes;
    }
}
