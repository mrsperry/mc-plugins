package com.mrjoshuasperry.mobeggs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class MobCapture {
    public enum Denial {
        PLAYER,
        BOSS,
        BLACKLISTED,
        NO_SPAWN_EGG,
        HAS_PASSENGERS,
        RIDING
    }

    private static final Set<EntityType> BOSSES = Set.of(EntityType.WITHER, EntityType.ENDER_DRAGON);

    public static Denial denialFor(Entity entity, Set<EntityType> blacklist) {
        if (!(entity instanceof LivingEntity)) {
            return Denial.NO_SPAWN_EGG;
        }

        Denial denial = denialForType(entity.getType(), blacklist);
        if (denial != null) {
            return denial;
        }

        if (!entity.getPassengers().isEmpty()) {
            return Denial.HAS_PASSENGERS;
        }

        if (entity.getVehicle() != null) {
            return Denial.RIDING;
        }

        return null;
    }

    static Denial denialForType(EntityType type, Set<EntityType> blacklist) {
        if (type == EntityType.PLAYER) {
            return Denial.PLAYER;
        }

        if (BOSSES.contains(type)) {
            return Denial.BOSS;
        }

        if (blacklist.contains(type)) {
            return Denial.BLACKLISTED;
        }

        if (spawnEggMaterial(type) == null) {
            return Denial.NO_SPAWN_EGG;
        }

        return null;
    }

    public static Material spawnEggMaterial(EntityType type) {
        try {
            return Material.valueOf(type.toString() + "_SPAWN_EGG");
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    public static ItemStack createEgg(LivingEntity entity, NamespacedKey markerKey) {
        Material material = spawnEggMaterial(entity.getType());
        if (material == null) {
            return null;
        }

        EntitySnapshot snapshot = entity.createSnapshot();
        if (snapshot == null) {
            return null;
        }

        ItemStack egg = new ItemStack(material);
        SpawnEggMeta meta = (SpawnEggMeta) egg.getItemMeta();
        meta.setSpawnedEntity(snapshot);
        meta.setMaxStackSize(1);
        meta.itemName(eggName(entity));
        meta.lore(buildLore(entity));
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BOOLEAN, true);
        egg.setItemMeta(meta);
        return egg;
    }

    static Component eggName(LivingEntity entity) {
        Component custom = entity.customName();
        Component base = custom != null ? custom : Component.translatable(entity.getType());
        return base.colorIfAbsent(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);
    }

    static List<Component> buildLore(LivingEntity entity) {
        List<Component> lore = new ArrayList<>();

        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        lore.add(line("Health", maxHealth == null
                ? formatNumber(entity.getHealth())
                : formatNumber(entity.getHealth()) + " / " + formatNumber(maxHealth.getValue())));

        if (entity instanceof Ageable ageable && !ageable.isAdult()) {
            lore.add(line("Age", "Baby"));
        }

        if (entity instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() != null) {
            lore.add(line("Owner", tameable.getOwner().getName()));
        }

        String variant = variantOf(entity);
        if (variant != null) {
            lore.add(line("Variant", variant));
        }

        return lore;
    }

    static String variantOf(LivingEntity entity) {
        return switch (entity) {
            case Horse horse -> describe(horse.getColor()) + " / " + describe(horse.getStyle());
            case Villager villager -> describe(villager.getProfession());
            case Cat cat -> describe(cat.getCatType());
            case Wolf wolf -> describe(wolf.getVariant());
            case Axolotl axolotl -> describe(axolotl.getVariant());
            case Sheep sheep -> describe(sheep.getColor());
            default -> null;
        };
    }

    static String describe(Object value) {
        if (value == null) {
            return null;
        }

        String raw = switch (value) {
            case Keyed keyed -> keyed.key().value();
            case Enum<?> constant -> constant.name();
            default -> value.toString();
        };

        return prettify(raw);
    }

    static String prettify(String raw) {
        String[] words = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }

        return builder.toString();
    }

    static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }

        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component line(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE))
                .decoration(TextDecoration.ITALIC, false);
    }
}
