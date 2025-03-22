package com.mrjoshuasperry.mcutils.builders;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import net.kyori.adventure.text.Component;

public class BookBuilder extends ItemBuilder {
    private BookMeta bookMeta;

    public BookBuilder() {
        super(Material.WRITTEN_BOOK);

        this.bookMeta = (BookMeta) this.meta;
        this.bookMeta.setGeneration(BookMeta.Generation.ORIGINAL);
    }

    public BookBuilder setAuthor(String author) {
        this.bookMeta.setAuthor(author);
        return this;
    }

    public BookBuilder setTitle(String title) {
        this.bookMeta.setTitle(title);
        return this;
    }

    public BookBuilder setGeneration(BookMeta.Generation generation) {
        this.bookMeta.setGeneration(generation);
        return this;
    }

    public BookBuilder addLine(int pageIndex, Component line) {
        Component page;
        try {
            page = this.bookMeta.page(pageIndex);
        } catch (Exception ex) {
            this.bookMeta.addPages(line);
            return this;
        }

        this.bookMeta.page(pageIndex, page.append(Component.newline()).append(line));
        return this;
    }

    public BookBuilder setPage(int pageIndex, Component page) {
        this.bookMeta.page(pageIndex, page);
        return this;
    }

    public BookBuilder setPages(List<Component> pages) {
        this.bookMeta.pages(pages);
        return this;
    }

    @Override
    public BookBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    @Override
    public BookBuilder setName(Component name) {
        super.setName(name);
        return this;
    }

    @Override
    public BookBuilder setLore(List<Component> lore) {
        super.setLore(lore);
        return this;
    }

    @Override
    public BookBuilder addLore(Component loreLine) {
        super.addLore(loreLine);
        return this;
    }

    @Override
    public BookBuilder setEnchantments(Map<Enchantment, Integer> enchantments) {
        super.setEnchantments(enchantments);
        return this;
    }

    @Override
    public BookBuilder addEnchantment(Enchantment enchantment, int level) {
        super.addEnchantment(enchantment, level);
        return this;
    }

    @Override
    public ItemStack build() {
        return super.build();
    }
}
