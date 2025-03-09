package com.mrjoshuasperry.enhanceddungeons.dungeons.content;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

public class DungeonLoot {
    /** The location of the inventory */
    private Location location;
    /** The inventory items will be placed into */
    private Inventory inventory;
    /** The number of times the items have to beat the chance */
    private int rolls;
    /** The chance this item is places (0-100) */
    private double chance;
    /** If the items should be randomly placed around the inventory */
    private boolean spread;
    /** A list of items to place */
    private Set<ItemStack> items;

    /** Creates a new dungeon lootable */
    public DungeonLoot() {
        this.location = null;
        this.inventory = null;
        this.rolls = 1;
        this.chance = -1;
        this.spread = false;
        this.items = new HashSet<>();
    }

    /** Fills the inventory with items */
    public void spawn() {
        // Try to place each item
        for (final ItemStack item : this.items) {
            // Each item has a number of rolls to beat its chance percentage
            for (int roll = 0; roll < this.rolls; roll++) {
                final double chance = Main.getRandom().nextDouble() * 100;

                // Check if this roll beat its chance
                if (this.chance >= chance) {
                    // Randomly spread copies of the item around the inventory
                    if (this.spread) {
                        final ItemStack copy = item.clone();
                        copy.setAmount(1);

                        for (int amount = 0; amount < item.getAmount(); amount++) {
                            final int slot = this.findAvailableSlot(item);

                            if (slot == -1) {
                                Utils.log(Level.SEVERE, "Could not find slot for: " + item + " at " + this.location);
                                break;
                            }

                            final ItemStack slotItem = this.inventory.getItem(slot);
                            if (slotItem != null) {
                                slotItem.setAmount(slotItem.getAmount() + 1);
                            } else {
                                this.inventory.setItem(slot, copy);
                            }
                        }
                    } else {
                        final int slot = this.findAvailableSlot(item);

                        if (slot == -1) {
                            Utils.log(Level.SEVERE, "Could not find slot for: " + item + " at " + this.location);
                            break;
                        }

                        this.inventory.setItem(slot, item);
                    }

                    break;
                }
            }
        }
    }

    /**
     * Finds an available slot to place an item in an inventory
     * @param item The item to find a slot for
     * @return The index of the slot or -1 if it could not find any available slots
     */
    private int findAvailableSlot(final ItemStack item) {
        if (this.inventory == null) {
            Utils.log(Level.SEVERE, "Dungeon loot inventory was not found!");
            return -1;
        }

        final List<Integer> availableSlots = new ArrayList<>();

        for (int index = 0; index < this.inventory.getSize(); index++) {
            final ItemStack content = this.inventory.getItem(index);

            // Check if the slot is empty
            if (content == null || content.getType() == Material.AIR) {
                availableSlots.add(index);
                continue;
            }

            // Ensure the items are of the same type
            if (content.getType() != item.getType()) {
                continue;
            }

            // Ensure that the max stack size is respected
            if (content.getAmount() + 1 <= content.getMaxStackSize()) {
                availableSlots.add(index);
            }
        }

        if (availableSlots.size() == 0) {
            return -1;
        }

        // Get a random slot
        return availableSlots.get(Main.getRandom().nextInt(availableSlots.size()));
    }

    /** Clears the inventory */
    public void remove() {
        if (this.inventory != null) {
            this.inventory.clear();
        }
    }

    /** @param location The location of the inventory in the world */
    public void setInventory(final Location location) {
        final BlockState state = Utils.locationToBlockCoordinates(location).getBlock().getState();

        if (!(state instanceof Container)) {
            Utils.log(Level.SEVERE, "Dungeon loot location was not a container: " + location);
            return;
        }

        this.location = location;
        this.inventory = ((Container) state).getInventory();
    }

    /** @param rolls The number of times the items have to beat the chance */
    public void setRolls(final int rolls) {
        this.rolls = rolls;
    }

    /** @param chance The chance an item is places (0-100) */
    public void setChance(final double chance) {
        this.chance = chance;
    }

    /** @param spread If the items should be randomly placed around the inventory */
    public void setSpread(final boolean spread) {
        this.spread = spread;
    }

    /** @param items A list of items to be placed */
    public void setItems(final Set<ItemStack> items) {
        this.items = items;
    }
}
