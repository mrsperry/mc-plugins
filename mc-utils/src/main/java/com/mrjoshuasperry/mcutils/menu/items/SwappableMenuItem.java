package com.mrjoshuasperry.mcutils.menu.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.menu.MenuEventContext;

public class SwappableMenuItem extends MenuItem {
    // The current index of the item
    private int index;
    // The list of item stacks that can be swapped
    private ArrayList<ItemStack> items;

    /**
     * Creates a new swappable menu item that allows an unlimited number of swaps
     * when clicked (useful for multi-state options)
     * 
     * @param slot     The slot for this item
     * @param item     The item stack to put in the slot
     * @param callback The consumer that is called when this item is clicked
     * @param items    Any number of item stacks that can be swapped to
     */
    public SwappableMenuItem(int slot, ItemStack item, Consumer<MenuEventContext> callback, ItemStack... items) {
        super(slot, item, callback);

        this.index = 0;
        this.items = Lists.newArrayList(item);
        this.items.addAll(Arrays.asList(items));
    }

    @Override
    public void onClick(MenuEventContext context) {
        this.swap();
        super.onClick(context);
    }

    /**
     * Swaps the current item out with the next in line
     */
    public void swap() {
        int index = this.index + 1;
        if (index >= this.items.size()) {
            index = 0;
        }

        super.setItem(this.items.get(index));
        this.index = index;
    }

    public void setIndex(int index) {
        this.index = index;
        super.setItem(this.items.get(index));
    }

    public int getIndex() {
        return this.index;
    }

    public ItemStack getCurrentItem() {
        return this.items.get(this.index);
    }
}
