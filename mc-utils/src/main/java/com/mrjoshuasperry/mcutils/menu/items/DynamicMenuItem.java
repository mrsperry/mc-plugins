package com.mrjoshuasperry.mcutils.menu.items;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.menu.Menu;

/**
 * A menu item that re-renders from live state every time it is drawn, so the
 * caller does not have to rebuild the menu when the thing it displays changes.
 */
public class DynamicMenuItem extends MenuItem {
    private final Supplier<ItemStack> supplier;

    public DynamicMenuItem(Supplier<ItemStack> supplier) {
        this(supplier, null);
    }

    public DynamicMenuItem(Supplier<ItemStack> supplier, BiConsumer<Player, Menu> onClick) {
        // The supplier is the source of truth; seed the base item so getItem is
        // never null before the first draw.
        super(supplier.get(), onClick);

        this.supplier = supplier;
    }

    @Override
    public ItemStack getItem() {
        return this.supplier.get();
    }
}
