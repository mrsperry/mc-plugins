package com.mrjoshuasperry.mcutils.menu.items;

import java.util.List;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.Menu;

public class SwappableMenuItem extends MenuItem {
    private int index;
    private List<ItemStack> items;

    public SwappableMenuItem(List<ItemStack> items, BiConsumer<Player, Menu> onClick) {
        super(items.get(0), onClick);

        this.index = 0;
        this.items = items;
    }

    @Override
    public void onClick(Player player, Menu menu) {
        super.onClick(player, menu);
        this.swap();
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
