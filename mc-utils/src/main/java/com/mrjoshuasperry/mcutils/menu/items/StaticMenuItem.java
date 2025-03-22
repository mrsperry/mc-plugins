package com.mrjoshuasperry.mcutils.menu.items;

import java.util.function.BiConsumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.Menu;

public class StaticMenuItem extends MenuItem {
    public StaticMenuItem(Material material, BiConsumer<Player, Menu> onClick) {
        this(new ItemStack(material), onClick);
    }

    public StaticMenuItem(ItemStack item, BiConsumer<Player, Menu> onClick) {
        super(item, onClick);
    }
}
