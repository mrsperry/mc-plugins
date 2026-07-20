package com.mrjoshuasperry.mcutils.confirm;

import net.kyori.adventure.text.Component;

/**
 * A confirmable action awaiting a player's {@code /confirm}.
 *
 * @param description what the player is being asked to confirm, used in the
 *                    prompt
 * @param action      run on confirm
 * @param onExpire    run if the confirmation goes stale or is superseded, or
 *                    null
 * @param expiresAt   epoch millis after which this is stale
 */
public record PendingConfirmation(
        Component description,
        Runnable action,
        Runnable onExpire,
        long expiresAt) {

    public boolean isExpired(long now) {
        return now >= this.expiresAt;
    }
}
