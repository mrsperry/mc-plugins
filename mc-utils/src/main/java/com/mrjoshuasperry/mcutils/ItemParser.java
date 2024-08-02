package com.mrjoshuasperry.mcutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;

public class ItemParser {
    public static ItemStack parseItem(ConfigurationSection key) {
        Material material = Material.AIR;
        int amount = 1;
        short damage = 0;
        String name = "";
        List<String> lore = new ArrayList<String>();
        HashMap<Enchantment, Integer> enchantments = new HashMap<Enchantment, Integer>();

        if (key.isString(".material")) {
            String materialString = key.getString(".material");
            try {
                material = Material.valueOf(materialString.toUpperCase().replace(" ", "_"));
            } catch (Exception ex) {
                Bukkit.getLogger().warning("Could not parse material: " + materialString);
            }
        }

        if (key.isInt(".amount")) {
            String itemAmount = key.getString(".amount");
            try {
                amount = Integer.parseInt(itemAmount);
            } catch (Exception ex) {
                Bukkit.getLogger().warning("Could not parse item amount: " + itemAmount);
            }
        }

        if (key.isInt(".damage")) {
            String damageAmount = key.getString(".damage");
            try {
                damage = Short.parseShort(damageAmount);
            } catch (Exception ex) {
                Bukkit.getLogger().warning("Could not parse item damage: " + damageAmount);
            }
        }

        if (key.isString(".name")) {
            try {
                name = key.getString(".name").replace("`", "\u00A7");
            } catch (Exception ex) {
                Bukkit.getLogger().info("Could not set item name: " + name);
            }
        }

        if (key.isList(".lore")) {
            for (String line : key.getStringList(".lore")) {
                try {
                    lore.add(line.replace("`", "\u00A7"));
                } catch (Exception ex) {
                    Bukkit.getLogger().warning("Could not add lore: " + line);
                }
            }
        }

        if (key.isList(".enchantments")) {
            for (String enchantString : key.getStringList(".enchantments")) {
                String[] split = enchantString.split(":");

                int level = 0;
                if (split.length == 2) {
                    try {
                        level = Integer.parseInt(split[1]);
                    } catch (Exception ex) {
                        Bukkit.getLogger().warning("Could not parse enchantment level: " + split[1]);
                    }
                }

                Enchantment enchantment = null;
                try {
                    enchantment = Enchantment.getByName(split[0].toUpperCase().replace(" ", "_"));
                } catch (Exception ex) {
                    Bukkit.getLogger().warning("Could not parse enchantment: " + enchantString);
                }

                if (enchantment != null) {
                    enchantments.put(enchantment, level);
                }
            }
        }

        ItemBuilder builder = new ItemBuilder(material).setAmount(amount).setData(damage);
        if (!name.equals("")) {
            builder.setName(name);
        }
        if (lore.size() > 0) {
            builder.setLore(lore);
        }
        if (enchantments.size() > 0) {
            if (material != Material.ENCHANTED_BOOK) {
                builder.setEnchantments(enchantments);
            }
        }

        ItemStack item = builder.build();
        if (item.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            for (Enchantment enchantment : enchantments.keySet()) {
                meta.addStoredEnchant(enchantment, enchantments.get(enchantment), true);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    public static List<ItemStack> parseItems(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (String key : section.getKeys(false)) {
            items.add(parseItem(section.getConfigurationSection(key)));
        }
        return items;
    }
}
