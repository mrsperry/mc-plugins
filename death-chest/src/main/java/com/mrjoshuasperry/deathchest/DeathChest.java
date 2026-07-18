package com.mrjoshuasperry.deathchest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DeathChest {
    /**
     * Version marker for the current on-disk item format. Contents are stored in the
     * chest's PDC as a byte array; version 1 serializes each item with
     * {@link ItemStack#serializeAsBytes()}, which tags the Minecraft data version and
     * therefore survives game upgrades. Chests written before this migration used Java
     * serialization ({@code BukkitObjectOutputStream#writeObject}); those payloads
     * always begin with the {@code ObjectOutputStream} magic {@code 0xACED}, which lets
     * the reader detect them and still load pre-migration chests. Must never be
     * {@code 0xAC}, or the two formats would be indistinguishable.
     */
    private static final byte FORMAT_VERSION = 1;

    static int getItemIndexForwardOffset(int index) {
        int adjustedIndex = index;
        // This shifts the hotbar items down 3 rows, to allow for a more natural look
        if (index < 9) {
            adjustedIndex += 27;
        }

        // Shift the main-inventory rows (9-35) up one row. This must include slot 9:
        // with `> 9` it was skipped, so slots 9 and 18 both mapped to display slot 9
        // and collided in the storage map, silently dropping one item.
        if (index >= 9 && index < 36) {
            adjustedIndex -= 9;
        }

        return adjustedIndex;
    }

    static int getItemIndexBackwardOffset(int index) {
        int adjustedIndex = index;
        // This shifts the hotbar items back to their original position
        if (index < 27) {
            adjustedIndex += 9;
        }

        // This shifts the rest of the main inventory items back to their original
        // position
        if (index > 26 && index < 36) {
            adjustedIndex -= 27;
        }

        return adjustedIndex;
    }

    /**
     * Serializes a slot-to-item map in the current format: a version byte, the entry
     * count, then each entry as its slot, the item's serialized length, and its
     * {@link ItemStack#serializeAsBytes()} payload. Package-private and static so the
     * round trip is unit-testable without a live chest.
     */
    static byte[] serializeItems(Map<Integer, ItemStack> items) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutput = new DataOutputStream(outputStream)) {
            dataOutput.writeByte(FORMAT_VERSION);
            dataOutput.writeInt(items.size());
            for (Entry<Integer, ItemStack> entry : items.entrySet()) {
                dataOutput.writeInt(entry.getKey());

                byte[] itemBytes = entry.getValue().serializeAsBytes();
                dataOutput.writeInt(itemBytes.length);
                dataOutput.write(itemBytes);
            }
        }

        return outputStream.toByteArray();
    }

    /**
     * Reads a slot-to-item map from either the current format or the pre-migration
     * Java-serialized one, dispatching on {@link #isLegacyFormat(byte[])}. Returns an
     * empty map for missing/empty data. Package-private and static so both formats are
     * unit-testable without a live chest.
     */
    static Map<Integer, ItemStack> deserializeItems(byte[] data) throws IOException, ClassNotFoundException {
        Map<Integer, ItemStack> items = new HashMap<>();
        if (data == null || data.length == 0) {
            return items;
        }

        if (isLegacyFormat(data)) {
            readLegacyItems(items, data);
        } else {
            readCurrentItems(items, data);
        }

        return items;
    }

    private static void readCurrentItems(Map<Integer, ItemStack> items, byte[] data) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            input.readByte(); // format version; only version 1 exists today
            int count = input.readInt();

            for (int index = 0; index < count; index++) {
                int slot = input.readInt();

                byte[] itemBytes = new byte[input.readInt()];
                input.readFully(itemBytes);
                items.put(slot, ItemStack.deserializeBytes(itemBytes));
            }
        }
    }

    // BukkitObjectInputStream is deprecated, but it is the only reader for the old
    // format, so this shim is required until every pre-migration chest has been reopened.
    @SuppressWarnings("deprecation")
    private static void readLegacyItems(Map<Integer, ItemStack> items, byte[] data)
            throws IOException, ClassNotFoundException {
        try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(data))) {
            int count = input.readInt();

            for (int index = 0; index < count; index++) {
                int slot = input.readInt();
                items.put(slot, (ItemStack) input.readObject());
            }
        } catch (EOFException ex) {
            // Pre-migration chests occasionally ended before their declared count;
            // keep whatever was read, matching the original loader's behavior.
        }
    }

    /**
     * Whether the payload was written by the pre-migration Java serializer.
     * {@code ObjectOutputStream} (and thus {@code BukkitObjectOutputStream}) always
     * opens its stream with the 2-byte magic {@code 0xACED}; the current format opens
     * with {@link #FORMAT_VERSION}, so this reliably tells the two apart.
     */
    static boolean isLegacyFormat(byte[] data) {
        return data.length >= 2 && (data[0] & 0xFF) == 0xAC && (data[1] & 0xFF) == 0xED;
    }

    public static void updateChestContents(Main plugin, Chest chest, Inventory inventory, boolean updateIndex) {
        Map<Integer, ItemStack> items = new HashMap<>();
        for (int index = 0; index < inventory.getSize(); index++) {
            ItemStack item = inventory.getItem(index);
            if (item == null) {
                continue;
            }

            int adjustedIndex = updateIndex ? DeathChest.getItemIndexForwardOffset(index) : index;
            items.put(adjustedIndex, item);
        }

        try {
            byte[] data = DeathChest.serializeItems(items);
            chest.getPersistentDataContainer().set(plugin.getDeathChestItemsKey(), PersistentDataType.BYTE_ARRAY, data);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save death chest contents", e);
        }

        chest.update();
    }

    public static void create(Main plugin, Player player) {
        Location location = player.getLocation();
        World world = player.getWorld();
        Block block = world.getBlockAt(location);

        Inventory inventory = player.getInventory();
        if (inventory.isEmpty()) {
            return;
        }

        player.sendMessage(
                Component
                        .text().color(NamedTextColor.RED).content("Your death chest is located at ("
                                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()
                                + ")")
                        .build());

        block.setType(Material.CHEST);

        Chest chest = (Chest) block.getState();

        PersistentDataContainer container = chest.getPersistentDataContainer();
        container.set(plugin.getDeathChestKey(), PersistentDataType.BOOLEAN, true);
        container.set(plugin.getDeathChestPlayerKey(), PersistentDataType.STRING, player.getName());

        DeathChest.updateChestContents(plugin, chest, inventory, true);
    }

    public static void destroy(Chest chest) {
        World world = chest.getWorld();
        Location location = chest.getLocation();
        world.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        for (ItemStack item : chest.getInventory().getContents()) {
            if (item == null) {
                continue;
            }

            world.dropItemNaturally(location, item);
        }

        chest.getBlock().setType(Material.AIR);
    }

    private static Inventory readInventory(Main plugin, Chest chest) {
        PersistentDataContainer container = chest.getPersistentDataContainer();

        String playerName = container.get(plugin.getDeathChestPlayerKey(), PersistentDataType.STRING);
        Inventory inventory = new DeathChestInventory(plugin, chest, 45, Component.text(playerName + "'s Death Chest"))
                .getInventory();

        byte[] itemBytes = container.get(plugin.getDeathChestItemsKey(), PersistentDataType.BYTE_ARRAY);
        try {
            for (Entry<Integer, ItemStack> entry : DeathChest.deserializeItems(itemBytes).entrySet()) {
                inventory.setItem(entry.getKey(), entry.getValue());
            }
        } catch (ClassNotFoundException | IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load death chest contents", ex);
        }

        return inventory;
    }

    public static void takeItems(Main plugin, Player player, Chest chest) {
        Inventory inventory = DeathChest.readInventory(plugin, chest);
        Inventory playerInventory = player.getInventory();

        for (int index = 0; index < inventory.getSize(); index++) {
            if (playerInventory.firstEmpty() == -1) {
                break;
            }

            ItemStack item = inventory.getItem(index);
            if (item == null) {
                continue;
            }

            int adjustedIndex = DeathChest.getItemIndexBackwardOffset(index);
            ItemStack currentItem = playerInventory.getItem(adjustedIndex);
            if (currentItem == null) {
                playerInventory.setItem(adjustedIndex, item);
            } else {
                playerInventory.addItem(item);
            }

            inventory.clear(index);
        }

        if (inventory.isEmpty()) {
            DeathChest.destroy(chest);
        } else {
            DeathChest.updateChestContents(plugin, chest, inventory, false);
        }
    }

    public static void openInventory(Main plugin, Player player, Chest chest) {
        Inventory inventory = DeathChest.readInventory(plugin, chest);
        player.openInventory(inventory);
    }
}
