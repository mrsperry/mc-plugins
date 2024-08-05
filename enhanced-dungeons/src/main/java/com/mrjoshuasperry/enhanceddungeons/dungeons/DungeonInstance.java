package com.mrjoshuasperry.enhanceddungeons.dungeons;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonGate;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonGroup;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonLoot;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonMob;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class DungeonInstance {
    /** The dungeon this instance creates */
    private final DungeonWorld dungeon;
    /** The party participating in the dungeon */
    private final Party party;
    /** A list of return locations when the dungeon completes */
    private final Map<Player, Location> returnLocations;

    /** The join countdown task */
    private final int startCountdownTask;
    private final int endingLocationTask;

    /**
     * Creates a new dungeon instance
     * @param dungeon The dungeon to create
     * @param party The party participating in the dungeon
     */
    public DungeonInstance(final DungeonWorld dungeon, final Party party) {
        this.dungeon = dungeon;
        this.party = party;
        this.returnLocations = new HashMap<>();

        final BukkitScheduler scheduler = Bukkit.getScheduler();
        final JavaPlugin plugin = Main.getInstance();

        // Teleport all players to the dungeon world after a short countdown
        this.startCountdownTask = scheduler.runTaskTimer(plugin, new Runnable() {
            private int countdown = 5;

            @Override
            public void run() {
                // Teleport players to the dungeon at 0 seconds remaining
                if (this.countdown == 0) {
                    join();

                    scheduler.cancelTask(startCountdownTask);
                    return;
                }

                // Send a message to all members notifying them of the time left
                for (final Player player : party.getMembers()) {
                    player.sendMessage(ChatColor.GRAY + "Teleporting to dungeon in " + ChatColor.GREEN + countdown + ChatColor.GRAY + "...");
                }

                this.countdown--;
            }
        }, 0, 20).getTaskId();

        // Spawn particles and sounds for ending blocks
        this.endingLocationTask = scheduler.runTaskTimer(plugin, () -> {
            final World world = dungeon.getWorld();

            for (final Location ending : dungeon.getConfig().getEndingLocations()) {
                final Location center = Utils.centerLocation(ending);
                world.spawnParticle(Particle.PORTAL, center, 50);
                world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1, 1);
            }
        }, 0, 10).getTaskId();

        // Spawn in the loot in containers on the map
        for (final DungeonLoot loot : this.dungeon.getConfig().getLoot()) {
            loot.spawn();
        }
    }

    /** Teleports all party members to the dungeon world */
    private void join() {
        final World world = this.dungeon.getWorld();

        final Location spawn;
        final Set<Location> spawns = this.dungeon.getConfig().getSpawnLocations();

        // Set the spawn location to the world spawn or get a random location from the defined spawn points
        if (spawns.size() == 0) {
            spawn = world.getSpawnLocation();
        } else {
            spawn = (Location) spawns.toArray()[Main.getRandom().nextInt(spawns.size())];
        }

        for (final Player player : this.party.getMembers()) {
            Utils.log(Level.INFO, "Teleporting " + player.getName() + " to " + world.getName());

            // Store the player's location before the teleport
            this.returnLocations.put(player, player.getLocation());

            player.teleport(spawn);
        }

        final DungeonConfig config = this.dungeon.getConfig();
        // Try to spawn each mob (for mobs with no proximity detector)
        for (final DungeonMob mob : config.getMobs()) {
            mob.checkProximities(null);
        }

        // Try to spawn each mob group
        for (final DungeonGroup group : config.getMobGroups()) {
            group.checkProximities(null);
        }
    }

    /**
     * Ends a dungeon run if all required mobs have been killed
     * @param force If this instance should be forcibly ended
     */
    public void end(final boolean force) {
        // Check if all required mobs have been killed
        final int mobsLeft = this.dungeon.getNumberOfTaggedMobs("required-kill");
        if (!force && mobsLeft != 0) {
            final boolean singular = mobsLeft == 1;
            final String prefix = "There " + (singular ? "is " : "are ") + mobsLeft + " mob" + (singular ? "" : "s");

            for (final Player player : this.party.getMembers()) {
                player.sendMessage(ChatColor.RED + prefix + " you must kill before leaving!");
            }

            return;
        }

        for (final Player player : this.party.getMembers()) {
            final Location location = this.returnLocations.getOrDefault(player, null);

            if (location != null) {
                player.teleport(this.returnLocations.get(player));
            }
        }

        // Disband parties that have only a single member
        if (this.party.getMembers().size() == 1) {
            PartyHandler.removeParty(this.party.getOwner());
        }

        final DungeonConfig config = this.dungeon.getConfig();
        for (final DungeonGate gate : config.getGates()) {
            gate.regenerate();
        }

        for (final DungeonMob mob : config.getMobs()) {
            mob.remove();
        }

        for (final DungeonGroup group : config.getMobGroups()) {
            group.remove();
        }

        for (final DungeonLoot loot : config.getLoot()) {
            loot.remove();
        }

        for (final Item item : this.dungeon.getWorld().getEntitiesByClass(Item.class)) {
            item.remove();
        }

        // Cancel ongoing tasks
        final BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.cancelTask(this.startCountdownTask);
        scheduler.cancelTask(this.endingLocationTask);

        DungeonHandler.removeDungeonInstance(this.dungeon.getID());

        Utils.log(Level.INFO, "Dungeon run ended: " + this.dungeon.getID());
    }

    /** @return The party of this dungeon instance */
    public Party getParty() {
        return this.party;
    }

    /**
     * @param player The player to be teleported
     * @return The location to teleport the player or null if it could not be found
     */
    public Location getReturnLocation(final Player player) {
        return this.returnLocations.getOrDefault(player, null);
    }
}
