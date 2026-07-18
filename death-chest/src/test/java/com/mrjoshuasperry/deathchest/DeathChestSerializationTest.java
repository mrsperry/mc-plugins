package com.mrjoshuasperry.deathchest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Covers the death-chest storage format and its migration. The format-detection
 * checks are pure byte inspection; the round-trip and legacy-read checks need a mock
 * server so real ItemStacks can be (de)serialized. The legacy test writes a payload
 * exactly the way the pre-migration code did so the reader is exercised against the
 * genuine old format, not a stand-in.
 */
class DeathChestSerializationTest {
  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void detectsLegacyStreamMagic() {
    // 0xACED 0x0005 is the header ObjectOutputStream always emits.
    assertTrue(DeathChest.isLegacyFormat(new byte[] { (byte) 0xAC, (byte) 0xED, 0x00, 0x05 }));
  }

  @Test
  void currentFormatIsNotDetectedAsLegacy() {
    assertFalse(DeathChest.isLegacyFormat(new byte[] { 1, 0, 0, 0, 0 }));
  }

  @Test
  void tooShortToBeLegacy() {
    assertFalse(DeathChest.isLegacyFormat(new byte[] { (byte) 0xAC }));
  }

  @Test
  void currentFormatRoundTripsItemsAndSlots() throws IOException, ClassNotFoundException {
    Map<Integer, ItemStack> items = new HashMap<>();
    items.put(3, new ItemStack(Material.DIAMOND, 5));
    items.put(40, new ItemStack(Material.OAK_LOG, 2));

    byte[] data = DeathChest.serializeItems(items);

    assertFalse(DeathChest.isLegacyFormat(data), "current format must not look legacy");
    assertEquals(items, DeathChest.deserializeItems(data));
  }

  @Test
  void emptyInventorySerializesToReadableData() throws IOException, ClassNotFoundException {
    byte[] data = DeathChest.serializeItems(new HashMap<>());

    assertEquals(new HashMap<>(), DeathChest.deserializeItems(data));
  }

  @Test
  void missingDataDeserializesToEmpty() throws IOException, ClassNotFoundException {
    assertEquals(new HashMap<>(), DeathChest.deserializeItems(null));
    assertEquals(new HashMap<>(), DeathChest.deserializeItems(new byte[0]));
  }

  @Test
  void readsChestsWrittenInThePreMigrationFormat() throws IOException, ClassNotFoundException {
    Map<Integer, ItemStack> items = new HashMap<>();
    items.put(3, new ItemStack(Material.DIAMOND, 5));
    items.put(40, new ItemStack(Material.OAK_LOG, 2));

    byte[] legacy = writeLegacyFormat(items);

    assertTrue(DeathChest.isLegacyFormat(legacy), "fixture should be in the old format");
    assertEquals(items, DeathChest.deserializeItems(legacy));
  }

  /** Reproduces exactly how the pre-migration code serialized chest contents. */
  @SuppressWarnings("deprecation") // deliberately uses the old serializer to produce a genuine legacy fixture
  private static byte[] writeLegacyFormat(Map<Integer, ItemStack> items) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
      dataOutput.writeInt(items.size());
      for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
        dataOutput.writeInt(entry.getKey());
        dataOutput.writeObject(entry.getValue());
      }
    }

    return outputStream.toByteArray();
  }
}
