package com.mrjoshuasperry.mcutils.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.menu.items.DecorMenuItem;
import com.mrjoshuasperry.mcutils.menu.items.MenuItem;
import com.mrjoshuasperry.mcutils.menu.items.StaticMenuItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * A {@link Menu} that pages a list of items through an inset content region,
 * reserving the border for decoration and the bottom row for navigation.
 */
public class PaginatedMenu extends Menu {
    private static final int ROW_WIDTH = 9;
    /** Rows below which there is no inset region to page through. */
    private static final int MIN_ROWS = 3;

    private final List<Integer> contentSlots;
    private final int prevSlot;
    private final int nextSlot;
    private final int indicatorSlot;

    private List<MenuItem> contents;
    private int page;

    public PaginatedMenu(Component title, int rows, List<MenuItem> contents) {
        super(title, clampRows(rows) * ROW_WIDTH);

        int actualRows = clampRows(rows);
        this.contentSlots = insetSlots(actualRows);
        this.contents = new ArrayList<>(contents);
        this.page = 0;

        int lastRow = (actualRows - 1) * ROW_WIDTH;
        this.prevSlot = lastRow + 3;
        this.indicatorSlot = lastRow + 4;
        this.nextSlot = lastRow + 5;

        this.fillInventory(new DecorMenuItem(Material.GRAY_STAINED_GLASS_PANE));
        this.render();
    }

    private static int clampRows(int rows) {
        return Math.clamp(rows, MIN_ROWS, 6);
    }

    /** Every slot not on the outer border, in reading order. */
    private static List<Integer> insetSlots(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < rows - 1; row++) {
            for (int column = 1; column < ROW_WIDTH - 1; column++) {
                slots.add(row * ROW_WIDTH + column);
            }
        }
        return slots;
    }

    public int getPageSize() {
        return this.contentSlots.size();
    }

    public int getPageCount() {
        // Always at least one page so an empty list still renders a valid menu.
        return Math.max(1, (int) Math.ceil((double) this.contents.size() / this.getPageSize()));
    }

    public int getPage() {
        return this.page;
    }

    /** Jumps to a page, clamped to the valid range. Re-renders. */
    public void setPage(int page) {
        this.page = Math.clamp(page, 0, this.getPageCount() - 1);
        this.render();
    }

    public void nextPage() {
        this.setPage(this.page + 1);
    }

    public void previousPage() {
        this.setPage(this.page - 1);
    }

    /**
     * Replaces the paged items. Keeps the current page if it still exists, so a
     * live refresh does not yank the viewer back to the first page.
     */
    public void setContents(List<MenuItem> contents) {
        this.contents = new ArrayList<>(contents);
        this.setPage(this.page);
    }

    public List<MenuItem> getContents() {
        return List.copyOf(this.contents);
    }

    /** Draws the current page and the navigation row. */
    public void render() {
        int start = this.page * this.getPageSize();

        for (int index = 0; index < this.contentSlots.size(); index++) {
            int slot = this.contentSlots.get(index);
            int contentIndex = start + index;

            if (contentIndex < this.contents.size()) {
                this.setItem(slot, this.contents.get(contentIndex));
            } else {
                this.clearItem(slot);
            }
        }

        this.renderNavigation();
    }

    private void renderNavigation() {
        if (this.page > 0) {
            this.setItem(this.prevSlot, new StaticMenuItem(
                    navigationItem(Material.RED_STAINED_GLASS_PANE, "Previous page"),
                    (player, menu) -> this.previousPage()));
        } else {
            this.setItem(this.prevSlot, new DecorMenuItem(Material.GRAY_STAINED_GLASS_PANE));
        }

        if (this.page < this.getPageCount() - 1) {
            this.setItem(this.nextSlot, new StaticMenuItem(
                    navigationItem(Material.LIME_STAINED_GLASS_PANE, "Next page"),
                    (player, menu) -> this.nextPage()));
        } else {
            this.setItem(this.nextSlot, new DecorMenuItem(Material.GRAY_STAINED_GLASS_PANE));
        }

        // StaticMenuItem with no handler rather than DecorMenuItem: DecorMenuItem
        // blanks the item name, which would throw away the page label.
        this.setItem(this.indicatorSlot, new StaticMenuItem(
                navigationItem(Material.PAPER, "Page " + (this.page + 1) + " of " + this.getPageCount()), null));
    }

    private static ItemStack navigationItem(Material material, String label) {
        return new ItemBuilder(material)
                .setName(Component.text(label, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false))
                .build();
    }
}
