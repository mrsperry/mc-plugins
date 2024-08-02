package com.mrjoshuasperry.mcutils.menu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;

public class MenuManager implements Listener {
    private JavaPlugin plugin;
    private HashSet<Menu> openMenus;
    private HashMap<Player, MenuHistory> histories;

    /**
     * Creates a new menu manager
     * 
     * @param plugin The owning plugin
     */
    public MenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.openMenus = new HashSet<>();
        this.histories = new HashMap<>();

        // Register events
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Creates a new menu
     * 
     * @param player The player this menu should open for
     * @param title  The title of the menu
     * @param slots  The number of slots for the menu (truncated to a factor of 9)
     * @param items  A list of menu items to add to the menu
     * @return The menu that was created
     */
    public Menu createNewMenu(Player player, String title, int slots, HashSet<MenuItem> items) {
        return this.createNewMenu(player, title, slots, items, null);
    }

    /**
     * Creates a new menu
     * 
     * @param player  The player this menu should open for
     * @param title   The title of the menu
     * @param slots   The number of slots for the menu (truncated to a factor of 9)
     * @param items   A list of menu items to add to the menu
     * @param onClose A function that is called when the menu is closed
     * @return The menu that was created
     */
    public Menu createNewMenu(Player player, String title, int slots, HashSet<MenuItem> items, Consumer<Menu> onClose) {
        Menu menu = new Menu(player, title, slots, items, onClose);
        this.openMenus.add(menu);

        // Add this menu to the player's history
        if (this.histories.containsKey(player)) {
            MenuHistory history = this.histories.get(player);
            history.addSafeMenu(menu, true);

            this.histories.put(player, history);
        } else {
            this.histories.put(player, new MenuHistory(this, menu));
        }

        return menu;
    }

    /**
     * Opens an existing menu
     * 
     * @param menu The menu to open
     */
    public void createNewMenu(Menu menu) {
        menu.getPlayer().openInventory(menu.getInventory());
        this.openMenus.add(menu);
    }

    /**
     * Gets the player's menu history
     * 
     * @param player The player
     * @return The menu history or null if none can be found
     */
    public MenuHistory getPlayerMenuHistory(Player player) {
        return this.histories.getOrDefault(player, null);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        for (Menu menu : this.openMenus) {
            if (menu.getInventory() == event.getInventory()) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);

                for (MenuItem item : menu.getItems()) {
                    if (item.getSlot() == event.getSlot()) {
                        // Create a runnable to handle the click event 1 tick later as there are issues
                        // using inventory methods with this event
                        new BukkitRunnable() {
                            public void run() {
                                item.onClick(new MenuEventContext((Player) event.getWhoClicked(), menu, item));
                            }
                        }.runTaskLater(this.plugin, 1);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        for (Menu menu : this.openMenus) {
            if (menu.getInventory() == event.getInventory()) {
                menu.onClose();

                this.openMenus.remove(menu);
                return;
            }
        }
    }
}
