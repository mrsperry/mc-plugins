package com.mrjoshuasperry.miniadditions.additions.experimental;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.miniadditions.utils.CustomProjectile;

public class BottleProjectile {
    public static CustomProjectile getBottleProjectile(Location location, Vector direction) {
        CustomProjectile bottle = new CustomProjectile(location, direction, 1, 200)
                .addAcceleration(0.2, 2.5)
                .addGravity(0.1)
                .onDisplay(proj -> {
                    if (proj.hasMetadata("proj_as")) {
                        ArmorStand armorStand = (ArmorStand) proj.getMetadata("proj_as");
                        armorStand.teleport(proj.getLocation().clone().subtract(0, 1.7, 0));
                    }
                })
                .onBlockCollision((proj, block) -> {
                    if (proj.hasMetadata("proj_as")) {
                        ArmorStand armorStand = (ArmorStand) proj.getMetadata("proj_as");
                        armorStand.remove();
                        proj.destroy();
                        block.getWorld().strikeLightning(block.getLocation());
                    }
                });

        World world = location.getWorld();
        if (world == null) {
            return bottle;
        }

        ArmorStand armorStand = (ArmorStand) world.spawnEntity(location.clone().subtract(0, 1.7, 0),
                EntityType.ARMOR_STAND);
        armorStand.setInvulnerable(true);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new ItemStack(Material.GLASS_BOTTLE));
        }

        bottle.setMetaData("proj_as", armorStand);
        return bottle;
    }
}
