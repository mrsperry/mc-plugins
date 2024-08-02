package com.mrjoshuasperry.mcutils.menu.items;

import java.util.function.Consumer;

import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.MenuEventContext;

public class StaticMenuItem extends MenuItem {
    /**
     * Creates a new static menu item that has no outstanding properties, only an
     * on-click callback
     * 
     * @param slot     The slot for this item
     * @param item     The item stack to put in the slot
     * @param callback The consumer that is called when this item is clicked
     */
    public StaticMenuItem(int slot, ItemStack item, Consumer<MenuEventContext> callback) {
        super(slot, item, callback);
    }
}
