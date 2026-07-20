package com.mrjoshuasperry.worldclusters.world;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * How a world is drawn in the {@code /worlds} menu. Fully config-driven so the
 * icon for a world can be changed without touching code.
 *
 * @param material the icon
 * @param name     the display name, or null to fall back to the world's name
 * @param lore     extra lines shown under the name
 */
public record DisplayItem(Material material, String name, List<String> lore) {
    private static final Material DEFAULT_MATERIAL = Material.GRASS_BLOCK;

    public static DisplayItem fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new DisplayItem(DEFAULT_MATERIAL, null, List.of());
        }

        Material material = Material.matchMaterial(section.getString("material", ""));

        return new DisplayItem(
                material == null ? DEFAULT_MATERIAL : material,
                section.getString("name"),
                List.copyOf(section.getStringList("lore")));
    }

    public void writeTo(ConfigurationSection section) {
        section.set("material", this.material.name());
        section.set("name", this.name);
        section.set("lore", new ArrayList<>(this.lore));
    }

    /**
     * Builds the icon.
     *
     * @param fallbackName used when the config gave no name
     * @param extraLore    appended after the configured lore, for context the
     *                     config can't know — such as the boundary warning
     */
    public ItemStack build(String fallbackName, List<Component> extraLore) {
        List<Component> lines = new ArrayList<>();
        for (String line : this.lore) {
            lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lines.addAll(extraLore);

        return new ItemBuilder(this.material)
                .setName(Component.text(this.name == null ? fallbackName : this.name, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false))
                .setLore(lines)
                .build();
    }
}
