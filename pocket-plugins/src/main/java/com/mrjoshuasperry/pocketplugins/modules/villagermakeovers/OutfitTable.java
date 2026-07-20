package com.mrjoshuasperry.pocketplugins.modules.villagermakeovers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.configuration.ConfigurationSection;

/**
 * The configured biome to villager-outfit mapping, parsed as plain strings.
 *
 * <p>
 * The config inverts the natural lookup: it is keyed by the seven villager types
 * with a list of biomes under each, which keeps the whole table readable in seven
 * blocks rather than sixty lines. This class flattens that into the biome-to-type
 * direction the sweep actually queries.
 *
 * <p>
 * Everything here is string work against biome and type <em>names</em> rather than
 * registry objects, so the parsing, the duplicate detection and the drift audit can
 * all be tested without standing up a server.
 */
public final class OutfitTable {
  public static final String DEFAULT_NAMESPACE = "minecraft";

  private final Map<String, String> typeByBiome;
  private final Set<String> unstyled;
  private final List<String> warnings;

  private OutfitTable(Map<String, String> typeByBiome, Set<String> unstyled, List<String> warnings) {
    this.typeByBiome = typeByBiome;
    this.unstyled = unstyled;
    this.warnings = warnings;
  }

  /**
   * Reads the {@code outfits} and {@code unstyled} keys of a module config section.
   *
   * @param section    the module's readable config, or null to build an empty table
   * @param knownTypes the villager type keys to accept, lower-cased; anything else
   *                   is dropped with a warning rather than failing the module
   */
  public static OutfitTable parse(ConfigurationSection section, Set<String> knownTypes) {
    Map<String, String> typeByBiome = new LinkedHashMap<>();
    Set<String> unstyled = new LinkedHashSet<>();
    List<String> warnings = new ArrayList<>();

    ConfigurationSection outfits = section == null ? null : section.getConfigurationSection("outfits");

    if (outfits != null) {
      for (String key : outfits.getKeys(false)) {
        readOutfit(outfits, key, knownTypes, typeByBiome, warnings);
      }
    }

    for (String rawBiome : section == null ? List.<String>of() : section.getStringList("unstyled")) {
      String biome = normalize(rawBiome);
      unstyled.add(biome);

      if (typeByBiome.containsKey(biome)) {
        warnings.add(biome + " is listed as unstyled but also mapped to " + typeByBiome.get(biome)
            + "; the outfit wins");
      }
    }

    return new OutfitTable(typeByBiome, unstyled, warnings);
  }

  private static void readOutfit(ConfigurationSection outfits, String key, Set<String> knownTypes,
      Map<String, String> typeByBiome, List<String> warnings) {
    String type = key.trim().toLowerCase(Locale.ROOT);

    if (!knownTypes.contains(type)) {
      warnings.add("'" + key + "' is not a villager type; expected one of "
          + String.join(", ", new TreeSet<>(knownTypes)));
      return;
    }

    for (String rawBiome : outfits.getStringList(key)) {
      String biome = normalize(rawBiome);
      String claimed = typeByBiome.putIfAbsent(biome, type);

      if (claimed != null) {
        warnings.add(biome + " is listed under both " + claimed + " and " + type + "; keeping " + claimed);
      }
    }
  }

  /** Biome keys are compared namespaced, so bare config entries pick up {@code minecraft:}. */
  public static String normalize(String biome) {
    String trimmed = biome.trim().toLowerCase(Locale.ROOT);
    return trimmed.indexOf(':') < 0 ? DEFAULT_NAMESPACE + ":" + trimmed : trimmed;
  }

  /** The villager type key configured for a namespaced biome key, or null for none. */
  public String getTypeName(String biomeKey) {
    return this.typeByBiome.get(biomeKey);
  }

  public Map<String, String> getTypeByBiome() {
    return Map.copyOf(this.typeByBiome);
  }

  public List<String> getWarnings() {
    return List.copyOf(this.warnings);
  }

  /**
   * Registered biomes the config neither styles nor lists as deliberately unstyled.
   *
   * <p>
   * This is the drift check: a Minecraft update that adds a biome would otherwise
   * just quietly never trigger a makeover there.
   */
  public List<String> findUnmappedBiomes(Collection<String> knownBiomeKeys) {
    return knownBiomeKeys.stream()
        .map(OutfitTable::normalize)
        .filter(biome -> !this.typeByBiome.containsKey(biome) && !this.unstyled.contains(biome))
        .distinct()
        .sorted()
        .toList();
  }

  /** Config entries that no longer name a registered biome, e.g. one renamed upstream. */
  public List<String> findUnknownBiomes(Collection<String> knownBiomeKeys) {
    Set<String> known = knownBiomeKeys.stream().map(OutfitTable::normalize).collect(Collectors.toSet());

    return Stream.concat(this.typeByBiome.keySet().stream(), this.unstyled.stream())
        .filter(biome -> !known.contains(biome))
        .distinct()
        .sorted()
        .toList();
  }
}
