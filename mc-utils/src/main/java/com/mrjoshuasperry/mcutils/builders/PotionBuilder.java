package com.mrjoshuasperry.mcutils.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class PotionBuilder extends ItemBuilder {
    private PotionMeta potionMeta;
    private List<PotionEffect> effects;

    public PotionBuilder() {
        super(Material.POTION);

        this.potionMeta = (PotionMeta) this.meta;
        this.effects = new ArrayList<>();
    }

    public PotionBuilder setBase(PotionData data) {
        this.potionMeta.setBasePotionData(data);
        return this;
    }

    public PotionBuilder setBase(PotionType type) {
        this.potionMeta.setBasePotionData(new PotionData(type));
        return this;
    }

    public PotionBuilder setBase(PotionType type, boolean extended, boolean upgraded) {
        this.potionMeta.setBasePotionData(new PotionData(type, extended, upgraded));
        return this;
    }

    public PotionBuilder setColor(Color color) {
        this.potionMeta.setColor(color);
        return this;
    }

    public PotionBuilder setColor(int r, int g, int b) {
        this.potionMeta.setColor(Color.fromRGB(r, g, b));
        return this;
    }

    public PotionBuilder addEffect(PotionEffect effect) {
        this.effects.add(effect);
        return this;
    }

    public PotionBuilder addEffect(PotionEffectType type, int duration) {
        this.effects.add(new PotionEffect(type, duration, 0));
        return this;
    }

    public PotionBuilder addEffect(PotionEffectType type, int duration, int amplifier) {
        this.effects.add(new PotionEffect(type, duration, amplifier));
        return this;
    }

    public PotionBuilder setEffects(List<PotionEffect> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public ItemStack build() {
        for (PotionEffect effect : this.effects) {
            this.potionMeta.addCustomEffect(new PotionEffect(
                    effect.getType(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    false,
                    true), true);
        }

        return super.build();
    }

    @Override
    public PotionBuilder setName(String name) {
        super.setName(name);
        return this;
    }

    @Override
    public PotionBuilder setAmount(int amount) {
        super.setAmount(amount);
        return this;
    }

    @Override
    public ItemBuilder setData(short data) {
        super.setData(data);
        return this;
    }

    @Override
    public ItemBuilder setLore(List<String> lore) {
        super.setLore(lore);
        return this;
    }

    @Override
    public ItemBuilder addLore(String loreLine) {
        super.addLore(loreLine);
        return this;
    }

    @Override
    public ItemBuilder setEnchantments(HashMap<Enchantment, Integer> enchantments) {
        super.setEnchantments(enchantments);
        return this;
    }

    @Override
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        super.addEnchantment(enchantment, level);
        return this;
    }
}
