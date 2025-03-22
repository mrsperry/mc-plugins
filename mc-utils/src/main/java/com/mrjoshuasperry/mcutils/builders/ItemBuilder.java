package com.mrjoshuasperry.mcutils.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

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
        this.item = this.item.withType(material);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    public ItemBuilder setName(Component name) {
        this.meta.itemName(name);
        return this;
    }

    public ItemBuilder setLore(List<Component> lore) {
        this.meta.lore(lore);
        return this;
    }

    public ItemBuilder addLore(Component loreLine) {
        List<Component> lore = this.meta.lore() == null ? new ArrayList<>() : this.meta.lore();
        lore.add(loreLine);
        this.meta.lore(lore);
        return this;
    }

    public ItemBuilder setEnchantments(Map<Enchantment, Integer> enchantments) {
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
