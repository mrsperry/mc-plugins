package com.mrjoshuasperry.pocketplugins.modules.nosheepgriefing;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class NoSheepGriefingListener extends Module {
    public NoSheepGriefingListener() {
        super("NoSheepGriefing");
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        final Entity entity = event.getEntity();
        if (entity.getType() == EntityType.SHEEP) {
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRASS_PLACE, 1, 1);
            event.setCancelled(true);
        }
    }
}
