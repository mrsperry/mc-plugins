package com.mrjoshuasperry.mcutils.menu;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;

import net.kyori.adventure.text.Component;

/**
 * A chest-style inventory whose slots are backed by {@link MenuItem}s.
 *
 * <p>
 * Menus are click-guarded: {@link MenuListener} routes interactions through
 * {@link #onClick}, so the menu's own slots can never be dragged out,
 * shift-clicked into, or picked up. The player's own inventory stays fully
 * usable — only moves that would cross into the menu are blocked.
 *
 * <p>
 * <b>Construct one menu per player.</b> {@link MenuItem}s carry mutable state
 * (see {@code SwappableMenuItem}'s current index) and a menu owns a single item
 * map, so sharing one instance between two viewers makes them share that state.
 */
public class Menu implements InventoryHolder {
    protected Inventory inventory;
    protected Map<Integer, MenuItem> items;

    public Menu(Component title, int slots) {
        this(title, slots, new HashMap<>());
    }

    public Menu(Component title, int slots, Map<Integer, MenuItem> items) {
        this.inventory = Bukkit.createInventory(this, slots, title);
        this.items = items;

        this.repopulateInventory();
    }

    public void repopulateInventory() {
        for (Map.Entry<Integer, MenuItem> entry : this.items.entrySet()) {
            ItemStack item = entry.getValue().getItem();
            // getItem returns null for an empty slot, which is every slot the first
            // time the constructor runs this, so compare null-safely rather than
            // dereferencing the current contents.
            if (!Objects.equals(this.inventory.getItem(entry.getKey()), item)) {
                this.inventory.setItem(entry.getKey(), item);
            }
        }
    }

    public void fillInventory(MenuItem item) {
        for (int index = 0; index < this.inventory.getSize(); index++) {
            if (this.inventory.getItem(index) == null) {
                this.items.put(index, item);
                this.inventory.setItem(index, item.getItem());
            }
        }
    }

    public void setItem(int slot, MenuItem item) {
        this.items.put(slot, item);
        this.inventory.setItem(slot, item.getItem());
    }

    public MenuItem getItem(int slot) {
        return this.items.get(slot);
    }

    public void clearItem(int slot) {
        this.items.remove(slot);
        this.inventory.setItem(slot, null);
    }

    public void clearItems(int minSlot, int maxSlot) {
        for (int index = minSlot; index <= maxSlot; index++) {
            this.clearItem(index);
        }
    }

    public void clickedSlot(Player player, int slot) {
        this.clickedSlot(player, slot, ClickType.UNKNOWN);
    }

    public void clickedSlot(Player player, int slot, ClickType click) {
        if (this.items.containsKey(slot)) {
            this.items.get(slot).onClick(player, this, click);
        }

        this.repopulateInventory();
    }

    /**
     * Handles a click anywhere while this menu is open.
     *
     * <p>
     * Returns whether the event should be cancelled. The default implementation
     * dispatches clicks on this menu's own slots to the corresponding
     * {@link MenuItem} and always guards them, but leaves the player's own
     * inventory fully usable — only blocking the actions that would reach across
     * into the menu (a shift-click move or a double-click collect). Override to
     * change how the menu's own slots behave.
     *
     * @param event the click
     * @return true if the click should be cancelled
     */
    public boolean onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return true;
        }

        // A click in the player's own inventory reports the menu as the top
        // inventory, so compare against the clicked inventory rather than the raw
        // slot. A click on the menu's own slots is dispatched to that slot's item
        // and always cancelled, so nothing is taken unless the item chooses to act.
        if (Objects.equals(event.getClickedInventory(), this.inventory)) {
            this.clickedSlot(player, event.getSlot(), event.getClick());
            return true;
        }

        // Otherwise the click is in the player's own inventory (or outside), which
        // is theirs to use freely — cancel only the actions that would move items
        // across into the menu.
        return switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> true;
            default -> false;
        };
    }

    /** Called when a player opens this menu. */
    public void onOpen(Player player) {
        // No-op by default.
    }

    /** Called when a player closes this menu. */
    public void onClose(Player player) {
        // No-op by default.
    }

    /**
     * Advances any time-driven items and pushes their new state into the
     * inventory. Driven by {@link MenuManager} while the menu is open.
     */
    public void tick() {
        // Distinct instances, not slots: one MenuItem can fill many slots (a
        // border filler occupies dozens), and ticking it once per slot would run
        // its timer that many times faster.
        Set<MenuItem> ticked = Collections.newSetFromMap(new IdentityHashMap<>());
        ticked.addAll(this.items.values());

        for (MenuItem item : ticked) {
            item.tick();
        }

        this.repopulateInventory();
    }

    /** Opens this menu for a player. */
    public void open(Player player) {
        player.openInventory(this.inventory);
    }

    public int getSize() {
        return this.inventory.getSize();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
