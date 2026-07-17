package com.mrjoshuasperry.pocketplugins.modules.commandmacros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;

class MacrosCommandTest {
  private ServerMock server;
  private MacrosCommand command;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    Map<String, MacroData> macros = Map.of(
        "public", new MacroData(false, List.of("say hi")),
        "secret", new MacroData(true, List.of("op stuff")));
    command = new MacrosCommand(macros);
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private List<String> suggestionsFor(Player player, String remaining) {
    SuggestionsBuilder builder = new SuggestionsBuilder(remaining, 0);
    return command.suggestMacroNames(player, builder).join().getList().stream()
        .map(suggestion -> suggestion.getText())
        .toList();
  }

  @Test
  void suggestionsHideOpOnlyMacrosFromNonOps() {
    Player player = server.addPlayer();
    player.setOp(false);

    List<String> suggestions = suggestionsFor(player, "");

    assertTrue(suggestions.contains("public"));
    assertFalse(suggestions.contains("secret"));
  }

  @Test
  void suggestionsIncludeOpOnlyMacrosForOps() {
    Player player = server.addPlayer();
    player.setOp(true);

    List<String> suggestions = suggestionsFor(player, "");

    assertTrue(suggestions.contains("public"));
    assertTrue(suggestions.contains("secret"));
  }

  @Test
  void suggestionsNarrowByTypedPrefix() {
    Player player = server.addPlayer();
    player.setOp(true);

    assertEquals(List.of("public"), suggestionsFor(player, "pu"));
  }

  @Test
  void runMacroReportsUnknownMacro() {
    PlayerMock player = server.addPlayer();

    command.runMacro(player, "does-not-exist");

    assertTrue(player.nextMessage().contains("doesn't exist"));
  }

  @Test
  void runMacroBlocksOpOnlyForNonOpsAndMatchesCaseInsensitively() {
    PlayerMock player = server.addPlayer();
    player.setOp(false);

    // "SECRET" resolves to the op-only "secret" macro; a non-op is denied rather
    // than told it does not exist, proving both the lookup and the op gate.
    command.runMacro(player, "SECRET");

    assertTrue(player.nextMessage().contains("permission"));
  }
}
