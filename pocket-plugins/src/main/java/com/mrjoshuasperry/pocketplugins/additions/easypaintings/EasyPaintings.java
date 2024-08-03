package com.mrjoshuasperry.pocketplugins.additions.easypaintings;

import org.bukkit.Art;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class EasyPaintings extends Module {
    public EasyPaintings() {
        super("EasyPaintings");
    }

    @EventHandler
    public void onPaintingClick(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Painting && event.getPlayer().isSneaking()) {
            if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
                return;
            }

            Painting painting = (Painting) event.getRightClicked();
            boolean changed = false;
            int type = painting.getArt().ordinal();

            while (!changed) {
                type = type + 1 > 25 ? 0 : type + 1;
                changed = painting.setArt(Art.values()[type]);
            }
        }
    }
}
