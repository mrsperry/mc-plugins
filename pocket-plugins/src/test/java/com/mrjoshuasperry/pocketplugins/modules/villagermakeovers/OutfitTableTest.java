package com.mrjoshuasperry.pocketplugins.modules.villagermakeovers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

class OutfitTableTest {
  private static final Set<String> TYPES = Set.of("desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga");

  private static ConfigurationSection section() {
    return new MemoryConfiguration();
  }

  @Test
  void flattensTheTypeKeyedConfigIntoABiomeLookup() {
    ConfigurationSection config = section();
    config.set("outfits.desert", List.of("desert", "badlands"));
    config.set("outfits.swamp", List.of("mangrove_swamp"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertEquals("desert", table.getTypeName("minecraft:desert"));
    assertEquals("desert", table.getTypeName("minecraft:badlands"));
    assertEquals("swamp", table.getTypeName("minecraft:mangrove_swamp"));
    assertTrue(table.getWarnings().isEmpty());
  }

  @Test
  void bareBiomeNamesPickUpTheMinecraftNamespace() {
    assertEquals("minecraft:desert", OutfitTable.normalize("desert"));
    assertEquals("minecraft:desert", OutfitTable.normalize("  DESERT "));
    // An explicit namespace is kept, so a data pack biome can be styled too.
    assertEquals("mypack:oasis", OutfitTable.normalize("MyPack:Oasis"));
  }

  @Test
  void anUnmappedBiomeHasNoOutfit() {
    OutfitTable table = OutfitTable.parse(section(), TYPES);

    assertNull(table.getTypeName("minecraft:desert"));
  }

  @Test
  void aBiomeListedTwiceKeepsTheFirstOutfitAndWarns() {
    ConfigurationSection config = section();
    config.set("outfits.desert", List.of("badlands"));
    config.set("outfits.savanna", List.of("badlands"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertEquals("desert", table.getTypeName("minecraft:badlands"));
    assertEquals(List.of("minecraft:badlands is listed under both desert and savanna; keeping desert"),
        table.getWarnings());
  }

  @Test
  void anUnrecognizedOutfitKeyIsSkippedRatherThanFatal() {
    ConfigurationSection config = section();
    config.set("outfits.tropical", List.of("jungle"));
    config.set("outfits.snow", List.of("ice_spikes"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertNull(table.getTypeName("minecraft:jungle"));
    assertEquals("snow", table.getTypeName("minecraft:ice_spikes"));
    assertEquals(1, table.getWarnings().size());
    assertTrue(table.getWarnings().getFirst().startsWith("'tropical' is not a villager type"));
  }

  @Test
  void unstyledBiomesAreExcusedFromTheDriftCheck() {
    ConfigurationSection config = section();
    config.set("outfits.desert", List.of("desert"));
    config.set("unstyled", List.of("the_end"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertNull(table.getTypeName("minecraft:the_end"));
    assertEquals(List.of(), table.findUnmappedBiomes(List.of("minecraft:desert", "minecraft:the_end")));
  }

  @Test
  void aBiomeThatIsBothStyledAndUnstyledKeepsItsOutfitAndWarns() {
    ConfigurationSection config = section();
    config.set("outfits.desert", List.of("badlands"));
    config.set("unstyled", List.of("badlands"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertEquals("desert", table.getTypeName("minecraft:badlands"));
    assertEquals(List.of("minecraft:badlands is listed as unstyled but also mapped to desert; the outfit wins"),
        table.getWarnings());
  }

  @Test
  void findsBiomesTheConfigHasNotAccountedFor() {
    ConfigurationSection config = section();
    config.set("outfits.desert", List.of("desert"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertEquals(List.of("minecraft:pale_garden", "minecraft:swamp"),
        table.findUnmappedBiomes(List.of("minecraft:desert", "minecraft:swamp", "minecraft:pale_garden")));
  }

  @Test
  void findsConfigEntriesThatNameNoRealBiome() {
    ConfigurationSection config = section();
    config.set("outfits.desert", List.of("desert", "dune_sea"));
    config.set("unstyled", List.of("hoth"));

    OutfitTable table = OutfitTable.parse(config, TYPES);

    assertEquals(List.of("minecraft:dune_sea", "minecraft:hoth"),
        table.findUnknownBiomes(List.of("minecraft:desert")));
  }

  @Test
  void aMissingSectionParsesToAnEmptyTable() {
    OutfitTable table = OutfitTable.parse(null, TYPES);

    assertTrue(table.getTypeByBiome().isEmpty());
    assertTrue(table.getWarnings().isEmpty());
  }
}
