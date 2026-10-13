package com.mrjoshuasperry.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class AuditFormatTest {
  private ServerMock server;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private static JsonObject parse(Map<String, ?> fields) {
    String line = AuditFormat.line(ZonedDateTime.of(2026, 10, 12, 21, 49, 17, 123_456_789, ZoneOffset.ofHours(-4)),
        "DeathChest", "pile-created", fields);
    assertFalse(line.contains("\n"), "a record must stay on one line");
    return JsonParser.parseString(line).getAsJsonObject();
  }

  @Test
  void aLineStartsWithWhenWhoAndWhatThenTheFieldsInOrder() {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("zeta", 1);
    fields.put("alpha", "two");

    JsonObject json = parse(fields);

    assertEquals(List.of("time", "plugin", "event", "zeta", "alpha"), List.copyOf(json.keySet()));
    assertEquals("2026-10-12T21:49:17.123-04:00", json.get("time").getAsString());
    assertEquals("DeathChest", json.get("plugin").getAsString());
    assertEquals("pile-created", json.get("event").getAsString());
  }

  @Test
  void itemsAreWrittenTheWayGiveTakesThem() {
    // MockBukkit can't produce an item's component string, so the dev server checks the real thing
    assertEquals("minecraft:torch 64", AuditFormat.giveString("minecraft:torch", "[]", 64));
    assertEquals("minecraft:diamond_sword[minecraft:enchantments={sharpness:5}] 1",
        AuditFormat.giveString("minecraft:diamond_sword", "[minecraft:enchantments={sharpness:5}]", 1));
  }

  @Test
  void anInventoryKeepsItsSlotsAndSkipsEmptyOnes() {
    ItemStack[] contents = new ItemStack[41];
    contents[3] = new ItemStack(Material.TORCH, 12);
    contents[40] = new ItemStack(Material.SHIELD);
    contents[5] = new ItemStack(Material.AIR);

    JsonObject json = AuditFormat.slots(contents, item -> item.getType().name() + " " + item.getAmount());

    assertEquals(2, json.size());
    assertEquals("TORCH 12", json.get("3").getAsString());
    assertEquals("SHIELD 1", json.get("40").getAsString());
  }

  @Test
  void playersLocationsAndEnumsReadPlainly() {
    Player player = server.addPlayer("Alice");
    WorldMock world = server.addSimpleWorld("world");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("player", player);
    fields.put("location", new Location(world, 12.345, 64, -30.5));
    fields.put("game-mode", GameMode.SURVIVAL);
    fields.put("type", EntityType.WOLF);
    fields.put("killer", null);

    JsonObject json = parse(fields);

    assertEquals("Alice", json.getAsJsonObject("player").get("name").getAsString());
    assertEquals(player.getUniqueId().toString(), json.getAsJsonObject("player").get("uuid").getAsString());
    assertEquals("world", json.getAsJsonObject("location").get("world").getAsString());
    assertEquals(12.35, json.getAsJsonObject("location").get("x").getAsDouble());
    assertEquals("survival", json.get("game-mode").getAsString());
    assertEquals("minecraft:wolf", json.get("type").getAsString());
    assertTrue(json.get("killer").isJsonNull(), "a missing value is still written, as null");
  }

  @Test
  void everyPlayerInvolvedGetsTheRecordOnce() {
    Player alice = server.addPlayer("Alice");
    Player bob = server.addPlayer("Bob");
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("owner", alice);
    fields.put("by", bob);
    fields.put("targets", List.of(alice));

    assertEquals(List.of("players/Alice.jsonl", "players/Bob.jsonl"), AuditFormat.files(fields));
  }

  @Test
  void aRecordWithNoPlayerGoesToTheServerFile() {
    assertEquals(List.of("server.jsonl"), AuditFormat.files(Map.of("by", "console")));
  }
}
