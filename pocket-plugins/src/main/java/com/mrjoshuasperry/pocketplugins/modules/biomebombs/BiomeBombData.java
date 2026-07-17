package com.mrjoshuasperry.pocketplugins.modules.biomebombs;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.kyori.adventure.text.format.TextColor;

public class BiomeBombData {
  private final String biomeType;
  private final Material catalyst;
  private final Color color;

  /**
   * @throws IllegalArgumentException if the section does not describe a complete
   *                                  biome bomb; the message names the problem and
   *                                  is meant to be logged against the biome's key
   */
  public BiomeBombData(ConfigurationSection section) {
    this.biomeType = section.getString("biome");
    if (this.biomeType == null) {
      throw new IllegalArgumentException("no biome name");
    }

    String catalystName = section.getString("catalyst");
    if (catalystName == null) {
      throw new IllegalArgumentException("no catalyst");
    }

    try {
      this.catalyst = Material.valueOf(catalystName);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("'" + catalystName + "' is not a material");
    }

    ConfigurationSection colorSection = section.getConfigurationSection("color");
    if (colorSection == null) {
      throw new IllegalArgumentException("no color");
    }

    try {
      this.color = Color.fromRGB(
          colorSection.getInt("red"),
          colorSection.getInt("green"),
          colorSection.getInt("blue"));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("color has a value outside 0-255");
    }
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