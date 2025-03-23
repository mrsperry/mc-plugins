package com.mrjoshuasperry.deathchest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import org.bukkit.util.io.BukkitObjectOutputStream;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DeathChest {
    private static int getItemIndexForwardOffset(int index) {
        int adjustedIndex = index;
        // This shifts the hotbar items down 3 rows, to allow for a more natural look
        if (index < 9) {
            adjustedIndex += 27;
        }

        // Maintain position of armor and offhand items, while shifting all other items
        // up one row
        if (index > 9 && index < 36) {
            adjustedIndex -= 9;
        }

        return adjustedIndex;
    }

    private static int getItemIndexBackwardOffset(int index) {
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

    public static void updateChestContents(Main plugin, Chest chest, Inventory inventory, boolean updateIndex) {
        PersistentDataContainer container = chest.getPersistentDataContainer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            Map<Integer, ItemStack> items = new HashMap<>();
            for (int index = 0; index < inventory.getSize(); index++) {
                ItemStack item = inventory.getItem(index);
                if (item == null) {
                    continue;
                }

                int adjustedIndex = updateIndex ? DeathChest.getItemIndexForwardOffset(index) : index;
                items.put(adjustedIndex, item);
            }

            dataOutput.writeInt(items.entrySet().size());
            for (Entry<Integer, ItemStack> entry : items.entrySet()) {
                dataOutput.writeInt(entry.getKey());
                dataOutput.writeObject(entry.getValue());
            }

            byte[] byteArray = outputStream.toByteArray();
            container.set(plugin.getDeathChestItemsKey(), PersistentDataType.BYTE_ARRAY, byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

        byte[] itemBytes = chest.getPersistentDataContainer().get(
                plugin.getDeathChestItemsKey(),
                PersistentDataType.BYTE_ARRAY);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(itemBytes);

        try (BukkitObjectInputStream input = new BukkitObjectInputStream(inputStream)) {
            int inventorySize = input.readInt();

            for (int index = 0; index < inventorySize; index++) {
                int slot = input.readInt();
                ItemStack item = (ItemStack) input.readObject();

                inventory.setItem(slot, item);
            }
        } catch (EOFException ex) {
            // Do nothing
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
