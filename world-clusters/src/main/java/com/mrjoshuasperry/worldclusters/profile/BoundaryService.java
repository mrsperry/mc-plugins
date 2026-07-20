package com.mrjoshuasperry.worldclusters.profile;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import com.mrjoshuasperry.worldclusters.cluster.Cluster;
import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.perms.ClusterPermissions;

/**
 * Performs the player-state swap when a player crosses between clusters.
 *
 * <p>
 * This is the whole point of the plugin: inside a cluster nothing happens at
 * all, and between clusters the player is wiped and restored so the two sides
 * can never exchange items or XP.
 */
public class BoundaryService {
    private final ClusterRegistry registry;
    private final ProfileStore store;
    private final ClusterPermissions permissions;

    public BoundaryService(ClusterRegistry registry, ProfileStore store, ClusterPermissions permissions) {
        this.registry = registry;
        this.store = store;
        this.permissions = permissions;
    }

    /**
     * Swaps a player from one cluster to another.
     *
     * @param player       the player, already standing in the destination world
     * @param fromCluster  the cluster they left
     * @param fromLocation where they were standing when they left, for the
     *                     return trip
     */
    public void cross(Player player, String fromCluster, Location fromLocation) {
        String toCluster = this.registry.getClusterId(player.getWorld());
        if (fromCluster.equals(toCluster)) {
            return;
        }

        PlayerProfile outgoing = PlayerProfile.capture(player);
        if (fromLocation != null) {
            outgoing.setLastLocation(fromLocation);
        }
        this.store.put(player.getUniqueId(), fromCluster, outgoing);

        this.wipe(player);

        PlayerProfile incoming = this.store.get(player.getUniqueId(), toCluster);
        if (incoming == null) {
            Cluster cluster = this.registry.getClusterById(toCluster);
            incoming = PlayerProfile.fresh(cluster == null ? GameMode.SURVIVAL : cluster.getDefaultGameMode());
        }
        incoming.apply(player);

        this.store.setCurrentCluster(player.getUniqueId(), toCluster);
        // One write for the whole crossing, after both the outgoing profile and
        // the new current cluster are in place.
        this.store.flush(player.getUniqueId());

        this.permissions.apply(player);
    }

    /**
     * Resets a player to a blank slate before the destination profile lands on
     * top. Without this, anything the incoming profile does not explicitly set —
     * an empty inventory slot, a missing potion effect — would carry over.
     */
    private void wipe(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setItemOnCursor(null);

        for (PotionEffect effect : List.copyOf(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }

        player.setFireTicks(0);
        player.setFreezeTicks(0);
        // Cleared so a cluster hop can't be used to shed accumulated fall damage.
        player.setFallDistance(0.0F);
        player.setAbsorptionAmount(0.0D);
    }

    /**
     * The location to send a player when they enter a cluster, or null to leave
     * them wherever the teleport put them.
     */
    public Location getReturnLocation(Player player, String clusterId) {
        PlayerProfile profile = this.store.get(player.getUniqueId(), clusterId);
        return profile == null ? null : profile.getLastLocation();
    }

    public ProfileStore getStore() {
        return this.store;
    }
}
