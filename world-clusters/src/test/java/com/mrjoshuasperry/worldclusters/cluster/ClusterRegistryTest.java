package com.mrjoshuasperry.worldclusters.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import com.mrjoshuasperry.worldclusters.world.WorldRole;

/**
 * Config parsing and the world-to-cluster lookup, which decides where every
 * boundary falls. Pure logic — no server needed.
 */
class ClusterRegistryTest {
    private static final String CONFIG = """
            survival:
              display-name: "Survival"
              default-gamemode: SURVIVAL
              permissions: []
              worlds:
                world:
                  role: overworld
                  environment: NORMAL
                world_nether:
                  role: nether
                  environment: NETHER
                world_the_end:
                  role: end
                  environment: THE_END
            creative:
              display-name: "Creative"
              default-gamemode: CREATIVE
              permissions:
                - worldedit.*
              worlds:
                build:
                  role: overworld
                  environment: NORMAL
                redstone:
                  role: none
                  environment: NORMAL
            """;

    private ClusterRegistry load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);

        ClusterRegistry registry = new ClusterRegistry(Logger.getLogger("test"));
        registry.load(config);
        return registry;
    }

    @Test
    void mapsWorldsToTheirCluster() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        assertEquals("survival", registry.getClusterId("world"));
        assertEquals("survival", registry.getClusterId("world_nether"));
        assertEquals("creative", registry.getClusterId("build"));
        assertEquals("creative", registry.getClusterId("redstone"));
    }

    @Test
    void unregisteredWorldsFallIntoTheDefaultCluster() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        assertEquals(ClusterRegistry.DEFAULT_ID, registry.getClusterId("some_world_nobody_configured"));
        assertNotNull(registry.getClusterById(ClusterRegistry.DEFAULT_ID),
                "the default cluster should always exist");
    }

    @Test
    void worldLookupIsCaseInsensitive() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        assertEquals("survival", registry.getClusterId("WORLD"));
        assertEquals("creative", registry.getClusterId("BuIlD"));
    }

    @Test
    void resolvesRolesWithinACluster() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        assertEquals("world_nether",
                registry.getWorldWithRole("survival", WorldRole.NETHER).getName());
        assertEquals("world_the_end",
                registry.getWorldWithRole("survival", WorldRole.END).getName());
        assertEquals("build",
                registry.getWorldWithRole("creative", WorldRole.OVERWORLD).getName());
    }

    @Test
    void aClusterWithNoNetherResolvesNoNether() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        // The creative cluster has no nether, so its portals must be inert rather
        // than routing into the survival nether.
        assertNull(registry.getWorldWithRole("creative", WorldRole.NETHER));
    }

    @Test
    void aWorldListedTwiceKeepsTheFirstCluster() throws InvalidConfigurationException {
        ClusterRegistry registry = load("""
                first:
                  worlds:
                    shared:
                      role: overworld
                second:
                  worlds:
                    shared:
                      role: overworld
                """);

        assertEquals("first", registry.getClusterId("shared"),
                "a duplicate world should keep its first cluster, not silently move");
        assertFalse(registry.getClusterById("second").contains("shared"),
                "the losing cluster should not still claim the world");
    }

    @Test
    void roleDefaultsToTheEnvironmentWhenUnset() throws InvalidConfigurationException {
        ClusterRegistry registry = load("""
                cluster:
                  worlds:
                    hell:
                      environment: NETHER
                """);

        assertEquals(WorldRole.NETHER, registry.getManagedWorld("hell").getRole());
    }

    @Test
    void assigningAWorldMovesItOutOfItsOldCluster() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        registry.assign(registry.getManagedWorld("redstone"), "survival");

        assertEquals("survival", registry.getClusterId("redstone"));
        assertFalse(registry.getClusterById("creative").contains("redstone"));
        assertTrue(registry.getClusterById("survival").contains("redstone"));
    }

    @Test
    void configRoundTripsThroughSave() throws InvalidConfigurationException {
        ClusterRegistry original = load(CONFIG);

        YamlConfiguration out = new YamlConfiguration();
        original.save(out.createSection("clusters"));

        ClusterRegistry reloaded = new ClusterRegistry(Logger.getLogger("test"));
        reloaded.load(out.getConfigurationSection("clusters"));

        assertEquals("survival", reloaded.getClusterId("world"));
        assertEquals("creative", reloaded.getClusterId("redstone"));
        assertEquals(WorldRole.NETHER, reloaded.getManagedWorld("world_nether").getRole());
        assertTrue(reloaded.getClusterById("creative").getPermissions().contains("worldedit.*"));
    }

    @Test
    void savingKeepsTheConfigComments() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                clusters:
                  # which side of the boundary this is
                  survival:
                    display-name: "Survival"
                    worlds:
                      # the main world
                      world:
                        role: overworld
                """);

        ClusterRegistry registry = new ClusterRegistry(Logger.getLogger("test"));
        registry.load(config.getConfigurationSection("clusters"));
        registry.save(config.getConfigurationSection("clusters"));

        String saved = config.saveToString();

        // Rewriting the section clears its keys, which drops their comments unless
        // they are explicitly carried over. Without that, the first /wc create
        // strips the shipped config's documentation.
        assertTrue(saved.contains("which side of the boundary this is"),
                "cluster comments should survive a save");
        assertTrue(saved.contains("the main world"),
                "per-world comments should survive a save");
    }

    @Test
    void savingDoesNotPersistTheImplicitDefaultCluster() throws InvalidConfigurationException {
        ClusterRegistry registry = load(CONFIG);

        YamlConfiguration out = new YamlConfiguration();
        registry.save(out.createSection("clusters"));

        assertFalse(out.getConfigurationSection("clusters").contains(ClusterRegistry.DEFAULT_ID),
                "persisting the default cluster would turn unregistered worlds into registered ones");
    }
}
