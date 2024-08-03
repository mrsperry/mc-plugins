package com.mrjoshuasperry.pocketplugins.additions.featherplucker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.pocketplugins.MiniAdditions;

public class FeatherPlucker extends Module {
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private int cooldown;

    public FeatherPlucker() {
        super("FeatherPlucker");

        this.plugin = MiniAdditions.getInstance();
        this.key = new NamespacedKey(this.plugin, "feather-plucked");
    }

    @Override
    public void init(final YamlConfiguration config) {
        super.init(config);

        this.cooldown = config.getInt("cooldown", 20 * 300);
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        final Entity damaged = event.getEntity();

        if (damaged.getType() == EntityType.CHICKEN) {
            final Chicken chicken = (Chicken) damaged;

            if (event.getFinalDamage() >= chicken.getHealth()) {
                return;
            }

            final PersistentDataContainer container = chicken.getPersistentDataContainer();

            if (!container.has(this.key, PersistentDataType.BYTE)) {
                container.set(this.key, PersistentDataType.BYTE, (byte) 0);

                chicken.getWorld().dropItemNaturally(chicken.getLocation(), new ItemStack(Material.FEATHER));

                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> container.remove(this.key),
                        this.cooldown);
            }
        }
    }
}
