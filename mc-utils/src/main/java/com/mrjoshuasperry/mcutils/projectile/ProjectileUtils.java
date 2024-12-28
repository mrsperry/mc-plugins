package com.mrjoshuasperry.mcutils.projectile;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ProjectileUtils {
    /**
     * Determines the bounce velocity
     * 
     * @param direction the direction vector of collision
     * @param velocity  the velocity the entity is currently moving
     * @return
     */
    public static Vector getBounceVelocity(Vector direction, Vector velocity) {
        float x = direction.getX() == 0 ? 1 : -1;
        float y = direction.getY() == 0 ? 1 : -1;
        float z = direction.getZ() == 0 ? 1 : -1;

        Vector bounceScalar = new Vector(x, y, z);
        return velocity.multiply(bounceScalar);
    }

    /**
     * Utility to create an invisible armor stand with an itemstack on its head
     * 
     * @param location Location where the eyes of the armor stand should be
     * @param headItem Itemstack that should be on the armorstands head
     * @param small    If the armorstand should be the small variant
     * @return
     */
    public static ArmorStand spawnArmorStand(Location location, ItemStack headItem, boolean small) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location.clone(), EntityType.ARMOR_STAND);
        armorStand.setSmall(small);
        armorStand.setVisible(false);
        armorStand.setCollidable(false);
        armorStand.setInvulnerable(true);
        armorStand.getEquipment().setHelmet(headItem);
        translateArmorStandPosition(armorStand);
        return armorStand;
    }

    /**
     * Moves the armor stand down so its eye location is at its current position
     * 
     * @param armorStand
     */
    public static void translateArmorStandPosition(ArmorStand armorStand) {
        final Location offset = armorStand.getEyeLocation().clone().subtract(armorStand.getLocation());
        final Location newLoc = armorStand.getLocation().clone().subtract(offset);
        armorStand.teleport(newLoc);
    }

    public static void teleportAndTranslateArmorStand(ArmorStand armorStand, Location location) {
        final Location offset = armorStand.getEyeLocation().clone().subtract(armorStand.getLocation());
        final Location newLoc = location.clone().subtract(offset);
        armorStand.teleport(newLoc);
    }
}
