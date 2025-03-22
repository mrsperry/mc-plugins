package com.mrjoshuasperry.mcutils.menu.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

public class DecorMenuItem extends MenuItem {
    public DecorMenuItem(Material material) {
        this(new ItemStack(material));
    }

    public DecorMenuItem(ItemStack item) {
        super(item, null);

        ItemMeta meta = item.getItemMeta();
        meta.itemName(Component.text(""));
        item.setItemMeta(meta);
    }
}
