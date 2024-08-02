package com.mrjoshuasperry.mcutils.menu;

import org.bukkit.entity.Player;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;

public class MenuEventContext {
    private Player player;
    private Menu menu;
    private MenuItem item;

    /**
     * Creates a new context for an inventory click event to be used by menu item
     * on-click consumers
     * 
     * @param player The player that clicked
     * @param menu   The menu the player clicked in
     * @param item   The menu item the player clicked
     */
    public MenuEventContext(Player player, Menu menu, MenuItem item) {
        this.player = player;
        this.menu = menu;
        this.item = item;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Menu getMenu() {
        return this.menu;
    }

    public MenuItem getItem() {
        return this.item;
    }
}
