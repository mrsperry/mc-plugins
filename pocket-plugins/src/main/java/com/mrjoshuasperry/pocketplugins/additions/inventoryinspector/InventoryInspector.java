package com.mrjoshuasperry.pocketplugins.additions.inventoryinspector;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionType;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.builders.PotionBuilder;

public class InventoryInspector extends Module implements Listener {
    private final ArrayList<Inventory> inventories = new ArrayList<>();

    public InventoryInspector() {
        super("InventoryInspector");
    }

    @EventHandler
    private void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        final Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        final Entity entity = event.getRightClicked();
        final Player clicked;
        if (entity instanceof Player) {
            clicked = (Player) entity;
        } else {
            return;
        }

        final GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            player.openInventory(this.createInventory(clicked));
        }
    }

    private Inventory createInventory(final Player player) {
        final Inventory result = Bukkit.createInventory(null, 54, player.getName());

        // Set health, hunger, and active effects
        result.setItem(0, new ItemBuilder(Material.GLISTERING_MELON_SLICE)
                .setName(ChatColor.RED + "Health")
                .setAmount((int) (player.getHealth()))
                .build());
        result.setItem(1, new ItemBuilder(Material.COOKED_BEEF)
                .setName(ChatColor.YELLOW + "Hunger")
                .setAmount(player.getFoodLevel())
                .build());
        result.setItem(2, new PotionBuilder()
                .setName(ChatColor.LIGHT_PURPLE + "Effects")
                .setBase(PotionType.MUNDANE)
                .setEffects(new ArrayList<>(player.getActivePotionEffects()))
                .build());

        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] armor = inventory.getArmorContents();
        final ItemStack[] contents = inventory.getStorageContents();

        // Set armor contents
        for (int index = 0; index < armor.length; index++) {
            result.setItem((9 - armor.length) + index, armor[index]);
        }

        // Set hotbar contents
        for (int index = 0; index < 9; index++) {
            result.setItem(45 + index, contents[index]);
        }

        // Set the rest of the inventory contents
        for (int index = 9; index < contents.length; index++) {
            result.setItem(18 + (index - 9), contents[index]);
        }

        this.inventories.add(result);
        return result;
    }

    @EventHandler
    private void onInventoryDrag(final InventoryDragEvent event) {
        if (this.inventories.contains(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInventoryClick(final InventoryClickEvent event) {
        final boolean regular = this.inventories.contains(event.getWhoClicked().getOpenInventory().getTopInventory());
        final boolean clicked = this.inventories.contains(event.getClickedInventory());

        final InventoryAction action = event.getAction();
        if (clicked && action != InventoryAction.CLONE_STACK) {
            event.setCancelled(true);
        }

        if (regular) {
            switch (action) {
                case COLLECT_TO_CURSOR:
                case MOVE_TO_OTHER_INVENTORY:
                    event.setCancelled(true);
                    break;
            }
        }
    }

    @EventHandler
    private void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        if (this.inventories.contains(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInventoryClose(final InventoryCloseEvent event) {
        this.inventories.removeIf(inventory -> inventory == event.getInventory());
    }
}
