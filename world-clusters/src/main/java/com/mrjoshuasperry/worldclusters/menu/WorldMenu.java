package com.mrjoshuasperry.worldclusters.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mrjoshuasperry.mcutils.menu.PaginatedMenu;
import com.mrjoshuasperry.mcutils.menu.items.MenuItem;
import com.mrjoshuasperry.mcutils.menu.items.StaticMenuItem;
import com.mrjoshuasperry.worldclusters.cluster.Cluster;
import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.travel.TeleportService;
import com.mrjoshuasperry.worldclusters.world.ManagedWorld;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * The {@code /worlds} picker: every managed world as a clickable icon.
 *
 * <p>
 * Worlds in a different cluster from the viewer are marked, since clicking one
 * swaps their inventory — that should never be a surprise.
 *
 * <p>
 * Built per player, as {@link com.mrjoshuasperry.mcutils.menu.Menu} requires.
 */
public class WorldMenu extends PaginatedMenu {
    private static final int ROWS = 6;

    public WorldMenu(Player viewer, ClusterRegistry registry, TeleportService teleports, Plugin plugin) {
        super(Component.text("Worlds", NamedTextColor.DARK_GRAY), ROWS,
                buildItems(viewer, registry, teleports, plugin));
    }

    private static List<MenuItem> buildItems(Player viewer, ClusterRegistry registry, TeleportService teleports,
            Plugin plugin) {
        String viewerCluster = registry.getClusterId(viewer.getWorld());
        List<MenuItem> items = new ArrayList<>();

        for (Cluster cluster : registry.getClusters()) {
            for (String worldName : cluster.getWorlds()) {
                ManagedWorld world = registry.getManagedWorld(worldName);
                if (world == null) {
                    continue;
                }

                items.add(buildItem(world, cluster, viewerCluster, teleports, plugin));
            }
        }

        return items;
    }

    private static MenuItem buildItem(ManagedWorld world, Cluster cluster, String viewerCluster,
            TeleportService teleports, Plugin plugin) {
        List<Component> lore = new ArrayList<>();
        lore.add(line("Cluster: " + cluster.getDisplayName(), NamedTextColor.DARK_AQUA));

        if (!world.isLoaded()) {
            lore.add(line("Not loaded", NamedTextColor.RED));
        } else if (cluster.getId().equals(viewerCluster)) {
            lore.add(line("Your inventory comes with you", NamedTextColor.GREEN));
        } else {
            lore.add(line("Crosses a boundary", NamedTextColor.YELLOW));
            lore.add(line("Your inventory and XP stay behind", NamedTextColor.GRAY));
        }

        return new StaticMenuItem(
                world.getDisplayItem().build(world.getName(), lore),
                (player, menu) -> {
                    // Deferred a tick: closing an inventory and teleporting from
                    // inside InventoryClickEvent dispatch can leave the client's
                    // cursor item desynced.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.closeInventory();

                        if (!teleports.teleport(player, world)) {
                            player.sendMessage(Component.text(
                                    "Could not send you to '" + world.getName() + "'.", NamedTextColor.RED));
                        }
                    });
                });
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
