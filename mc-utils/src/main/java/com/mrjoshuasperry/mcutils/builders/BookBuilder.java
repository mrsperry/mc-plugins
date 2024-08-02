package com.mrjoshuasperry.mcutils.builders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

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

    public BookBuilder addLine(int index, String line) {
        String page = "";
        try {
            page = this.bookMeta.getPage(index);
        } catch (Exception ex) {
            this.bookMeta.addPage(line);
        }
        this.bookMeta.setPage(index, page.concat("\n" + line));
        return this;
    }

    public BookBuilder addPage(List<String> content) {
        int count = this.bookMeta.getPageCount();
        return this.setPage((count == 0 ? 0 : count + 1), content);
    }

    public BookBuilder addPage(String... content) {
        int count = this.bookMeta.getPageCount();
        return this.setPage((count == 0 ? 0 : count + 1), Arrays.asList(content));
    }

    public BookBuilder setPage(int index, List<String> content) {
        try {
            this.bookMeta.getPage(index);
        } catch (Exception ex) {
            this.bookMeta.addPage("");
        }
        this.bookMeta.setPage(index, this.getLines(content));
        return this;
    }

    public BookBuilder setPage(int index, String... content) {
        return this.setPage(index, Arrays.asList(content));
    }

    private String getLines(List<String> content) {
        if (content.size() > 0) {
            String page = "";
            for (String line : content) {
                page = page.concat(line + "\n");
            }
            return page.substring(0, page.length() - 1);
        }
        return "";
    }

    public BookBuilder setPages(List<List<String>> pages) {
        for (int index = 0; index < pages.size(); index++) {
            this.setPage(index, pages.get(index));
        }
        return this;
    }

    @Override
    public BookBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    @Override
    public BookBuilder setData(short data) {
        this.item.setDurability(data);
        return this;
    }

    @Override
    public BookBuilder setName(String name) {
        this.meta.setDisplayName(name);
        return this;
    }

    @Override
    public BookBuilder setLore(List<String> lore) {
        this.meta.setLore(lore);
        return this;
    }

    @Override
    public BookBuilder addLore(String loreLine) {
        List<String> temp = this.meta.getLore();
        temp.add(loreLine);
        return this.setLore(temp);
    }

    @Override
    public BookBuilder setEnchantments(HashMap<Enchantment, Integer> enchantments) {
        for (Enchantment enchant : enchantments.keySet()) {
            this.meta.addEnchant(enchant, enchantments.get(enchant), true);
        }
        return this;
    }

    @Override
    public BookBuilder addEnchantment(Enchantment enchantment, int level) {
        this.meta.addEnchant(enchantment, level, true);
        return this;
    }

    @Override
    public ItemStack build() {
        return super.build();
    }
}
