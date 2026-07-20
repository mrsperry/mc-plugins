package com.mrjoshuasperry.mcutils.menu;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes inventory events to the {@link Menu} that owns them and keeps track of
 * which menus are currently open so {@link MenuManager} only ticks those.
 */
public class MenuListener implements Listener {
    private final Set<Menu> openMenus = new HashSet<>();

    /** The menus with at least one viewer, for the tick task. */
    public Set<Menu> getOpenMenus() {
        return Collections.unmodifiableSet(this.openMenus);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // getInventory is the top inventory, which is the menu even when the click
        // landed in the player's own inventory. That is deliberate: a shift-click
        // from below targets the menu and has to be guarded too.
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }

        if (menu.onClick(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu)) {
            return;
        }

        // A drag can span both inventories; cancel it outright rather than trying
        // to work out which of the raw slots belong to the menu.
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu && event.getPlayer() instanceof Player player) {
            this.openMenus.add(menu);
            menu.onOpen(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu menu && event.getPlayer() instanceof Player player) {
            menu.onClose(player);

            // Menus are per-player, but a menu can legitimately have more than one
            // viewer, so only stop ticking once the last one leaves.
            if (menu.getInventory().getViewers().size() <= 1) {
                this.openMenus.remove(menu);
            }
        }
    }
}
