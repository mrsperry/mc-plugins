package com.mrjoshuasperry.worldclusters.travel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.world.ManagedWorld;
import com.mrjoshuasperry.worldclusters.world.WorldRole;

/**
 * Keeps portal travel inside the cluster it started in.
 *
 * <p>
 * Vanilla links dimensions server-wide, so a nether portal built in the creative
 * cluster would drop the player in the survival nether — silently crossing a
 * boundary they never asked to cross. Instead the destination is resolved from
 * the source world's own cluster, and a portal with no counterpart there is made
 * inert rather than sending the player somewhere wrong.
 */
public class PortalListener implements Listener {
    /** Nether portals compress horizontal distance by this factor. */
    private static final double NETHER_SCALE = 8.0D;

    private final ClusterRegistry registry;

    public PortalListener(ClusterRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        World origin = event.getFrom().getWorld();
        String clusterId = this.registry.getClusterId(origin);

        WorldRole targetRole = this.targetRoleFor(event.getCause(), origin);
        if (targetRole == null) {
            return;
        }

        ManagedWorld destination = this.registry.getWorldWithRole(clusterId, targetRole);

        if (destination == null || !destination.isLoaded()) {
            // No counterpart in this cluster; a dead portal is better than one
            // that quietly moves the player across a boundary.
            event.setCancelled(true);
            return;
        }

        World destinationWorld = destination.getWorld();
        if (destinationWorld.equals(event.getTo() == null ? null : event.getTo().getWorld())) {
            // Vanilla already picked the right world for this cluster.
            return;
        }

        event.setTo(this.scaledLocation(event.getFrom(), destinationWorld, targetRole));
        event.setCanCreatePortal(true);
    }

    /**
     * Which role the player is heading for, or null when this isn't a portal we
     * route.
     */
    private WorldRole targetRoleFor(TeleportCause cause, World origin) {
        WorldRole originRole = WorldRole.fromEnvironment(origin.getEnvironment());

        return switch (cause) {
            // A nether portal toggles between the overworld and the nether.
            case NETHER_PORTAL -> originRole == WorldRole.NETHER ? WorldRole.OVERWORLD : WorldRole.NETHER;
            // An end portal goes to the end; leaving the end returns to the overworld.
            case END_PORTAL -> originRole == WorldRole.END ? WorldRole.OVERWORLD : WorldRole.END;
            case END_GATEWAY -> WorldRole.END;
            default -> null;
        };
    }

    /**
     * Applies vanilla's 8:1 nether coordinate scaling so a portal in a cluster's
     * nether lands where the player expects in its overworld, and vice versa.
     */
    private Location scaledLocation(Location from, World destination, WorldRole targetRole) {
        double scale = 1.0D;

        if (targetRole == WorldRole.NETHER && from.getWorld().getEnvironment() == World.Environment.NORMAL) {
            scale = 1.0D / NETHER_SCALE;
        } else if (targetRole == WorldRole.OVERWORLD && from.getWorld().getEnvironment() == World.Environment.NETHER) {
            scale = NETHER_SCALE;
        }

        if (scale == 1.0D) {
            return destination.getSpawnLocation();
        }

        Location scaled = new Location(
                destination,
                from.getX() * scale,
                from.getY(),
                from.getZ() * scale,
                from.getYaw(),
                from.getPitch());

        // Keep inside the destination's border; the scaled coordinate can land
        // outside it when the two worlds have different border sizes.
        double limit = destination.getWorldBorder().getSize() / 2.0D;
        scaled.setX(Math.clamp(scaled.getX(), -limit, limit));
        scaled.setZ(Math.clamp(scaled.getZ(), -limit, limit));

        return scaled;
    }
}
