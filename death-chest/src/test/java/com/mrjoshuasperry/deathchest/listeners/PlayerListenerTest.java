package com.mrjoshuasperry.deathchest.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

class PlayerListenerTest {
  @Test
  void survivalAndCreativeDeathsLeaveAPile() {
    assertTrue(PlayerListener.leavesAPile(GameMode.SURVIVAL));
    assertTrue(PlayerListener.leavesAPile(GameMode.CREATIVE));
  }

  @Test
  void adventureAndSpectatorDeathsDropAsVanilla() {
    assertFalse(PlayerListener.leavesAPile(GameMode.ADVENTURE));
    assertFalse(PlayerListener.leavesAPile(GameMode.SPECTATOR));
  }
}
