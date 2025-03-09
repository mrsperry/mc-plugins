package com.mrjoshuasperry.enhanceddungeons.dungeons;

import com.google.common.collect.Lists;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonGate;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonGroup;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonMob;

import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class DungeonWorld implements Listener {
    private static final List<CreatureSpawnEvent.SpawnReason> spawnReasons = Lists.newArrayList(
            CreatureSpawnEvent.SpawnReason.CUSTOM, CreatureSpawnEvent.SpawnReason.SHOULDER_ENTITY,
            CreatureSpawnEvent.SpawnReason.SLIME_SPLIT, CreatureSpawnEvent.SpawnReason.SPAWNER
    );

    /** The ID of the dungeon layout */
    private final String id;
    /** The Bukkit world this dungeon is in */
    private final World world;
    /** The config settings of this world */
    private DungeonConfig config;

    /**
     * Creates a new dungeon world
     * @param id The ID of the dungeon layout
     * @param world The Bukkit world
     */
    protected DungeonWorld(final String id, final World world, final DungeonConfig config) {
        this.id = id;
        this.world = world;
        this.config = config;

        final JavaPlugin plugin = Main.getInstance();

        // Register events for this world
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        // Run a task to lock the time on the world
        if (config.getTimeLock()) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> world.setFullTime(world.getFullTime() - 1), 0, 1);
        }
    }

    /**
     * Gets all dungeon mobs that have tags applied to them
     * @param tag The tag to check for
     * @return The number of tagged mobs
     */
    public int getNumberOfTaggedMobs(final String tag) {
        int tagged = 0;

        // Create the namespaced key
        final NamespacedKey key = new NamespacedKey(Main.getInstance(), tag);

        // Check each spawned entity for the key
        for (final LivingEntity entity : this.world.getLivingEntities()) {
            final PersistentDataContainer container = entity.getPersistentDataContainer();

            if (container.has(key, PersistentDataType.BYTE)) {
                tagged++;
            }
        }

        final DungeonConfig config = this.config;

        // Check un-spawned entities
        final Set<DungeonMob> mobs = new HashSet<>(config.getMobs());
        for (final DungeonGroup group : config.getMobGroups()) {
            mobs.addAll(group.getMobs());
        }

        for (final DungeonMob mob : mobs) {
            if (mob.hasTag(tag) && !mob.hasSpawned()) {
                tagged += mob.getCount();
            }
        }

        return tagged;
    }

    /** @return The dungeon ID */
    public String getID() {
        return this.id;
    }

    /** @return The Bukkit world this dungeon is in */
    public World getWorld() {
        return this.world;
    }

    /** @return The config settings of this world */
    public DungeonConfig getConfig() {
        return this.config;
    }

    /** @param config The new config to assign to this world */
    public void setConfig(final DungeonConfig config) {
        this.config = config;
    }

    /**
     * Checks if a location is an ending block location
     * @param location The location to check
     * @return If the location is an ending block location
     */
    private boolean checkEndingBlock(final Location location) {
        // Check if the block was an ending block
        for (final Location ending : this.getConfig().getEndingLocations()) {
            if (Utils.compareLocations(ending, location)) {
                // Get the instance currently running
                final DungeonInstance instance = DungeonHandler.getDungeonInstance(this.id);
                if (instance == null) {
                    return false;
                }

                // End the run
                instance.end(false);

                return true;
            }
        }

        return false;
    }

    @EventHandler
    private void onWeatherChange(final WeatherChangeEvent event) {
        if (event.getWorld() == this.world) {
            event.setCancelled(!this.config.getAllowWeather());
        }
    }

    @EventHandler
    private void onPlayerLeave(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (player.getWorld() == this.world) {
            // Get the dungeon instance of this world
            final DungeonInstance instance = DungeonHandler.getDungeonInstance(this.id);
            if (instance == null) {
                return;
            }

            // Check if the player who left was part of the instance's party
            final Party party = instance.getParty();
            final Set<Player> members = party.getMembers();

            if (members.contains(player)) {
                // Teleport the player back to their return location
                final Location location = instance.getReturnLocation(player);
                if (location != null) {
                    player.teleport(location);
                }

                // End the instance if they were the only player online
                if (members.size() == 1) {
                    instance.end(true);
                    PartyHandler.removeParty(player);

                    return;
                }

                party.removeMember(player);

                boolean online = false;
                for (final Player member : members) {
                    if (member.isOnline()) {
                        online = true;
                        break;
                    }
                }

                if (!online) {
                    instance.end(true);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        if (player.getWorld() == this.world) {
            if (player.getGameMode() == GameMode.CREATIVE) {
                return;
            }

            // Try to spawn mobs with proximity detectors
            for (final DungeonMob mob : this.config.getMobs()) {
                mob.checkProximities(player);
            }

            // Try to spawn groups with proximity detectors
            for (final DungeonGroup group : this.config.getMobGroups()) {
                group.checkProximities(player);
            }
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        // Only trigger on the main hand right click
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Ending location must be a block
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        // Ensure the block exists
        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // Ensure the world is correct
        final World world = block.getWorld();
        if (world != this.world) {
            return;
        }

        event.setCancelled(this.checkEndingBlock(block.getLocation()));
    }

    @EventHandler
    private void onCreatureSpawn(final CreatureSpawnEvent event) {
        if (event.getEntity().getWorld() == this.world) {
            // Cancel any blocked spawn events
            event.setCancelled(!DungeonWorld.spawnReasons.contains(event.getSpawnReason()));
        }
    }

    @EventHandler
    private void onEntityDeath(final EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        if (entity.getWorld() == this.world) {
            // Handle custom drops
            final List<ItemStack> drops = event.getDrops();
            boolean complete = false;

            for (final DungeonMob mob : this.config.getAllMobs()) {
                for (final Entity spawned : mob.getEntities()) {
                    if (entity == spawned) {
                        if (!mob.getIncludeDefaultDrops()) {
                            drops.clear();
                        }

                        drops.addAll(mob.getCustomDrops());

                        complete = true;
                        break;
                    }
                }

                // Early return if the drops have been set
                if (complete) {
                    break;
                }
            }

            // Handle gate opening
            for (final DungeonGate gate : this.config.getGates()) {
                // Check if the gate has any mobs remaining
                if (this.getNumberOfTaggedMobs(gate.getID()) == 0) {
                    if (gate.trigger()) {
                        final Player killer = entity.getKiller();
                        if (killer == null) {
                            continue;
                        }

                        final Party party = PartyHandler.getPartyByMember(killer);
                        if (party == null) {
                            continue;
                        }

                        for (final Player member : party.getMembers()) {
                            member.sendMessage(ChatColor.GREEN + "A gate has opened!");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        // Ensure the world is correct
        final Location location = event.getEntity().getLocation();
        if (location.getWorld() != this.world) {
            return;
        }

        // Only trigger on player damage
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        event.setCancelled(this.checkEndingBlock(location));
    }

    @EventHandler
    private void onEntityExplode(final EntityExplodeEvent event) {
        if (event.getLocation().getWorld() == this.world) {
            event.blockList().clear();
        }
    }

    @EventHandler
    private void onBlockBreak(final BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            if (event.getBlock().getWorld() == this.world) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onBlockExplode(final BlockExplodeEvent event) {
        if (event.getBlock().getWorld() == this.world) {
            event.blockList().clear();
        }
    }

    @EventHandler
    private void onBlockBurn(final BlockBurnEvent event) {
        if (event.getBlock().getWorld() == this.world) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockPlace(final BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            if (event.getBlock().getWorld() == this.world) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onBlockGrow(final BlockGrowEvent event) {
        if (event.getBlock().getWorld() == this.world) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockFade(final BlockFadeEvent event) {
        if (event.getBlock().getWorld() == this.world) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockSpread(final BlockSpreadEvent event) {
        if (event.getBlock().getWorld() == this.world) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockForm(final BlockFormEvent event) {
        if (event.getBlock().getWorld() == this.world) {
            event.setCancelled(true);
        }
    }
}
