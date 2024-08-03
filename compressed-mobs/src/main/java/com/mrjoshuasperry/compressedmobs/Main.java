package com.mrjoshuasperry.compressedmobs;

import com.google.common.collect.Lists;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {
    private ArrayList<CreatureSpawnEvent.SpawnReason> blacklistedReasons = Lists.newArrayList(
        CreatureSpawnEvent.SpawnReason.BEEHIVE,
        CreatureSpawnEvent.SpawnReason.CUSTOM,
        CreatureSpawnEvent.SpawnReason.CURED,
        CreatureSpawnEvent.SpawnReason.DROWNED,
        CreatureSpawnEvent.SpawnReason.EXPLOSION,
        CreatureSpawnEvent.SpawnReason.INFECTION,
        CreatureSpawnEvent.SpawnReason.LIGHTNING,
        CreatureSpawnEvent.SpawnReason.SHEARED,
        CreatureSpawnEvent.SpawnReason.SHOULDER_ENTITY,
        CreatureSpawnEvent.SpawnReason.SLIME_SPLIT,
        CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
    );

    private Random random;

    private double chance;
    private int minYield;
    private int maxYield;

    private HashMap<EntityType, Object[]> creatures = new HashMap<>();
    private HashSet<EntityType> blacklist = new HashSet<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.random = new Random();

        FileConfiguration config = this.getConfig();
        this.chance = config.getDouble("global-chance", 100.0);
        this.minYield = config.getInt("global-min-yield", 3);
        this.maxYield = config.getInt("global-max-yield", 5);

        if (config.isConfigurationSection("creatures")) {
            for (String key : config.getConfigurationSection("creatures").getKeys(false)) {
                try {
                    this.creatures.put(EntityType.valueOf(key.toUpperCase()), new Object[] {
                        config.getDouble("creatures." + key + ".chance", this.chance),
                        config.getInt("creatures." + key + ".min-yield", this.minYield),
                        config.getInt("creatures." + key + ".max-yield", this.maxYield)
                    });
                } catch (IllegalArgumentException ex) {
                    this.getLogger().severe("Could not parse entity type: " + key);
                }
            }
        }
        this.getLogger().info("Found " + this.creatures.size() + " custom setting(s)");

        if (config.isList("blacklist")) {
            for (String mob : config.getStringList("blacklist")) {
                try {
                    this.blacklist.add(EntityType.valueOf(mob.toUpperCase()));
                } catch (Exception ex) {
                    this.getLogger().severe("Invalid entity type: " + mob);
                }
            }
        }
        this.getLogger().info("Found " + this.blacklist.size() + " types on the blacklist");

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!this.blacklist.contains(event.getEntityType())) {
            Entity mob = event.getEntity();
            EntityType type = mob.getType();
            PersistentDataContainer container = mob.getPersistentDataContainer();

            if (container.has(new NamespacedKey(this, "compressed"), PersistentDataType.BYTE)) {
                int min = this.minYield, max = this.maxYield;
                if (this.creatures.containsKey(type)) {
                    Object[] values = this.creatures.get(type);
                    min = (int) values[1];
                    max = (int) values[2];
                }

                int range = max - min;
                int total = min + (range > 0 ? this.random.nextInt(range) : 0);
                for (int amount = 0; amount < total; amount++) {
                    Entity entity = mob.getWorld().spawnEntity(mob.getLocation(), type);
                    entity.setVelocity(new Vector(
                        (this.random.nextDouble() * 2) - 1,
                        (this.random.nextDouble() / 2),
                        (this.random.nextDouble() * 2) - 1));
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        EntityType type = event.getEntityType();

        if (!this.blacklist.contains(type)) {
            Entity creature = event.getEntity();
            PersistentDataContainer container = creature.getPersistentDataContainer();

            if (container.has(new NamespacedKey(this, "compressed"), PersistentDataType.BYTE)
                || this.blacklistedReasons.contains(event.getSpawnReason())) {
                    return;
            }

            double chance = this.creatures.containsKey(type) ? (double) this.creatures.get(type)[0] : this.chance;
            if (this.random.nextDouble() * chance <= 1) {
                container.set(new NamespacedKey(this, "compressed"), PersistentDataType.BYTE, (byte) 1);

                creature.setCustomName(ChatColor.GRAY + "Compressed " + creature.getName());
                creature.setCustomNameVisible(true);
            }
        }
    }
}
