package com.mrjoshuasperry.mcutils.menu.items;

import java.util.function.BiConsumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.Menu;

public abstract class MenuItem {
    private ItemStack item;
    private BiConsumer<Player, Menu> onClick;

    public MenuItem(Material material) {
        this(new ItemStack(material));
    }

    public MenuItem(Material material, BiConsumer<Player, Menu> onClick) {
        this(new ItemStack(material), onClick);
    }

    public MenuItem(ItemStack item) {
        this(item, null);
    }

    public MenuItem(ItemStack item, BiConsumer<Player, Menu> onClick) {
        this.item = item;
        this.onClick = onClick;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public void onClick(Player player, Menu menu) {
        if (this.onClick != null) {
            this.onClick.accept(player, menu);
        }
    }
}
