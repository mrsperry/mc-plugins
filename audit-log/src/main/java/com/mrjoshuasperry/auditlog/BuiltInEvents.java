package com.mrjoshuasperry.auditlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

/**
 * The events every server wants on record, whatever plugins it runs. All of them
 * listen at MONITOR so they log the outcome other plugins settled on.
 */
final class BuiltInEvents implements Listener {
    /** The ways an item on the ground can be destroyed rather than picked up or merged. */
    private static final Set<EntityRemoveEvent.Cause> LOST = Set.of(EntityRemoveEvent.Cause.DEATH,
            EntityRemoveEvent.Cause.DESPAWN, EntityRemoveEvent.Cause.OUT_OF_WORLD, EntityRemoveEvent.Cause.DISCARD);

    private final Plugin plugin;
    private final boolean joins;
    private final boolean snapshots;
    private final boolean deaths;
    private final boolean kills;
    private final boolean commands;
    private final boolean lostItems;
    private final boolean gameModes;
    private final Set<String> auditedCommands = new HashSet<>();

    BuiltInEvents(Plugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.joins = config.getBoolean("events.joins", true);
        this.snapshots = config.getBoolean("events.inventory-snapshots", true);
        this.deaths = config.getBoolean("events.deaths", true);
        this.kills = config.getBoolean("events.kills", true);
        this.commands = config.getBoolean("events.commands", true);
        this.lostItems = config.getBoolean("events.lost-items", true);
        this.gameModes = config.getBoolean("events.game-modes", true);
        for (String command : config.getStringList("audited-commands")) {
            this.auditedCommands.add(command.toLowerCase(Locale.ROOT));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.joins) {
            this.recordPresence("join", event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.joins) {
            this.recordPresence("leave", event.getPlayer());
        }
    }

    private void recordPresence(String event, Player player) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("player", player);
        fields.put("location", player.getLocation());
        fields.put("game-mode", player.getGameMode());
        if (this.snapshots) {
            fields.put("level", player.getLevel());
            fields.put("exp", player.getExp());
            fields.put("inventory", player.getInventory().getContents());
            fields.put("ender-chest", player.getEnderChest().getContents());
        }
        AuditLog.record(this.plugin, event, fields);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!this.deaths) {
            return;
        }

        Player player = event.getPlayer();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("player", player);
        fields.put("location", player.getLocation());
        fields.put("message", event.deathMessage());
        fields.put("cause", damageCause(player));
        // A player killer gets this in their own file too, which is how PvP kills are kept
        fields.put("killer", player.getKiller());
        fields.put("keep-inventory", event.getKeepInventory());
        fields.put("level", player.getLevel());
        fields.put("dropped-exp", event.getDroppedExp());
        // The inventory is only cleared after this event, so this is what they died with
        fields.put("inventory", player.getInventory().getContents());
        fields.put("drops", new ArrayList<>(event.getDrops()));
        AuditLog.record(this.plugin, "death", fields);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Player deaths arrive here too; they have their own record above
        if (!this.kills || event instanceof PlayerDeathEvent) {
            return;
        }

        LivingEntity entity = event.getEntity();
        OfflinePlayer owner = entity instanceof Tameable tameable && tameable.getOwner() instanceof OfflinePlayer tamer
                ? tamer
                : null;
        if (owner == null && entity.customName() == null) {
            return;
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entity", entity);
        fields.put("location", entity.getLocation());
        fields.put("owner", owner);
        fields.put("cause", damageCause(entity));
        fields.put("killer", entity.getKiller());
        fields.put("drops", new ArrayList<>(event.getDrops()));
        AuditLog.record(this.plugin, "mob-death", fields);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        this.recordCommand(player, event.getMessage(), player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        // Command blocks and other senders come through here too; only people count
        if (event.getSender() instanceof ConsoleCommandSender) {
            this.recordCommand("console", event.getCommand(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        CommandSender sender = event.getSender();
        if (sender instanceof RemoteConsoleCommandSender) {
            this.recordCommand("rcon", event.getCommand(), null);
        }
    }

    private void recordCommand(Object sender, String line, Player player) {
        if (!this.commands || !this.auditedCommands.contains(commandName(line))) {
            return;
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("by", sender);
        fields.put("location", player == null ? null : player.getLocation());
        fields.put("command", line);
        // Selectors can't be resolved here, but a target named outright gets the record too
        fields.put("targets", mentionedPlayers(line, Bukkit.getOnlinePlayers()));
        AuditLog.record(this.plugin, "command", fields);
    }

    /** The command's name in lower case, without its slash or namespace. */
    static String commandName(String line) {
        String trimmed = line.strip();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        String name = trimmed.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return name.substring(name.indexOf(':') + 1);
    }

    /** Online players whose name appears as one of the command's arguments. */
    static List<Player> mentionedPlayers(String line, Collection<? extends Player> online) {
        String[] words = line.strip().split("\\s+");
        List<Player> mentioned = new ArrayList<>();
        for (Player player : online) {
            for (int index = 1; index < words.length; index++) {
                if (words[index].equalsIgnoreCase(player.getName())) {
                    mentioned.add(player);
                    break;
                }
            }
        }
        return mentioned;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!this.gameModes) {
            return;
        }

        Player player = event.getPlayer();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("player", player);
        fields.put("location", player.getLocation());
        fields.put("from", player.getGameMode());
        fields.put("to", event.getNewGameMode());
        fields.put("cause", event.getCause());
        AuditLog.record(this.plugin, "game-mode", fields);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        if (!this.lostItems || !(event.getEntity() instanceof Item item) || !LOST.contains(event.getCause())) {
            return;
        }

        // Only items a player threw; drops from mobs and farms would bury everything else
        UUID thrower = item.getThrower();
        if (thrower == null) {
            return;
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("thrower", Bukkit.getOfflinePlayer(thrower));
        fields.put("location", item.getLocation());
        fields.put("item", item.getItemStack());
        fields.put("cause", event.getCause());
        AuditLog.record(this.plugin, "item-lost", fields);
    }

    private static EntityDamageEvent.DamageCause damageCause(LivingEntity entity) {
        EntityDamageEvent lastDamage = entity.getLastDamageCause();
        return lastDamage == null ? null : lastDamage.getCause();
    }
}
