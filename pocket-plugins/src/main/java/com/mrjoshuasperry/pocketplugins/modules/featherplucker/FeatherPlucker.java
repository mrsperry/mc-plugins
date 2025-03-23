package com.mrjoshuasperry.pocketplugins.modules.featherplucker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class FeatherPlucker extends Module {
    private NamespacedKey key;
    private int cooldown;

    public FeatherPlucker(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.key = this.createKey("feather-plucked");
        this.cooldown = readableConfig.getInt("cooldown", 300) * 20;
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

                Bukkit.getScheduler().scheduleSyncDelayedTask(this.getPlugin(), () -> container.remove(this.key),
                        this.cooldown);
            }
        }
    }
}
