package com.mrjoshuasperry.worldclusters.travel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.profile.BoundaryService;
import com.mrjoshuasperry.worldclusters.world.ManagedWorld;

/**
 * Command- and menu-driven teleporting between managed worlds.
 *
 * <p>
 * Picks the arrival spot: returning to a cluster puts a player back where they
 * left it, not at spawn, which is what makes the survival side feel untouched
 * after a detour through creative.
 */
public class TeleportService {
    private final ClusterRegistry registry;
    private final BoundaryService boundary;

    public TeleportService(ClusterRegistry registry, BoundaryService boundary) {
        this.registry = registry;
        this.boundary = boundary;
    }

    /**
     * Sends a player to a world.
     *
     * <p>
     * The boundary swap is not done here — it happens on
     * {@code PlayerChangedWorldEvent}, so it applies just as much to a portal or
     * another plugin's teleport as it does to this one.
     *
     * @return whether the teleport was accepted
     */
    public boolean teleport(Player player, ManagedWorld destination) {
        if (!destination.isLoaded()) {
            return false;
        }

        World world = destination.getWorld();

        if (world.equals(player.getWorld())) {
            return false;
        }

        player.teleport(this.arrivalFor(player, world));
        return true;
    }

    /**
     * Where a player lands in a world: their last spot in that cluster if they
     * have one and it is in this world, otherwise the world's spawn.
     */
    public Location arrivalFor(Player player, World world) {
        String clusterId = this.registry.getClusterId(world);

        // Only the cluster they are leaving triggers a restore; hopping within a
        // cluster should land on the requested world, not wherever they last were.
        if (!clusterId.equals(this.registry.getClusterId(player.getWorld()))) {
            Location last = this.boundary.getReturnLocation(player, clusterId);
            if (last != null && last.getWorld() != null && last.getWorld().equals(world)) {
                return last;
            }
        }

        return world.getSpawnLocation();
    }
}
