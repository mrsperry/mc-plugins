package com.mrjoshuasperry.mcutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class BlockUtils {
  private static BlockUtils self;
  private List<Material> blacklistedBlocks;
  private Set<Material> solidBlocks;
  private Map<Color, BlockData> blockColors;

  private BlockUtils() {
    this.blacklistedBlocks = List.of(
        Material.BARRIER,
        Material.SPAWNER,
        Material.VAULT,
        Material.TRIAL_SPAWNER,
        Material.CHAIN_COMMAND_BLOCK,
        Material.COMMAND_BLOCK);

    this.solidBlocks = Arrays.stream(Material.values())
        .filter(Material::isBlock)
        .filter(Material::isSolid)
        .filter(Material::isOccluding)
        .filter(material -> !material.isAir())
        .filter(material -> !this.blacklistedBlocks.contains(material))
        .collect(Collectors.toSet());

    this.blockColors = new HashMap<>();
    buildBlockColors();
  }

  public static BlockUtils getInstance() {
    if (self == null) {
      self = new BlockUtils();
    }

    return self;
  }

  public Set<Material> getSolidBlocks() {
    return this.solidBlocks;
  }

  public BlockData getClosestBlockColor(Color targetColor) {
    BlockData result = null;
    double smallestDistance = Double.MAX_VALUE;

    for (Color blockColor : this.blockColors.keySet()) {
      int dR = blockColor.getRed() - targetColor.getRed();
      int dG = blockColor.getGreen() - targetColor.getGreen();
      int dB = blockColor.getBlue() - targetColor.getBlue();

      double redMean = (blockColor.getRed() + targetColor.getRed()) / 2.0;

      // Compensations for how we see color.
      double dRSq = (2 + redMean / 256) * dR * dR;
      double dGSq = 4 * dG * dG;
      double dBSq = (2 + (255 - redMean)) * dB * dB;

      double distance = Math.sqrt(dRSq + dGSq + dBSq);

      if (distance <= smallestDistance) {
        smallestDistance = distance;
        result = this.blockColors.get(blockColor);
      }
    }

    return result;
  }

  private void buildBlockColors() {
    try {
      // Get the resource from your plugin
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("block_colors.csv");
      if (inputStream == null) {
        System.err.println("Could not find block_colors.csv");
        return;
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

      // Skip header
      reader.readLine();

      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length == 2) {
          String blockName = parts[0].trim();
          String hexColor = parts[1].trim();

          try {
            Material material = Material.valueOf(blockName.toUpperCase());
            // Only add if it's in our solid blocks set
            if (this.solidBlocks.contains(material)) {
              Color color = hexToColor(hexColor);
              this.blockColors.put(color, material.createBlockData());
            }
          } catch (IllegalArgumentException e) {
            // Skip invalid materials
            continue;
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error reading block colors: " + e.getMessage());
    }
  }

  private Color hexToColor(String hex) {
    hex = hex.replace("#", "");
    return Color.fromRGB(
        Integer.parseInt(hex.substring(0, 2), 16),
        Integer.parseInt(hex.substring(2, 4), 16),
        Integer.parseInt(hex.substring(4, 6), 16));
  }

  // Add getter for blockColors
  public Map<Color, BlockData> getBlockColors() {
    return this.blockColors;
  }

  // Convert RGB to HSB (returns [hue, saturation, brightness])
  public double[] rgbToHsb(Color color) {
    double r = color.getRed() / 255.0;
    double g = color.getGreen() / 255.0;
    double b = color.getBlue() / 255.0;

    double max = Math.max(Math.max(r, g), b);
    double min = Math.min(Math.min(r, g), b);
    double delta = max - min;

    // Calculate hue (0-1)
    double hue = 0;
    if (delta != 0) {
      if (max == r) {
        hue = ((g - b) / delta) % 6;
      } else if (max == g) {
        hue = ((b - r) / delta) + 2;
      } else {
        hue = ((r - g) / delta) + 4;
      }
      hue /= 6;
      if (hue < 0)
        hue += 1;
    }

    // Calculate saturation (0-1)
    double saturation = (max == 0) ? 0 : (delta / max);

    // Brightness is just the maximum component (0-1)
    double brightness = max;

    return new double[] { hue, saturation, brightness };
  }
}
