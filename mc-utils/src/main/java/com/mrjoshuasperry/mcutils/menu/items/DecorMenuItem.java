package com.mrjoshuasperry.mcutils.menu.items;

import org.bukkit.inventory.ItemStack;

public class DecorMenuItem extends MenuItem {
    /**
     * Creates a new decorative menu item that has no on-click event; its only use
     * is decoration
     * 
     * @param slot The slot for this item
     * @param item The item stack to put in the slot
     */
    public DecorMenuItem(int slot, ItemStack item) {
        super(slot, item, null);
    }
}
