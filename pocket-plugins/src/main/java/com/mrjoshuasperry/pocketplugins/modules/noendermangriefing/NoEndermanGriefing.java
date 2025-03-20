package com.mrjoshuasperry.pocketplugins.modules.noendermangriefing;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class NoEndermanGriefing extends Module {
    public NoEndermanGriefing() {
        super("NoEndermanGriefing");
    }

    @Override
    public void initialize(YamlConfiguration config) {
        super.initialize(config);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity().getType() == EntityType.ENDERMAN) {
            event.setCancelled(true);
        }
    }
}
