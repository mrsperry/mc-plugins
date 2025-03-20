package com.mrjoshuasperry.pocketplugins.modules.cobblegenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFormEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class CobbleGenerator extends Module {
    private final Map<Material, Double> materials;

    public CobbleGenerator() {
        super("CobbleGenerator");

        this.materials = new HashMap<>();
    }

    @Override
    public void initialize(YamlConfiguration config) {
        super.initialize(config);

        ConfigurationSection section = config.getConfigurationSection("blocks");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                double weight = section.getDouble(key, 0);
                this.materials.put(material, weight);
            } catch (Exception ex) {
                this.getPlugin().getLogger().warning("Invalid block material or weight! (" + ex.getMessage() + ")");
            }
        }
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        BlockState newState = event.getNewState();

        if (newState.getType() != Material.COBBLESTONE) {
            return;
        }

        newState.setType(this.generateNewBlock());
        newState.getWorld().playSound(event.getNewState().getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1f, 1.5f);
    }

    protected Material generateNewBlock() {
        if (this.materials.size() == 0) {
            return Material.COBBLESTONE;
        }

        List<Double> weights = new ArrayList<>(this.materials.values());
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();

        double chance = this.getPlugin().getRandom().nextDouble() * total;
        List<Material> validMaterials = new ArrayList<>(this.materials.keySet());

        for (int index = 0; index < validMaterials.size(); index++) {
            if (chance < weights.get(index)) {
                return validMaterials.get(index);
            }
            chance -= weights.get(index);
        }

        return validMaterials.get(validMaterials.size() - 1);
    }
}