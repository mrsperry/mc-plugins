package com.mrjoshuasperry.compressedmobs;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {
    private final Random random = new Random();
    private final NamespacedKey compressedKey = new NamespacedKey(this, "compressed");

    private static final double GLOBAL_COMPRESS_CHANCE = 100;
    private static final int GLOBAL_MIN_YIELD = 3;
    private static final int GLOBAL_MAX_YIELD = 5;

    private final Map<EntityType, Settings> creatures = new EnumMap<>(EntityType.class);
    private final Set<EntityType> blacklist = new HashSet<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        FileConfiguration config = this.getConfig();

        if (config.isConfigurationSection("creatures")) {
            for (String key : config.getConfigurationSection("creatures").getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    Settings settings = Settings.fromConfig(config.getConfigurationSection("creatures." + key),
                            GLOBAL_COMPRESS_CHANCE, GLOBAL_MIN_YIELD, GLOBAL_MAX_YIELD);

                    this.creatures.put(type, settings);
                } catch (IllegalArgumentException ex) {
                    this.getLogger().severe("Could not parse entity type: " + key);
                }
            }
        }
        this.getLogger().info(() -> "Found " + this.creatures.size() + " custom setting(s)");

        if (config.isList("blacklist")) {
            for (String mob : config.getStringList("blacklist")) {
                try {
                    this.blacklist.add(EntityType.valueOf(mob.toUpperCase()));
                } catch (Exception ex) {
                    this.getLogger().severe("Invalid entity type: " + mob);
                }
            }
        }
        this.getLogger().info(() -> "Found " + this.blacklist.size() + " types on the blacklist");

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (this.blacklist.contains(event.getEntityType())) {
            return;
        }

        Entity mob = event.getEntity();
        EntityType type = mob.getType();
        PersistentDataContainer container = mob.getPersistentDataContainer();

        if (!container.has(this.compressedKey, PersistentDataType.BYTE)) {
            return;
        }

        Settings values = this.creatures.get(type);
        if (values == null) {
            return;
        }

        int range = values.maxYield() - values.minYield();
        int mobsToSpawn = values.minYield() + (range > 0 ? this.random.nextInt(range) : 0);

        for (int amount = 0; amount < mobsToSpawn; amount++) {
            Entity entity = mob.getWorld().spawnEntity(mob.getLocation(), type);
            entity.setVelocity(new Vector(
                    (this.random.nextDouble() * 2) - 1,
                    (this.random.nextDouble() / 2),
                    (this.random.nextDouble() * 2) - 1));
        }

    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        EntityType type = event.getEntityType();

        if (this.blacklist.contains(type)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        PersistentDataContainer container = entity.getPersistentDataContainer();

        // Ignore already compressed mobs
        if (container.has(this.compressedKey, PersistentDataType.BYTE)) {
            return;
        }

        // Ignore non-natural spawns
        if (event.getSpawnReason() != SpawnReason.NATURAL) {
            return;
        }

        Settings values = this.creatures.get(type);
        if (values == null) {
            return;
        }

        if (this.random.nextDouble() * values.compressChance() <= 1) {
            container.set(this.compressedKey, PersistentDataType.BYTE, (byte) 1);

            entity.customName(Component.text(ChatColor.GRAY + "Compressed " + entity.getName()));
            entity.setCustomNameVisible(true);
            entity.setRemoveWhenFarAway(true);
        }

    }
}
