package com.mrjoshuasperry.mcutils.menu.items;

import java.util.function.BiConsumer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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

    /**
     * Handles a click, with the {@link ClickType} available for items that treat
     * left/right/shift clicks differently. The default ignores it and runs the
     * plain {@link #onClick(Player, Menu)}, so items that do not care are
     * unaffected.
     */
    public void onClick(Player player, Menu menu, ClickType click) {
        this.onClick(player, menu);
    }

    /**
     * Advances any time-driven state. Called once per menu tick while the owning
     * menu is open; see {@link com.mrjoshuasperry.mcutils.menu.MenuManager}.
     */
    public void tick() {
        // No-op by default.
    }
}
