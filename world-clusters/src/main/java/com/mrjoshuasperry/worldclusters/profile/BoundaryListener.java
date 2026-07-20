package com.mrjoshuasperry.worldclusters.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.perms.ClusterPermissions;

/**
 * Detects cluster crossings and hands them to {@link BoundaryService}.
 */
public class BoundaryListener implements Listener {
    private final ClusterRegistry registry;
    private final ProfileStore store;
    private final BoundaryService boundary;
    private final ClusterPermissions permissions;

    /**
     * Where each player stood before their most recent teleport.
     *
     * <p>
     * {@link PlayerChangedWorldEvent} fires <i>after</i> the move, so by then the
     * player's location is already the destination and the exact spot they left
     * from is gone. Recording it on the teleport is the only way to restore them
     * to it when they come back.
     */
    private final Map<UUID, Location> departureLocations = new HashMap<>();

    public BoundaryListener(ClusterRegistry registry, ProfileStore store, BoundaryService boundary,
            ClusterPermissions permissions) {
        this.registry = registry;
        this.store = store;
        this.boundary = boundary;
        this.permissions = permissions;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        this.departureLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        String fromCluster = this.registry.getClusterId(event.getFrom());
        String toCluster = this.registry.getClusterId(player.getWorld());

        // The overwhelmingly common case: overworld to nether, creative to
        // redstone. Nothing is swapped and nothing is written.
        if (fromCluster.equals(toCluster)) {
            return;
        }

        Location departure = this.departureLocations.remove(player.getUniqueId());
        if (departure == null || !departure.getWorld().equals(event.getFrom())) {
            departure = event.getFrom().getSpawnLocation();
        }

        this.boundary.cross(player, fromCluster, departure);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        this.store.load(uuid);

        String actualCluster = this.registry.getClusterId(player.getWorld());
        String recordedCluster = this.store.getCurrentCluster(uuid);

        if (recordedCluster != null && !recordedCluster.equals(actualCluster)) {
            // They logged in somewhere other than where their live state belongs —
            // a world was deleted or reassigned while they were away. Treat the
            // login as a boundary crossing so the old state doesn't leak in.
            this.boundary.cross(player, recordedCluster, null);
        } else {
            this.store.setCurrentCluster(uuid, actualCluster);
            this.store.flush(uuid);
            this.permissions.apply(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String cluster = this.registry.getClusterId(player.getWorld());
        PlayerProfile profile = PlayerProfile.capture(player);
        this.store.put(uuid, cluster, profile);
        this.store.setCurrentCluster(uuid, cluster);
        this.store.flush(uuid);

        this.permissions.clear(player);
        this.departureLocations.remove(uuid);
        this.store.unload(uuid);
    }
}
