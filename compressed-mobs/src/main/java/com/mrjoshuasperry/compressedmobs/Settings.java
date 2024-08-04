package com.mrjoshuasperry.compressedmobs;

import org.bukkit.configuration.ConfigurationSection;

public record Settings(double compressChance, int minYield, int maxYield) {
  public static Settings fromConfig(ConfigurationSection config, double defaultCompressChance, int defaultMinYield,
      int defaultMaxYield) {
    return new Settings(
        config.getDouble("compress-chance", defaultCompressChance),
        config.getInt("min-yield", defaultMinYield),
        config.getInt("max-yield", defaultMaxYield));
  }

}
