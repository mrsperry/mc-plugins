package com.mrjoshuasperry.mcutils.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import net.kyori.adventure.text.Component;

public class PotionBuilder extends ItemBuilder {
    private PotionMeta potionMeta;
    private List<PotionEffect> effects;

    public PotionBuilder() {
        super(Material.POTION);

        this.potionMeta = (PotionMeta) this.meta;
        this.effects = new ArrayList<>();
    }

    public PotionBuilder setBase(PotionType type) {
        this.potionMeta.setBasePotionType(type);
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
    public PotionBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    @Override
    public PotionBuilder setName(Component name) {
        super.setName(name);
        return this;
    }

    @Override
    public PotionBuilder setLore(List<Component> lore) {
        super.setLore(lore);
        return this;
    }

    @Override
    public PotionBuilder addLore(Component loreLine) {
        super.addLore(loreLine);
        return this;
    }

    @Override
    public PotionBuilder setEnchantments(Map<Enchantment, Integer> enchantments) {
        super.setEnchantments(enchantments);
        return this;
    }

    @Override
    public PotionBuilder addEnchantment(Enchantment enchantment, int level) {
        super.addEnchantment(enchantment, level);
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
}
