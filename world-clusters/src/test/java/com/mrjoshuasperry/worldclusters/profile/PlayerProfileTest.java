package com.mrjoshuasperry.worldclusters.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * The profile is what crosses the boundary, so anything it silently drops is an
 * item or a level a player permanently loses. These tests capture a populated
 * player, round-trip through YAML, and check the state survives.
 */
class PlayerProfileTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Round-trips a profile through the same YAML path ProfileStore uses. */
    private PlayerProfile roundTrip(PlayerProfile profile) throws InvalidConfigurationException {
        YamlConfiguration out = new YamlConfiguration();
        profile.writeTo(out.createSection("profile"));

        YamlConfiguration in = new YamlConfiguration();
        in.loadFromString(out.saveToString());

        return PlayerProfile.readFrom(in.getConfigurationSection("profile"));
    }

    @Test
    void capturesAndRestoresInventoryContents() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 17));
        player.getInventory().setItem(5, new ItemStack(Material.OAK_LOG, 3));

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        assertEquals(new ItemStack(Material.DIAMOND, 17), restored.getInventory().get(0));
        assertEquals(new ItemStack(Material.OAK_LOG, 3), restored.getInventory().get(5));
    }

    @Test
    void preservesEmptySlotsSoItemsDoNotShiftPosition() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.getInventory().setItem(8, new ItemStack(Material.STONE));

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        // Slot 8 must still be slot 8; a naive filter of nulls would slide it to 0.
        assertNull(restored.getInventory().get(0));
        assertEquals(Material.STONE, restored.getInventory().get(8).getType());
    }

    @Test
    void capturesEnderChest() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.getEnderChest().setItem(2, new ItemStack(Material.EMERALD, 5));

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        assertEquals(new ItemStack(Material.EMERALD, 5), restored.getEnderChest().get(2));
    }

    @Test
    void capturesExperience() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.setLevel(42);
        player.setExp(0.75F);
        player.setTotalExperience(1234);

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        assertEquals(42, restored.getLevel());
        assertEquals(0.75F, restored.getExp(), 0.0001F);
        assertEquals(1234, restored.getTotalExperience());
    }

    @Test
    void capturesVitals() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.setHealth(7.5D);
        player.setFoodLevel(11);
        player.setSaturation(2.5F);

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        assertEquals(7.5D, restored.getHealth(), 0.0001D);
        assertEquals(11, restored.getFoodLevel());
        assertEquals(2.5F, restored.getSaturation(), 0.0001F);
    }

    @Test
    void capturesGameMode() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.setGameMode(GameMode.CREATIVE);

        assertEquals(GameMode.CREATIVE, roundTrip(PlayerProfile.capture(player)).getGameMode());
    }

    @Test
    void capturesPotionEffects() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 1));

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        assertEquals(1, restored.getPotionEffects().size());
        assertEquals(PotionEffectType.SPEED, restored.getPotionEffects().get(0).getType());
        assertEquals(1, restored.getPotionEffects().get(0).getAmplifier());
    }

    @Test
    void capturesLastLocation() throws InvalidConfigurationException {
        Player player = this.server.addPlayer();
        player.teleport(player.getLocation().add(100.5D, 12.0D, -33.25D));

        PlayerProfile restored = roundTrip(PlayerProfile.capture(player));

        assertNotNull(restored.getLastLocation(), "the return spot must survive the round trip");
        assertEquals(player.getLocation().getX(), restored.getLastLocation().getX(), 0.001D);
        assertEquals(player.getLocation().getZ(), restored.getLastLocation().getZ(), 0.001D);
    }

    @Test
    void aFreshProfileIsABlankPlayer() {
        PlayerProfile fresh = PlayerProfile.fresh(GameMode.CREATIVE);

        assertEquals(GameMode.CREATIVE, fresh.getGameMode());
        assertEquals(0, fresh.getLevel());
        assertEquals(0, fresh.getTotalExperience());
        assertEquals(20.0D, fresh.getHealth(), 0.0001D);
        assertEquals(20, fresh.getFoodLevel());
        assertTrue(fresh.getInventory().isEmpty(), "a first visit should arrive with nothing");
    }

    @Test
    void applyingAProfileRestoresThePlayer() {
        Player player = this.server.addPlayer();
        player.getInventory().setItem(0, new ItemStack(Material.GOLD_INGOT, 9));
        player.setLevel(30);

        PlayerProfile saved = PlayerProfile.capture(player);

        player.getInventory().clear();
        player.setLevel(0);

        saved.apply(player);

        assertEquals(new ItemStack(Material.GOLD_INGOT, 9), player.getInventory().getItem(0));
        assertEquals(30, player.getLevel());
    }

    @Test
    void applyingClearsEffectsThePlayerHasButTheProfileDoesNot() {
        Player player = this.server.addPlayer();
        PlayerProfile clean = PlayerProfile.capture(player);

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 0));
        clean.apply(player);

        assertTrue(player.getActivePotionEffects().isEmpty(),
                "a profile with no effects must leave the player with none");
    }
}
