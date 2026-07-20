package com.mrjoshuasperry.mcutils.confirm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.mojang.brigadier.Command;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Gates destructive actions behind a {@code /confirm}.
 *
 * <p>
 * A player has at most <b>one</b> pending confirmation at a time. Requesting a
 * second one immediately invalidates the first, so {@code /confirm} can never
 * apply to an action the player has stopped thinking about. Confirmations also
 * go stale on their own after {@link #getTimeoutSeconds()}.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * confirmations.request(player, Component.text("delete the world 'sandbox'"),
 *         () -&gt; worldManager.delete("sandbox"));
 * </pre>
 *
 * <p>
 * <b>One per server.</b> mc-utils is shaded per plugin, so two plugins each
 * constructing a manager would both try to register {@code /confirm}. Either
 * share one manager, or give each a distinct command name via the constructor.
 */
public class ConfirmationManager {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final long SWEEP_INTERVAL_TICKS = 20L;

    private final JavaPlugin plugin;
    private final String confirmCommand;
    private final String cancelCommand;
    private final Map<UUID, PendingConfirmation> pending = new HashMap<>();

    /** Injectable so tests can advance time without sleeping. */
    private final LongSupplier clock;

    private final BukkitTask sweepTask;

    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    public ConfirmationManager(JavaPlugin plugin) {
        this(plugin, "confirm", "cancel", System::currentTimeMillis);
    }

    public ConfirmationManager(JavaPlugin plugin, String confirmCommand, String cancelCommand, LongSupplier clock) {
        this.plugin = plugin;
        this.confirmCommand = confirmCommand;
        this.cancelCommand = cancelCommand;
        this.clock = clock;

        this.sweepTask = Bukkit.getScheduler().runTaskTimer(
                plugin, () -> this.sweep(this.clock.getAsLong()), SWEEP_INTERVAL_TICKS, SWEEP_INTERVAL_TICKS);
    }

    /**
     * Registers {@code /confirm} and {@code /cancel}. Separate from the
     * constructor so the manager can be used — and tested — without a command
     * registrar.
     */
    public void registerCommands() {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                (ReloadableRegistrarEvent<Commands> event) -> {
                    event.registrar().register(Commands
                            .literal(this.confirmCommand)
                            .requires(source -> source.getExecutor() instanceof Player)
                            .executes(context -> {
                                this.confirm((Player) context.getSource().getExecutor());
                                return Command.SINGLE_SUCCESS;
                            })
                            .build(), "Confirms the last action that asked for confirmation");

                    event.registrar().register(Commands
                            .literal(this.cancelCommand)
                            .requires(source -> source.getExecutor() instanceof Player)
                            .executes(context -> {
                                this.cancel((Player) context.getSource().getExecutor());
                                return Command.SINGLE_SUCCESS;
                            })
                            .build(), "Cancels the last action that asked for confirmation");
                });
    }

    /**
     * Asks a player to confirm an action, superseding whatever they had pending.
     *
     * @param player      who to ask
     * @param description the action, phrased to follow "Really" — for example
     *                    "delete the world 'sandbox'"
     * @param action      run if they confirm
     */
    public void request(Player player, Component description, Runnable action) {
        this.request(player, description, action, null);
    }

    public void request(Player player, Component description, Runnable action, Runnable onExpire) {
        // Supersede first so the player is told why their old prompt died before
        // being shown the new one.
        this.invalidate(player.getUniqueId(), Component.text("Superseded by a newer action.", NamedTextColor.GRAY));

        this.pending.put(player.getUniqueId(), new PendingConfirmation(
                description, action, onExpire, this.clock.getAsLong() + this.timeoutSeconds * 1000L));

        player.sendMessage(this.buildPrompt(description));
    }

    private Component buildPrompt(Component description) {
        Component confirm = Component.text("[Confirm]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/" + this.confirmCommand))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click, or run /" + this.confirmCommand, NamedTextColor.GRAY)));

        Component cancel = Component.text("[Cancel]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/" + this.cancelCommand))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click, or run /" + this.cancelCommand, NamedTextColor.GRAY)));

        return Component.text("Really ", NamedTextColor.YELLOW)
                .append(description)
                .append(Component.text("? ", NamedTextColor.YELLOW))
                .append(confirm)
                .append(Component.space())
                .append(cancel)
                .append(Component.text(" (" + this.timeoutSeconds + "s)", NamedTextColor.GRAY));
    }

    /** Runs a player's pending action, if it has one and it is still fresh. */
    public boolean confirm(Player player) {
        PendingConfirmation confirmation = this.pending.remove(player.getUniqueId());

        if (confirmation == null || confirmation.isExpired(this.clock.getAsLong())) {
            this.notifyExpired(confirmation);
            player.sendMessage(Component.text("You have nothing to confirm.", NamedTextColor.RED));
            return false;
        }

        try {
            confirmation.action().run();
        } catch (RuntimeException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "A confirmed action threw", ex);
            player.sendMessage(Component.text("That action failed; check the server log.", NamedTextColor.RED));
            return false;
        }

        return true;
    }

    /** Drops a player's pending action without running it. */
    public boolean cancel(Player player) {
        boolean had = this.pending.containsKey(player.getUniqueId());

        this.invalidate(player.getUniqueId(), null);
        player.sendMessage(had
                ? Component.text("Cancelled.", NamedTextColor.GRAY)
                : Component.text("You have nothing to cancel.", NamedTextColor.RED));

        return had;
    }

    public boolean hasPending(Player player) {
        PendingConfirmation confirmation = this.pending.get(player.getUniqueId());
        return confirmation != null && !confirmation.isExpired(this.clock.getAsLong());
    }

    private void invalidate(UUID uuid, Component reason) {
        PendingConfirmation confirmation = this.pending.remove(uuid);
        if (confirmation == null) {
            return;
        }

        this.notifyExpired(confirmation);

        Player player = Bukkit.getPlayer(uuid);
        if (reason != null && player != null) {
            player.sendMessage(reason);
        }
    }

    private void notifyExpired(PendingConfirmation confirmation) {
        if (confirmation == null || confirmation.onExpire() == null) {
            return;
        }

        try {
            confirmation.onExpire().run();
        } catch (RuntimeException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "A confirmation expiry handler threw", ex);
        }
    }

    /**
     * Drops every confirmation that has gone stale. Package-visible with an
     * explicit timestamp so tests can advance the clock directly.
     */
    void sweep(long now) {
        this.pending.entrySet().removeIf(entry -> {
            if (!entry.getValue().isExpired(now)) {
                return false;
            }

            this.notifyExpired(entry.getValue());

            CommandSender player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(Component.text("Your pending confirmation expired.", NamedTextColor.GRAY));
            }

            return true;
        });
    }

    /** Clears any pending confirmation for a player, e.g. on quit. */
    public void forget(Player player) {
        this.pending.remove(player.getUniqueId());
    }

    public int getTimeoutSeconds() {
        return this.timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Stops the sweep task. Call from {@code onDisable}. */
    public void shutdown() {
        this.sweepTask.cancel();
    }
}
