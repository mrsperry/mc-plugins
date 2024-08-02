package com.mrjoshuasperry.mcutils.menu;

import java.util.HashSet;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;

public class Menu {
    // The player that opened this menu
    private Player player;
    // The title of the menu
    private String title;
    // The number of slots in the menu (truncated to a factor of 9)
    private int slots;
    // The items to be added to the inventory
    private HashSet<MenuItem> items;
    // The function that runs when the menu is closed
    private Consumer<Menu> onClose;
    // The actual inventory of the menu
    private Inventory inventory;

    Menu(Player player, String title, int slots, HashSet<MenuItem> items, Consumer<Menu> onClose) {
        this.player = player;
        this.title = title;
        this.items = items;
        this.onClose = onClose;

        // Set the slots to the minimum of 9 if they are below it
        if (slots < 9) {
            slots = 9;
        }

        // Round the number of slots up to a factor of 9
        int remainder = slots % 9;
        if (remainder != 0) {
            slots += (9 - remainder);
        }
        this.slots = slots;

        // Create the inventory
        this.inventory = Bukkit.createInventory(null, this.slots, this.title);
        // Set all menu items to their positions in the inventory
        for (MenuItem item : this.items) {
            item.setMenu(this);
            this.inventory.setItem(item.getSlot(), item.getItem());
        }

        // Open the inventory
        player.openInventory(this.inventory);
    }

    /**
     * Closes the inventory for the player that opened it
     */
    public void close() {
        if (this.inventory.getViewers().contains(this.player)) {
            this.onClose();
            this.player.closeInventory();
        }
    }

    /**
     * Runs the function that will run when the menu is closed
     */
    public void onClose() {
        if (this.onClose != null) {
            this.onClose.accept(this);
        }
    }

    /**
     * Gets a menu item based on the slot
     * 
     * @param slot The slot to look at
     * @return The menu item at the slot or null if one wasn't found
     */
    public MenuItem getMenuItem(int slot) {
        for (MenuItem item : this.items) {
            if (item.getSlot() == slot) {
                return item;
            }
        }

        return null;
    }

    /**
     * Removes a menu item from this menu
     * 
     * @param slot The slot of the menu item
     */
    public void removeMenuItem(int slot) {
        MenuItem item = this.getMenuItem(slot);
        if (item != null) {
            this.items.remove(item);

            this.inventory.setItem(slot, new ItemStack(Material.AIR));
        }
    }

    /**
     * Updates a slot with a new menu item
     * 
     * @param item The replacement menu item
     */
    public void updateSlot(MenuItem item) {
        this.inventory.setItem(item.getSlot(), item.getItem());
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getTitle() {
        return this.title;
    }

    public int getSlots() {
        return this.slots;
    }

    public HashSet<MenuItem> getItems() {
        return this.items;
    }

    public Consumer<Menu> getOnClose() {
        return this.onClose;
    }

    public Inventory getInventory() {
        return this.inventory;
    }
}
