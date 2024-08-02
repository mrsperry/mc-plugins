package com.mrjoshuasperry.mcutils.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    protected ItemStack item;
    protected ItemMeta meta;

    public ItemBuilder() {
        this(Material.STONE);
    }

    public ItemBuilder(Material material) {
        if (material == Material.AIR) {
            Bukkit.getLogger().warning("Tried to build an item stack with air!");
            return;
        }
        this.item = new ItemStack(material, 1);
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder setMaterial(Material material) {
        this.item.setType(material);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    public ItemBuilder setData(short data) {
        this.item.setDurability(data);
        return this;
    }

    public ItemBuilder setName(String name) {
        this.meta.setDisplayName(ChatColor.RESET + name);
        return this;
    }

    public ItemBuilder setNameColor(ChatColor color) {
        this.meta.setDisplayName(color + this.meta.getDisplayName());
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        this.meta.setLore(lore);
        return this;
    }

    public ItemBuilder addLore(String loreLine) {
        List<String> temp = this.meta.getLore();
        if (temp == null) {
            temp = new ArrayList<>();
        }

        temp.add(loreLine);
        return this.setLore(temp);
    }

    public ItemBuilder setEnchantments(HashMap<Enchantment, Integer> enchantments) {
        for (Enchantment enchant : enchantments.keySet()) {
            this.meta.addEnchant(enchant, enchantments.get(enchant), true);
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        this.meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return this.item;
    }
}
