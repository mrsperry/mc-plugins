package com.mrjoshuasperry.pocketplugins.modules.easypaintings;

import java.util.ArrayList;

import org.bukkit.Art;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

/** @author TimPCunningham */
public class EasyPaintings extends Module {
    protected ArrayList<Art> availableArt;

    public EasyPaintings(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);

        this.availableArt = Lists
                .newArrayList(RegistryAccess.registryAccess().getRegistry(RegistryKey.PAINTING_VARIANT).iterator());
    }

    @EventHandler
    public void onPaintingClick(PlayerInteractEntityEvent event) {
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            return;
        }

        if (!(event.getRightClicked() instanceof Painting)) {
            return;
        }

        Painting painting = (Painting) event.getRightClicked();
        int newArtIndex = (this.availableArt.indexOf(painting.getArt()) + 1) % this.availableArt.size();
        painting.setArt(availableArt.get(newArtIndex));
    }
}
