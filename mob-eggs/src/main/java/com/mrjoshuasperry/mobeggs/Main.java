package com.mrjoshuasperry.mobeggs;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    // Can't group all mobs that have eggs any other way
    private final Set<EntityType> blacklist = new HashSet<>();

    private Random random;
    private NamespacedKey markerKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.random = new Random();
        this.markerKey = new NamespacedKey(this, "captured");

        FileConfiguration config = this.getConfig();
        if (config.isList("blacklist")) {
            for (String type : config.getStringList("blacklist")) {
                try {
                    this.blacklist.add(EntityType.valueOf(type.toUpperCase().replace(" ", "_")));
                } catch (Exception _) {
                    this.getLogger().severe("Invalid entity type: " + type);
                }
            }
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Egg egg)) {
            return;
        }

        Entity hit = event.getHitEntity();
        if (hit == null || MobCapture.denialFor(hit, this.blacklist) != null) {
            this.spawnChicken(egg.getLocation());
            return;
        }

        this.capture((LivingEntity) hit);
    }

    private void capture(LivingEntity entity) {
        ItemStack egg = MobCapture.createEgg(entity, this.markerKey);
        if (egg == null) {
            this.spawnChicken(entity.getLocation());
            return;
        }

        // Otherwise the lead is destroyed along with the mob holding it
        if (entity.isLeashed()) {
            entity.setLeashHolder(null);
            entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.LEAD));
        }

        entity.getWorld().dropItemNaturally(entity.getLocation().add(0, 1, 0), egg);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0);
        entity.remove();
    }

    private void spawnChicken(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        // Implement default chicken spawning mechanics
        int amount = 0;
        if (this.random.nextInt(8) == 0) {
            amount = 1;
        }
        if (this.random.nextInt(32) == 0) {
            amount = 4;
        }
        for (int index = 0; index < amount; index++) {
            Chicken chick = (Chicken) world.spawnEntity(location, EntityType.CHICKEN);
            chick.setBaby();
        }
    }

    @EventHandler
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        // Don't want chickens spawning when we hit a mob
        event.setHatching(false);
    }
}
