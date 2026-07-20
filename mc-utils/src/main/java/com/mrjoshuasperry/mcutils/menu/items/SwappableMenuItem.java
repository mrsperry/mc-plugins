package com.mrjoshuasperry.mcutils.menu.items;

import java.util.List;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.Menu;

public class SwappableMenuItem extends MenuItem {
    private int index;
    private List<ItemStack> items;

    /** Ticks between automatic swaps, or 0 to only swap on click. */
    private int swapInterval;
    private int ticksSinceSwap;

    public SwappableMenuItem(List<ItemStack> items, BiConsumer<Player, Menu> onClick) {
        this(items, onClick, 0);
    }

    /**
     * @param swapInterval menu ticks between automatic swaps, or 0 to swap only on
     *                     click. See
     *                     {@link com.mrjoshuasperry.mcutils.menu.MenuManager} for
     *                     how long a menu tick is.
     */
    public SwappableMenuItem(List<ItemStack> items, BiConsumer<Player, Menu> onClick, int swapInterval) {
        super(items.get(0), onClick);

        this.index = 0;
        this.items = items;
        this.swapInterval = swapInterval;
        this.ticksSinceSwap = 0;
    }

    @Override
    public void onClick(Player player, Menu menu) {
        super.onClick(player, menu);
        this.swap();
    }

    @Override
    public void tick() {
        if (this.swapInterval <= 0) {
            return;
        }

        this.ticksSinceSwap++;
        if (this.ticksSinceSwap >= this.swapInterval) {
            this.ticksSinceSwap = 0;
            this.swap();
        }
    }

    public void setSwapInterval(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public int getSwapInterval() {
        return this.swapInterval;
    }

    public void swap() {
        this.index = (this.index + 1) % this.items.size();

        super.setItem(this.items.get(this.index));
    }

    public void setIndex(int index) {
        this.index = index;
        super.setItem(this.items.get(index));
    }

    public int getIndex() {
        return this.index;
    }

    public ItemStack getCurrentItem() {
        return this.items.get(this.index);
    }
}
