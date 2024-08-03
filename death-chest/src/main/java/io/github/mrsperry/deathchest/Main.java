package io.github.mrsperry.deathchest;

import io.github.mrsperry.deathchest.listeners.BlockListener;
import io.github.mrsperry.deathchest.listeners.PlayerListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class Main extends JavaPlugin {
    private static HashSet<DeathChest> chests = new HashSet<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        chests = DeathChest.deserialize(this.getConfig());

        this.getServer().getPluginManager().registerEvents(new BlockListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    @Override
    public void onDisable() {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (DeathChest chest : chests) {
            serialized.add(chest.serialize());
        }
        this.getConfig().set("chests", serialized);
        this.saveConfig();
    }

    public static HashSet<DeathChest> getChests() {
        return chests;
    }

    public static void addChest(DeathChest chest) {
        chests.add(chest);
    }

    public static void removeChest(DeathChest chest) {
        chests.remove(chest);
    }
}
