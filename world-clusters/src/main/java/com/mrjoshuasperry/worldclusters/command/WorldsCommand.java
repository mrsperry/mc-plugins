package com.mrjoshuasperry.worldclusters.command;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.menu.WorldMenu;
import com.mrjoshuasperry.worldclusters.travel.TeleportService;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * {@code /worlds} — opens the world picker. The one command players use.
 */
public class WorldsCommand {
    public static final String PERMISSION = "worldclusters.use";

    private final ClusterRegistry registry;
    private final TeleportService teleports;
    private final Plugin plugin;

    public WorldsCommand(ClusterRegistry registry, TeleportService teleports, Plugin plugin) {
        this.registry = registry;
        this.teleports = teleports;
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands
                .literal("worlds")
                .requires(source -> source.getExecutor() instanceof Player
                        && source.getSender().hasPermission(PERMISSION))
                .executes(this::openMenu);
    }

    private int openMenu(CommandContext<CommandSourceStack> context) {
        Player player = (Player) context.getSource().getExecutor();

        // Built fresh per open: menus carry per-viewer state, and the world list
        // can change between opens.
        new WorldMenu(player, this.registry, this.teleports, this.plugin).open(player);

        return Command.SINGLE_SUCCESS;
    }
}
