package com.mrjoshuasperry.deathchest.listeners;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.deathchest.DeathPileManager;
import com.mrjoshuasperry.deathchest.Main;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

public class DeathPileListener implements Listener {
    private final Main plugin;

    public DeathPileListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        if (!(event.getAttacked() instanceof Interaction box)) {
            return;
        }

        PersistentDataContainer container = box.getPersistentDataContainer();
        if (!container.getOrDefault(plugin.getPileMemberKey(), PersistentDataType.BOOLEAN, false)) {
            return;
        }

        // Ours to handle: never let the swing fall through to combat or block damage.
        event.setCancelled(true);

        Player player = event.getPlayer();
        DeathPileManager manager = plugin.getPileManager();

        String groupValue = container.get(plugin.getPileGroupKey(), PersistentDataType.STRING);
        if (player.isSneaking() && groupValue != null) {
            manager.grabAll(player, UUID.fromString(groupValue));
        } else {
            manager.grabOne(player, box);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof ItemDisplay display) {
                plugin.getPileManager().adopt(display);
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof ItemDisplay display) {
                plugin.getPileManager().forget(display.getUniqueId());
            }
        }
    }
}
