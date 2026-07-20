package com.mrjoshuasperry.worldclusters.perms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.worldclusters.cluster.Cluster;
import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;

/**
 * Grants and denies permission nodes based on which cluster a player is in.
 *
 * <p>
 * This is how per-cluster plugin access works. A plugin cannot actually be
 * enabled for one world and not another — plugins are server-wide — so instead
 * we take the nodes they gate their commands on. Listing {@code worldedit.*}
 * under the creative cluster means WorldEdit answers there and refuses in
 * survival, which is the observable behaviour wanted.
 *
 * <p>
 * Nodes are set <b>explicitly false</b> outside their cluster rather than just
 * omitted. An attachment value takes precedence over op's implicit "yes", so an
 * explicit deny is the only thing that stops an op using a gated command.
 *
 * <p>
 * Whether a wildcard like {@code worldedit.*} cascades to its children depends
 * on the target plugin registering them as children of that node. When it
 * doesn't, list the specific nodes instead.
 */
public class ClusterPermissions {
    private final JavaPlugin plugin;
    private final ClusterRegistry registry;

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public ClusterPermissions(JavaPlugin plugin, ClusterRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /** Re-applies a player's nodes for whichever cluster they are now in. */
    public void apply(Player player) {
        this.clear(player);

        Cluster cluster = this.registry.getCluster(player.getWorld());
        Set<String> granted = cluster == null ? Set.of() : cluster.getPermissions();

        // Every node any cluster grants, so the ones this cluster doesn't grant
        // can be denied rather than left to op's defaults.
        Set<String> allNodes = new HashSet<>(this.registry.getAllGrantedPermissions());
        if (allNodes.isEmpty()) {
            return;
        }

        PermissionAttachment attachment = player.addAttachment(this.plugin);
        for (String node : allNodes) {
            attachment.setPermission(node, granted.contains(node));
        }

        this.attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    /** Drops a player's attachment. Call on quit. */
    public void clear(Player player) {
        PermissionAttachment attachment = this.attachments.remove(player.getUniqueId());
        if (attachment == null) {
            return;
        }

        // removeAttachment throws if it was already detached, which happens when
        // the player disconnected before we got here.
        try {
            player.removeAttachment(attachment);
        } catch (IllegalArgumentException ex) {
            this.plugin.getLogger().fine("Permission attachment was already detached for " + player.getName());
        }
    }

    /** Re-applies every online player's nodes, after a config reload. */
    public void refreshAll() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            this.apply(player);
        }
    }
}
