package com.mrjoshuasperry.mcutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Proves the test harness itself works: MockBukkit boots a mock server on this
 * toolchain and tears it down cleanly. If this fails, nothing else in the suite
 * can run — start debugging the harness here, not in a feature test.
 */
class MockBukkitSmokeTest {
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
  void bootsAndAddsPlayer() {
    assertNotNull(server, "MockBukkit.mock() should return a server");

    var player = server.addPlayer();

    assertNotNull(player, "a mock player should be created");
    assertEquals(1, server.getOnlinePlayers().size());
  }
}
