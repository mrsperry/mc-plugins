package com.mrjoshuasperry.mcutils.menu;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Wires the menu system into a plugin: registers {@link MenuListener} and runs
 * the task that drives {@link Menu#tick()} for open menus.
 *
 * <p>
 * Construct one per plugin in {@code onEnable}. The shade relocation in the root
 * pom gives every consuming plugin its own copy of these classes, so two plugins
 * each having a manager is fine.
 */
public class MenuManager {
    /** How often open menus tick. One second is enough for rotating display items. */
    private static final long DEFAULT_TICK_INTERVAL = 20L;

    private final JavaPlugin plugin;
    private final MenuListener listener;
    private final BukkitTask task;

    public MenuManager(JavaPlugin plugin) {
        this(plugin, DEFAULT_TICK_INTERVAL);
    }

    public MenuManager(JavaPlugin plugin, long tickInterval) {
        this.plugin = plugin;
        this.listener = new MenuListener();

        Bukkit.getPluginManager().registerEvents(this.listener, plugin);

        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, tickInterval, tickInterval);
    }

    private void tick() {
        // Copy first: a tick can close a menu, which mutates the open set.
        for (Menu menu : List.copyOf(this.listener.getOpenMenus())) {
            menu.tick();
        }
    }

    /** Stops the tick task. Call from {@code onDisable}. */
    public void shutdown() {
        this.task.cancel();
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public MenuListener getListener() {
        return this.listener;
    }
}
