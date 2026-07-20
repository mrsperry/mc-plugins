package com.mrjoshuasperry.worldclusters.travel;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.world.ManagedWorld;
import com.mrjoshuasperry.worldclusters.world.WorldRole;

/**
 * Keeps respawning inside the cluster the player died in.
 *
 * <p>
 * Vanilla sends a player without a bed to the primary world's spawn, which for a
 * death in the creative cluster would be a boundary crossing triggered by dying
 * — the worst possible time to have an inventory swapped. A bed or anchor is
 * honoured only if it is in the same cluster.
 */
public class RespawnListener implements Listener {
    private final ClusterRegistry registry;

    public RespawnListener(ClusterRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        String clusterId = this.registry.getClusterId(event.getPlayer().getWorld());

        Location respawn = event.getRespawnLocation();
        if (respawn != null && this.registry.getClusterId(respawn.getWorld()).equals(clusterId)) {
            return;
        }

        ManagedWorld overworld = this.registry.getWorldWithRole(clusterId, WorldRole.OVERWORLD);
        if (overworld != null && overworld.isLoaded()) {
            event.setRespawnLocation(overworld.getWorld().getSpawnLocation());
            return;
        }

        // No overworld in this cluster; the world they died in is the best
        // remaining option that stays on this side of the boundary.
        event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
    }
}
