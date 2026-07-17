package com.mrjoshuasperry.pocketplugins.modules.cobblegenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFormEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class CobbleGenerator extends Module {
    private final Map<Material, Double> materials;

    public CobbleGenerator(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.materials = new HashMap<>();

        ConfigurationSection section = readableConfig.getConfigurationSection("blocks");
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
        if (this.materials.isEmpty()) {
            return Material.COBBLESTONE;
        }

        // keySet() and values() of the same map iterate in matching order, so the two
        // lists stay aligned index-for-index.
        List<Material> validMaterials = new ArrayList<>(this.materials.keySet());
        List<Double> weights = new ArrayList<>(this.materials.values());
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        double roll = this.getPlugin().getRandom().nextDouble() * total;

        return pick(validMaterials, weights, roll);
    }

    /**
     * Weighted pick: walks the weight buckets subtracting as it goes and returns the
     * material whose bucket {@code roll} lands in. {@code roll} is expected in
     * [0, sum(weights)); a value at or past the end falls through to the last entry.
     * Package-private and static so the selection is unit-testable without a live RNG.
     */
    static Material pick(List<Material> materials, List<Double> weights, double roll) {
        for (int index = 0; index < materials.size(); index++) {
            if (roll < weights.get(index)) {
                return materials.get(index);
            }
            roll -= weights.get(index);
        }

        return materials.get(materials.size() - 1);
    }
}