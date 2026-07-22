package com.mrjoshuasperry.deathchest;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.deathchest.listeners.DeathPileListener;
import com.mrjoshuasperry.deathchest.listeners.PlayerListener;

public class Main extends JavaPlugin {
    // Marks an Interaction as a clickable pile hitbox and points it at its item.
    private final NamespacedKey pileMemberKey = new NamespacedKey(this, "pile-member");
    private final NamespacedKey pileDisplayIdKey = new NamespacedKey(this, "pile-display-id");
    // Shared by every entity in one death; groups them for pickup and cleanup.
    private final NamespacedKey pileGroupKey = new NamespacedKey(this, "pile-group");
    // On the ItemDisplay: points back at its hitbox, and the orbit blob that lets a
    // pile be rebuilt from the entity after a restart.
    private final NamespacedKey pileInteractionIdKey = new NamespacedKey(this, "pile-interaction-id");
    private final NamespacedKey pileOrbitKey = new NamespacedKey(this, "pile-orbit");

    private DeathPileManager pileManager;

    @Override
    public void onEnable() {
        this.pileManager = new DeathPileManager(this);

        Listener[] listeners = {
                new PlayerListener(this),
                new DeathPileListener(this)
        };

        PluginManager manager = this.getServer().getPluginManager();
        for (Listener listener : listeners) {
            manager.registerEvents(listener, this);
        }

        this.pileManager.start();
    }

    public DeathPileManager getPileManager() {
        return this.pileManager;
    }

    public NamespacedKey getPileMemberKey() {
        return this.pileMemberKey;
    }

    public NamespacedKey getPileDisplayIdKey() {
        return this.pileDisplayIdKey;
    }

    public NamespacedKey getPileGroupKey() {
        return this.pileGroupKey;
    }

    public NamespacedKey getPileInteractionIdKey() {
        return this.pileInteractionIdKey;
    }

    public NamespacedKey getPileOrbitKey() {
        return this.pileOrbitKey;
    }
}
