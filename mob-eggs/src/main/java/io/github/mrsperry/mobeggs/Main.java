package io.github.mrsperry.mobeggs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {
    // Can't group all mobs that have eggs any other way
    public HashSet<EntityType> blacklist = new HashSet<>();

    private Random random;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.random = new Random();

        FileConfiguration config = this.getConfig();
        if (config.isList("blacklist")) {
            for (String type : config.getStringList("blacklist")) {
                try {
                    this.blacklist.add(EntityType.valueOf(type.toUpperCase().replace(" ", "_")));
                } catch (Exception ex) {
                    this.getLogger().severe("Invalid entity type: " + type);
                }
            }
        }

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity entity = event.getEntity();
        Entity hit = event.getHitEntity();
        if (entity instanceof Egg) {
            if (hit != null) {
                ItemStack item = null;
                try {
                    item = new ItemStack(Material.valueOf(hit.getType().toString() + "_SPAWN_EGG"));
                } catch (IllegalArgumentException ignored) { /* Mob does not have a spawn egg */ }

                if (item != null && !this.blacklist.contains(hit.getType())) {
                    hit.getWorld().dropItemNaturally(hit.getLocation().add(0, 1, 0), item);
                    hit.getWorld().playSound(hit.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0);
                    hit.remove();
                } else {
                    this.spawnChicken(entity.getLocation());
                }
            } else {
                this.spawnChicken(entity.getLocation());
            }
        }
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

    private String getType(EntityType type) {
        switch (type) {
            case MUSHROOM_COW:
                return "MOOSHROOM";
            case PIG_ZOMBIE:
                return "ZOMBIE_PIGMAN";
            default:
                return type.toString();
        }
    }
}
