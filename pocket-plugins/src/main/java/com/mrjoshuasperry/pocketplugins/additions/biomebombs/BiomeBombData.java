package com.mrjoshuasperry.pocketplugins.additions.biomebombs;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.format.TextColor;

public class BiomeBombData {
  private final String biomeType;
  private final Material catalyst;
  private final Color color;

  public BiomeBombData(ConfigurationSection section) {
    this.biomeType = section.getString("biome");
    this.catalyst = Material.valueOf(section.getString("catalyst"));

    ConfigurationSection colorSection = section.getConfigurationSection("color");
    this.color = Color.fromRGB(
        colorSection.getInt("red"),
        colorSection.getInt("green"),
        colorSection.getInt("blue"));
  }

  public String getBiomeType() {
    return biomeType.toUpperCase();
  }

  public String getBiomeName() {
    return biomeType.replace('_', ' ');
  }

  public Material getCatalyst() {
    return catalyst;
  }

  public Color getColor() {
    return color;
  }

  public TextColor getTextColor() {
    return TextColor.color(getColor().getRed(), getColor().getGreen(), getColor().getBlue());
  }
}