package com.mrjoshuasperry.deathchest.listeners;

import java.util.UUID;

import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.deathchest.DeathPileManager;
import com.mrjoshuasperry.deathchest.Main;
import com.mrjoshuasperry.mcutils.audit.Audit;

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
        } else if (manager.isHeadHitbox(box)) {
            // The head holds no stack of its own; it hands over one at random
            if (groupValue != null) {
                manager.grabRandom(player, UUID.fromString(groupValue));
            }
        } else {
            manager.grabOne(player, box);
        }
    }

    /**
     * A stack removed by anything but this plugin (a /kill, say) is gone for good, so
     * it goes on record. Chunk unloads are left out: the stack is only put away.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof ItemDisplay display) || event.getCause() == EntityRemoveEvent.Cause.PLUGIN
                || event.getCause() == EntityRemoveEvent.Cause.UNLOAD) {
            return;
        }

        PersistentDataContainer container = display.getPersistentDataContainer();
        String groupValue = container.get(plugin.getPileGroupKey(), PersistentDataType.STRING);
        // Only stacks point at a hitbox; the head has a role instead
        if (groupValue == null || container.has(plugin.getPileRoleKey())
                || !container.has(plugin.getPileInteractionIdKey())) {
            return;
        }

        Audit.event(plugin, "pile-item-destroyed")
                .with("owner", plugin.getPileManager().ownerOf(UUID.fromString(groupValue)))
                .at(display.getLocation())
                .with("item", display.getItemStack())
                .with("cause", event.getCause())
                .record();
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Display display) {
                plugin.getPileManager().adopt(display);
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Display display) {
                plugin.getPileManager().forget(display.getUniqueId());
            }
        }
    }
}
