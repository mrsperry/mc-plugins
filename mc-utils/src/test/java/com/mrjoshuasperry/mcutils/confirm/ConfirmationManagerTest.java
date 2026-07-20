package com.mrjoshuasperry.mcutils.confirm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import net.kyori.adventure.text.Component;

/**
 * The two behaviours that make confirmations safe: a newer request kills the
 * older one immediately, and a stale one can't be confirmed by accident.
 */
class ConfirmationManagerTest {
    private ServerMock server;
    private PluginMock plugin;

    /** Test clock, advanced by hand so expiry doesn't need real waiting. */
    private long now;
    private ConfirmationManager confirmations;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.now = 1_000L;
        this.confirmations = new ConfirmationManager(this.plugin, "confirm", "cancel", () -> this.now);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static Component description() {
        return Component.text("do the thing");
    }

    @Test
    void confirmingRunsTheAction() {
        Player player = this.server.addPlayer();
        AtomicInteger ran = new AtomicInteger();

        this.confirmations.request(player, description(), ran::incrementAndGet);

        assertTrue(this.confirmations.confirm(player));
        assertEquals(1, ran.get());
    }

    @Test
    void anActionRunsOnlyOnce() {
        Player player = this.server.addPlayer();
        AtomicInteger ran = new AtomicInteger();

        this.confirmations.request(player, description(), ran::incrementAndGet);
        this.confirmations.confirm(player);
        this.confirmations.confirm(player);

        assertEquals(1, ran.get(), "a second /confirm must not re-run the action");
    }

    @Test
    void aSecondRequestInvalidatesTheFirst() {
        Player player = this.server.addPlayer();
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();

        this.confirmations.request(player, description(), first::incrementAndGet);
        this.confirmations.request(player, description(), second::incrementAndGet);

        assertTrue(this.confirmations.confirm(player));
        assertEquals(0, first.get(), "/confirm must never apply to a superseded action");
        assertEquals(1, second.get());
    }

    @Test
    void supersedingFiresTheExpiryHandler() {
        Player player = this.server.addPlayer();
        AtomicInteger expired = new AtomicInteger();

        this.confirmations.request(player, description(), () -> {
        }, expired::incrementAndGet);
        this.confirmations.request(player, description(), () -> {
        });

        assertEquals(1, expired.get());
    }

    @Test
    void aStaleConfirmationCannotBeConfirmed() {
        Player player = this.server.addPlayer();
        AtomicInteger ran = new AtomicInteger();

        this.confirmations.setTimeoutSeconds(30);
        this.confirmations.request(player, description(), ran::incrementAndGet);

        this.now += 31_000L;

        assertFalse(this.confirmations.confirm(player));
        assertEquals(0, ran.get());
    }

    @Test
    void sweepingDropsStaleConfirmationsAndFiresTheirExpiryHandler() {
        Player player = this.server.addPlayer();
        AtomicInteger expired = new AtomicInteger();

        this.confirmations.setTimeoutSeconds(30);
        this.confirmations.request(player, description(), () -> {
        }, expired::incrementAndGet);

        this.now += 31_000L;
        this.confirmations.sweep(this.now);

        assertEquals(1, expired.get());
        assertFalse(this.confirmations.hasPending(player));
    }

    @Test
    void aFreshConfirmationSurvivesASweep() {
        Player player = this.server.addPlayer();

        this.confirmations.setTimeoutSeconds(30);
        this.confirmations.request(player, description(), () -> {
        });

        this.now += 5_000L;
        this.confirmations.sweep(this.now);

        assertTrue(this.confirmations.hasPending(player));
    }

    @Test
    void cancellingDropsTheActionWithoutRunningIt() {
        Player player = this.server.addPlayer();
        AtomicInteger ran = new AtomicInteger();

        this.confirmations.request(player, description(), ran::incrementAndGet);

        assertTrue(this.confirmations.cancel(player));
        assertFalse(this.confirmations.confirm(player));
        assertEquals(0, ran.get());
    }

    @Test
    void confirmationsAreScopedToOnePlayer() {
        Player first = this.server.addPlayer();
        Player second = this.server.addPlayer();
        AtomicInteger ran = new AtomicInteger();

        this.confirmations.request(first, description(), ran::incrementAndGet);

        assertFalse(this.confirmations.confirm(second),
                "one player's request must not be confirmable by another");
        assertTrue(this.confirmations.hasPending(first));
        assertEquals(0, ran.get());
    }

    @Test
    void aThrowingActionIsReportedRatherThanPropagated() {
        Player player = this.server.addPlayer();

        this.confirmations.request(player, description(), () -> {
            throw new IllegalStateException("boom");
        });

        assertFalse(this.confirmations.confirm(player), "a failed action should report, not throw");
    }
}
