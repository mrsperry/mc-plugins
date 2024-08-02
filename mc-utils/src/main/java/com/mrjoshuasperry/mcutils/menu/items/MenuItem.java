package com.mrjoshuasperry.mcutils.menu.items;

import java.util.function.Consumer;

import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.Menu;
import com.mrjoshuasperry.mcutils.menu.MenuEventContext;

public abstract class MenuItem {
    // The owning menu
    private Menu menu;
    // The slot this item is in
    private int slot;
    // The item stack this item uses
    private ItemStack item;
    // The on click callback
    private Consumer<MenuEventContext> callback;

    public MenuItem(int slot, ItemStack item, Consumer<MenuEventContext> callback) {
        this.slot = slot;
        this.item = item;
        this.callback = callback;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public int getSlot() {
        return this.slot;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack item) {
        this.item = item;

        this.menu.updateSlot(this);
    }

    public void onClick(MenuEventContext context) {
        if (this.callback != null) {
            this.callback.accept(context);
        }
    }
}
