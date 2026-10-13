package com.mrjoshuasperry.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class BuiltInEventsTest {
  private ServerMock server;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void aCommandIsMatchedByNameWhateverItsSlashNamespaceOrCase() {
    assertEquals("give", BuiltInEvents.commandName("/give Alice diamond 64"));
    assertEquals("give", BuiltInEvents.commandName("minecraft:give Alice diamond"));
    assertEquals("clear", BuiltInEvents.commandName("/CLEAR"));
  }

  @Test
  void playersNamedInACommandAreItsTargets() {
    Player alice = server.addPlayer("Alice");
    server.addPlayer("Bob");

    assertEquals(List.of(alice), BuiltInEvents.mentionedPlayers("/clear alice", server.getOnlinePlayers()));
    // The command's own name never counts, even if a player shares it
    assertEquals(List.of(), BuiltInEvents.mentionedPlayers("/bob give", server.getOnlinePlayers()));
  }
}
