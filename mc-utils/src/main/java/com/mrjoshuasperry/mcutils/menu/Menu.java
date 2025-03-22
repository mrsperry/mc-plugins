package com.mrjoshuasperry.mcutils.menu;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;

import net.kyori.adventure.text.Component;

public class Menu implements InventoryHolder {
    protected Inventory inventory;
    protected Map<Integer, MenuItem> items;

    public Menu(Component title, int slots) {
        this(title, slots, new HashMap<>());
    }

    public Menu(Component title, int slots, Map<Integer, MenuItem> items) {
        this.inventory = Bukkit.createInventory(this, slots, title);
        this.items = items;

        this.repopulateInventory();
    }

    public void repopulateInventory() {
        for (Integer slot : this.items.keySet()) {
            ItemStack item = this.items.get(slot).getItem();
            if (!this.inventory.getItem(slot).equals(item)) {
                this.inventory.setItem(slot, item);
            }
        }
    }

    public void fillInventory(MenuItem item) {
        for (int index = 0; index < this.inventory.getSize(); index++) {
            if (this.inventory.getItem(index) == null) {
                this.items.put(index, item);
                this.inventory.setItem(index, item.getItem());
            }
        }
    }

    public void setItem(int slot, MenuItem item) {
        this.items.put(slot, item);
        this.inventory.setItem(slot, item.getItem());
    }

    public void clearItem(int slot) {
        this.items.remove(slot);
        this.inventory.setItem(slot, null);
    }

    public void clearItems(int minSlot, int maxSlot) {
        for (int index = minSlot; index <= maxSlot; index++) {
            this.clearItem(index);
        }
    }

    public void clickedSlot(Player player, int slot) {
        if (this.items.containsKey(slot)) {
            this.items.get(slot).onClick(player, this);
        }

        this.repopulateInventory();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
