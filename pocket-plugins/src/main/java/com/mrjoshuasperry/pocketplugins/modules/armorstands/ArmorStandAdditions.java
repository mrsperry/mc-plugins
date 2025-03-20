package com.mrjoshuasperry.pocketplugins.modules.armorstands;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class ArmorStandAdditions extends Module {
    public ArmorStandAdditions() {
        super("ArmorStands");
    }

    @EventHandler
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand().equals(EquipmentSlot.HAND)) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            Entity entity = event.getRightClicked();

            if (entity.getType().equals(EntityType.ARMOR_STAND) && event.getPlayer().isSneaking()) {
                Material type = item.getType();
                ArmorStand armorStand = (ArmorStand) entity;

                switch (type) {
                    case STICK:
                        if (armorStand.hasArms()) {
                            break;
                        }
                        event.setCancelled(true);
                        armorStand.setArms(true);
                        removeOneMainHand(event.getPlayer());
                        break;
                    case SUGAR:
                        if (armorStand.isSmall()) {
                            break;
                        }
                        event.setCancelled(true);
                        armorStand.setSmall(true);
                        removeOneMainHand(event.getPlayer());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void removeOneMainHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }
}
