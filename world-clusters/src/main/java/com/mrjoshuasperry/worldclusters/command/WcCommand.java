package com.mrjoshuasperry.worldclusters.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mrjoshuasperry.mcutils.confirm.ConfirmationManager;
import com.mrjoshuasperry.worldclusters.WorldClusters;
import com.mrjoshuasperry.worldclusters.cluster.Cluster;
import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.travel.TeleportService;
import com.mrjoshuasperry.worldclusters.world.DisplayItem;
import com.mrjoshuasperry.worldclusters.world.ManagedWorld;
import com.mrjoshuasperry.worldclusters.world.WorldManager;
import com.mrjoshuasperry.worldclusters.world.WorldRole;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * {@code /wc} — the admin surface: world lifecycle and cluster membership.
 *
 * <p>
 * {@code /wc delete} is routed through {@link ConfirmationManager} because it
 * removes a world folder from disk and cannot be undone.
 */
public class WcCommand {
    public static final String PERMISSION = "worldclusters.admin";

    private final WorldClusters plugin;
    private final ClusterRegistry registry;
    private final WorldManager worlds;
    private final TeleportService teleports;
    private final ConfirmationManager confirmations;

    public WcCommand(WorldClusters plugin, ClusterRegistry registry, WorldManager worlds,
            TeleportService teleports, ConfirmationManager confirmations) {
        this.plugin = plugin;
        this.registry = registry;
        this.worlds = worlds;
        this.teleports = teleports;
        this.confirmations = confirmations;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands
                .literal("wc")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("list").executes(this::list))
                .then(Commands.literal("info")
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(this::suggestWorlds)
                                .executes(this::info)))
                .then(Commands.literal("tp")
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(this::suggestWorlds)
                                .executes(this::teleport)))
                .then(Commands.literal("load")
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(this::suggestWorlds)
                                .executes(this::load)))
                .then(Commands.literal("unload")
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(this::suggestWorlds)
                                .executes(this::unload)))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("cluster", StringArgumentType.word())
                                        .suggests(this::suggestClusters)
                                        .executes(context -> this.create(context, World.Environment.NORMAL))
                                        .then(Commands.argument("environment", StringArgumentType.word())
                                                .suggests(this::suggestEnvironments)
                                                .executes(this::createWithEnvironment)))))
                .then(Commands.literal("import")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("cluster", StringArgumentType.word())
                                        .suggests(this::suggestClusters)
                                        .executes(this::importWorld))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(this::suggestWorlds)
                                .executes(this::delete)))
                .then(Commands.literal("cluster")
                        .then(Commands.literal("create")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(this::createCluster)))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(this::suggestClusters)
                                        .executes(this::deleteCluster)))
                        .then(Commands.literal("assign")
                                .then(Commands.argument("world", StringArgumentType.word())
                                        .suggests(this::suggestWorlds)
                                        .then(Commands.argument("cluster", StringArgumentType.word())
                                                .suggests(this::suggestClusters)
                                                .executes(this::assign)))))
                .then(Commands.literal("reload").executes(this::reload));
    }

    private int list(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        for (Cluster cluster : this.registry.getClusters()) {
            sender.sendMessage(Component.text(cluster.getId(), NamedTextColor.AQUA)
                    .append(Component.text(" (" + cluster.getDefaultGameMode().name().toLowerCase() + ")",
                            NamedTextColor.DARK_GRAY)));

            for (String worldName : cluster.getWorlds()) {
                ManagedWorld world = this.registry.getManagedWorld(worldName);
                boolean loaded = world != null && world.isLoaded();

                sender.sendMessage(Component.text("  " + worldName, NamedTextColor.WHITE)
                        .append(Component.text(
                                world == null ? "" : " [" + world.getRole().name().toLowerCase() + "]",
                                NamedTextColor.GRAY))
                        .append(loaded
                                ? Component.text(" loaded", NamedTextColor.GREEN)
                                : Component.text(" unloaded", NamedTextColor.RED)));
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "world");

        ManagedWorld world = this.registry.getManagedWorld(name);
        if (world == null) {
            return fail(sender, "'" + name + "' is not a managed world.");
        }

        sender.sendMessage(Component.text(world.getName(), NamedTextColor.AQUA));
        sender.sendMessage(info("Cluster", this.registry.getClusterId(world.getName())));
        sender.sendMessage(info("Role", world.getRole().name().toLowerCase()));
        sender.sendMessage(info("Environment", world.getEnvironment().name().toLowerCase()));
        sender.sendMessage(info("Type", world.getWorldType().name().toLowerCase()));
        sender.sendMessage(info("Seed", world.getSeed() == null ? "random" : world.getSeed().toString()));
        sender.sendMessage(info("Loaded", Boolean.toString(world.isLoaded())));

        return Command.SINGLE_SUCCESS;
    }

    private static Component info(String label, String value) {
        return Component.text("  " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private int teleport(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            return fail(sender, "Only a player can teleport.");
        }

        String name = StringArgumentType.getString(context, "world");
        ManagedWorld world = this.registry.getManagedWorld(name);

        if (world == null) {
            return fail(sender, "'" + name + "' is not a managed world.");
        }
        if (!this.teleports.teleport(player, world)) {
            return fail(sender, "Could not send you to '" + name + "'; is it loaded?");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int load(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "world");

        ManagedWorld world = this.registry.getManagedWorld(name);
        if (world == null) {
            return fail(sender, "'" + name + "' is not a managed world.");
        }

        return this.report(sender, this.worlds.load(world), "Loaded '" + name + "'.");
    }

    private int unload(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "world");

        return this.report(sender, this.worlds.unload(name), "Unloaded '" + name + "'.");
    }

    private int create(CommandContext<CommandSourceStack> context, World.Environment environment) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "name");
        String clusterId = StringArgumentType.getString(context, "cluster");

        ManagedWorld world = new ManagedWorld(
                name, environment, WorldType.NORMAL, null, null,
                WorldRole.fromEnvironment(environment),
                DisplayItem.fromConfig(null), true);

        WorldManager.Result result = this.worlds.create(world, clusterId);
        if (result.success()) {
            this.plugin.saveClusters();
        }

        return this.report(sender, result, "Created '" + name + "' in cluster '" + clusterId + "'.");
    }

    private int createWithEnvironment(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, "environment");

        World.Environment environment;
        try {
            environment = World.Environment.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fail(context.getSource().getSender(), "Unknown environment: '" + raw + "'.");
        }

        return this.create(context, environment);
    }

    private int importWorld(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "name");
        String clusterId = StringArgumentType.getString(context, "cluster");

        World existing = Bukkit.getWorld(name);
        World.Environment environment = existing == null ? World.Environment.NORMAL : existing.getEnvironment();

        ManagedWorld world = new ManagedWorld(
                name, environment, WorldType.NORMAL, null, null,
                WorldRole.fromEnvironment(environment),
                DisplayItem.fromConfig(null), true);

        WorldManager.Result result = this.worlds.importWorld(world, clusterId);
        if (result.success()) {
            this.plugin.saveClusters();
        }

        return this.report(sender, result, "Imported '" + name + "' into cluster '" + clusterId + "'.");
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "world");

        if (!(context.getSource().getExecutor() instanceof Player player)) {
            return fail(sender, "Deleting a world has to be confirmed, so it must be run by a player.");
        }

        this.confirmations.request(player,
                Component.text("permanently delete the world '" + name + "' and its folder",
                        NamedTextColor.RED),
                () -> {
                    WorldManager.Result result = this.worlds.delete(name);
                    this.plugin.saveClusters();
                    this.report(player, result, "Deleted '" + name + "'.");
                });

        return Command.SINGLE_SUCCESS;
    }

    private int createCluster(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, "id");

        if (this.registry.getClusterById(id) != null) {
            return fail(sender, "Cluster '" + id + "' already exists.");
        }

        this.registry.createCluster(id);
        this.plugin.saveClusters();

        sender.sendMessage(Component.text("Created cluster '" + id + "'.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int deleteCluster(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, "id");

        if (ClusterRegistry.DEFAULT_ID.equals(id)) {
            return fail(sender, "The default cluster cannot be deleted.");
        }

        Cluster cluster = this.registry.getClusterById(id);
        if (cluster == null) {
            return fail(sender, "No such cluster: '" + id + "'.");
        }
        if (!(context.getSource().getExecutor() instanceof Player player)) {
            return fail(sender, "Deleting a cluster has to be confirmed, so it must be run by a player.");
        }

        int worldCount = cluster.getWorlds().size();

        this.confirmations.request(player,
                Component.text("delete the cluster '" + id + "', unregistering its "
                        + worldCount + " world(s)", NamedTextColor.RED),
                () -> {
                    this.registry.deleteCluster(id);
                    this.plugin.saveClusters();

                    // Stored profiles for the deleted cluster are left alone on
                    // purpose: recreating the cluster with the same id restores
                    // everyone's state rather than silently discarding it.
                    player.sendMessage(Component.text(
                            "Deleted cluster '" + id + "'. Its worlds are now unregistered.",
                            NamedTextColor.GREEN));
                });

        return Command.SINGLE_SUCCESS;
    }

    private int assign(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String worldName = StringArgumentType.getString(context, "world");
        String clusterId = StringArgumentType.getString(context, "cluster");

        ManagedWorld world = this.registry.getManagedWorld(worldName);
        if (world == null) {
            return fail(sender, "'" + worldName + "' is not a managed world.");
        }
        if (this.registry.getClusterById(clusterId) == null) {
            return fail(sender, "No such cluster: '" + clusterId + "'.");
        }

        // Anyone standing in the world is about to have its boundary change under
        // them; move them out so the swap happens as a normal crossing.
        if (world.isLoaded()) {
            this.worlds.evacuate(world.getWorld());
        }

        this.registry.assign(world, clusterId);
        this.plugin.saveClusters();

        sender.sendMessage(Component.text(
                "Moved '" + worldName + "' into cluster '" + clusterId + "'.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        this.plugin.reloadClusters();

        context.getSource().getSender().sendMessage(
                Component.text("Reloaded world-clusters config.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private int report(CommandSender sender, WorldManager.Result result, String successMessage) {
        sender.sendMessage(result.success()
                ? Component.text(successMessage, NamedTextColor.GREEN)
                : Component.text(result.message(), NamedTextColor.RED));

        return Command.SINGLE_SUCCESS;
    }

    private static int fail(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestWorlds(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        for (ManagedWorld world : this.registry.getManagedWorlds()) {
            if (world.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(world.getName());
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestClusters(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        for (Cluster cluster : this.registry.getClusters()) {
            if (cluster.getId().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(cluster.getId());
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestEnvironments(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        for (World.Environment environment : List.of(
                World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END)) {
            String name = environment.name().toLowerCase();
            if (name.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }
}
