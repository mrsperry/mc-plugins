package com.mrjoshuasperry.pocketplugins.additions.cobblegenerator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class CobbleGeneratorManager {
    private static final CobbleGeneratorManager self = new CobbleGeneratorManager();
    private final Map<Material, Double> materials;

    private CobbleGeneratorManager() {
        this.materials = new EnumMap<>(Material.class);
    }

    void addMaterial(Material material, double weight) {
        this.materials.put(material, weight);
    }

    Material generateBlock() {
        if (this.materials.size() == 0) {
            return Material.COBBLESTONE;
        }

        List<Double> weights = new ArrayList<>(materials.values());
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();

        double randValue = PocketPlugins.getRandom().nextDouble() * total;
        List<Material> mats = new ArrayList<>(this.materials.keySet());

        for (int index = 0; index < mats.size(); index++) {
            if (randValue < weights.get(index)) {
                return mats.get(index);
            }
            randValue -= weights.get(index);
        }

        return mats.get(mats.size() - 1);
    }

    public static CobbleGeneratorManager getInstance() {
        return self;
    }

    public static void loadConfig(ConfigurationSection section) {
        CobbleGeneratorManager manager = getInstance();

        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                Material mat = Material.valueOf(key.toUpperCase());
                double weight = section.getDouble(key);
                manager.addMaterial(mat, weight);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Invalid block option! (" + e.getMessage() + ")");
            }
        }
    }
}
