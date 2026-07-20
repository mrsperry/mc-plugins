package com.mrjoshuasperry.pocketplugins.modules.villagermakeovers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

/**
 * Guards the shipped outfit table against the biome and villager-type registries.
 *
 * <p>
 * The mapping is maintained by hand, so a Minecraft update that adds a biome would
 * otherwise just mean villagers never change outfit there — no error, no symptom,
 * only a gap. The module warns about that at startup; this fails the build for it
 * instead, in the same spirit as mc-utils' {@code RegistryDriftTest}.
 *
 * <p>
 * A failure here is a prompt to classify the new biome — give it an outfit under
 * {@code outfits}, or list it under {@code unstyled} if it should be left alone.
 */
class OutfitDriftTest {
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private static ConfigurationSection shippedConfig() {
    try (InputStream stream = OutfitDriftTest.class.getResourceAsStream("/config.yml")) {
      assertNotNull(stream, "config.yml is missing from the jar resources");

      ConfigurationSection section = YamlConfiguration
          .loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .getConfigurationSection("villagermakeovers");

      assertNotNull(section, "config.yml has no villagermakeovers section");
      return section;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not read the shipped config.yml", exception);
    }
  }

  private static Set<String> typeNames() {
    return RegistryAccess.registryAccess().getRegistry(RegistryKey.VILLAGER_TYPE).stream()
        .map(type -> type.getKey().value())
        .collect(Collectors.toSet());
  }

  private static List<String> biomeKeys() {
    List<String> keys = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream()
        .map(biome -> biome.getKey().asString())
        .toList();

    // Without this the drift checks below would pass vacuously on an empty registry,
    // which is exactly the situation they exist to catch.
    assertTrue(keys.contains("minecraft:plains"),
        "The mock server exposed no biome registry, so nothing here was actually checked");
    return keys;
  }

  @Test
  void everyBiomeIsEitherStyledOrExplicitlyUnstyled() {
    OutfitTable table = OutfitTable.parse(shippedConfig(), typeNames());

    assertEquals(List.of(), table.findUnmappedBiomes(biomeKeys()),
        "The outfit table has drifted — a biome was added. Give it an outfit under `outfits`,"
            + " or list it under `unstyled` to leave villagers there alone");
  }

  @Test
  void everyConfiguredBiomeStillExists() {
    OutfitTable table = OutfitTable.parse(shippedConfig(), typeNames());

    assertEquals(List.of(), table.findUnknownBiomes(biomeKeys()),
        "The outfit table names a biome the registry does not have — it was renamed or removed");
  }

  @Test
  void theShippedTableParsesWithoutComplaint() {
    OutfitTable table = OutfitTable.parse(shippedConfig(), typeNames());

    assertEquals(List.of(), table.getWarnings());
  }

  @Test
  void everyVillagerTypeIsUsedByAtLeastOneBiome() {
    // Not strictly required, but an outfit no biome maps to is almost certainly a
    // typo in the type key rather than an intentional choice.
    OutfitTable table = OutfitTable.parse(shippedConfig(), typeNames());
    Set<String> used = Set.copyOf(table.getTypeByBiome().values());

    for (String type : typeNames()) {
      assertTrue(used.contains(type), "No biome maps to the " + type + " outfit");
    }
  }
}
