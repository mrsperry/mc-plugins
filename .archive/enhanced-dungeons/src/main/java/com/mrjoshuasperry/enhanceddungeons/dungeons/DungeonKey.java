package com.mrjoshuasperry.enhanceddungeons.dungeons;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.types.EntityTypes;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.logging.Level;

public class DungeonKey implements Listener {
    /** The global display name for dungeon keys */
    private static final String displayName = ChatColor.GOLD + "Dungeon Key";

    /** The percent chance of a dungeon key to drop when a monster dies */
    private static double dropChance;
    /** If a dungeon key should only drop when a player kills a monster */
    private static boolean requirePlayerKill;

    /**
     * Loads dungeon key settings from the config
     * @param config The section used for config values
     */
    public static void initialize(final ConfigurationSection config) {
        DungeonKey.dropChance = config.getDouble("drop-chance", 0.05);
        DungeonKey.requirePlayerKill = config.getBoolean("require-player-kill", false);
    }

    /**
     * Creates a new dungeon key with the specified dungeon layout
     * @param id The ID of the dungeon layout
     * @return The dungeon key item or null if the layout was not found
     */
    public static ItemStack create(String id) {
        if (id == null) {
            return null;
        }

        // Create the item
        return new ItemBuilder(Material.TRIPWIRE_HOOK)
                .setName(DungeonKey.displayName)
                .addLore(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + Utils.idToDisplay(id))
                .build();
    }

    /**
     * Creates a new dungeon key with a random dungeon layout
     * @return The dungeon key item or null if no layouts were found
     */
    public static ItemStack createRandom() {
        return DungeonKey.create(DungeonHandler.getRandomDungeonID());
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        // Only trigger if the key is in the correct hand
        if (event.getHand() == EquipmentSlot.HAND) {
            // Get the item that was clicked
            final ItemStack stack = event.getItem();
            if (stack == null) {
                return;
            }

            // Make sure the item has meta
            final ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                return;
            }

            // Check if the name of the item matches the global key name
            if (meta.getDisplayName().equals(DungeonKey.displayName)) {
                final List<String> lore = meta.getLore();

                // Make sure there is an ID attached to this key
                if (lore == null || lore.size() != 1) {
                    Utils.log(Level.SEVERE, "A dungeon key was used but the dungeon ID could not be found!");
                    return;
                }
                // Convert the display string to an ID
                final String id = Utils.displayToID(ChatColor.stripColor(lore.get(0)));

                // Cancel the click event
                event.setCancelled(true);

                // Get the player that clicked the key
                final Player player = event.getPlayer();

                // Check if the player is already in a dungeon world
                if (player.getGameMode() != GameMode.CREATIVE) {
                    final World world = player.getWorld();

                    for (final DungeonWorld dungeon : DungeonHandler.getDungeonWorlds()) {
                        if (dungeon.getWorld() == world) {
                            player.sendMessage(ChatColor.RED + "You may not join a new dungeon in a dungeon world");
                            return;
                        }
                    }
                }

                // Check if there is a party associated with the player
                Party party = PartyHandler.getPartyByMember(player);
                if (party == null) {
                    PartyHandler.createParty(player);
                    party = PartyHandler.getPartyByMember(player);
                }

                // Create the dungeon world
                if (DungeonHandler.createDungeonInstance(id, party) != null) {
                    // Remove the key if the player was not in creative
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        stack.setAmount(stack.getAmount() - 1);
                    }
                }
            }
        }
    }

    @EventHandler
    private void onEntityDeath(final EntityDeathEvent event) {
        // Only drop keys on hostile monster deaths
        if (EntityTypes.getHostileTypes().contains(event.getEntityType())) {
            // Check if the monster was killed by a player
            if (DungeonKey.requirePlayerKill && event.getEntity().getKiller() == null) {
                return;
            }

            // Roll a chance to drop a random dungeon key (chance >= 0-100)
            if (DungeonKey.dropChance >= Main.getRandom().nextDouble() * 100) {
                final List<ItemStack> drops = event.getDrops();
                drops.add(DungeonKey.createRandom());
            }
        }
    }
}
