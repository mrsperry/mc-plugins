package com.mrjoshuasperry.deathchest;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.deathchest.listeners.BlockListener;
import com.mrjoshuasperry.deathchest.listeners.EntityListener;
import com.mrjoshuasperry.deathchest.listeners.InventoryListener;
import com.mrjoshuasperry.deathchest.listeners.PlayerListener;

public class Main extends JavaPlugin {
    private final NamespacedKey deathChestKey = new NamespacedKey(this, "death-chest");
    private final NamespacedKey deathChestItemsKey = new NamespacedKey(this, "death-chest-items");
    private final NamespacedKey deathChestPlayerKey = new NamespacedKey(this, "death-chest-player");

    @Override
    public void onEnable() {
        Listener[] listeners = {
                new BlockListener(this),
                new EntityListener(this),
                new InventoryListener(this),
                new PlayerListener(this)
        };

        PluginManager manager = this.getServer().getPluginManager();
        for (Listener listener : listeners) {
            manager.registerEvents(listener, this);
        }
    }

    public NamespacedKey getDeathChestKey() {
        return this.deathChestKey;
    }

    public NamespacedKey getDeathChestItemsKey() {
        return this.deathChestItemsKey;
    }

    public NamespacedKey getDeathChestPlayerKey() {
        return this.deathChestPlayerKey;
    }
}
