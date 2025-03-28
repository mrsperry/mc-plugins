package com.mrjoshuasperry.pocketplugins.modules.armorstandposer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class ArmorStandPoser extends Module {
    public ArmorStandPoser(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);
    }

    @EventHandler
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        Entity entity = event.getRightClicked();

        if (entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }

        if (!event.getPlayer().isSneaking()) {
            return;
        }

        Material type = item.getType();
        ArmorStand armorStand = (ArmorStand) entity;

        switch (type) {
            case STICK:
                if (armorStand.hasArms()) {
                    break;
                }
                event.setCancelled(true);
                armorStand.setArms(true);
                this.removeOneMainHand(event.getPlayer());
                break;
            case SUGAR:
                if (armorStand.isSmall()) {
                    break;
                }
                event.setCancelled(true);
                armorStand.setSmall(true);
                this.removeOneMainHand(event.getPlayer());
                break;
            default:
                break;
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
