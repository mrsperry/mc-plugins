package com.mrjoshuasperry.mcutils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.mcutils.classes.Pair;

public class VectorUtils {
    public static boolean hasLOS(Location from, Location to) {
        Pair<Location, Location> fixed = getFixedPoints(from, to);
        from = fixed.getKey();
        to = fixed.getValue();
        double step = 0.1;
        Location pos = from.clone();
        Vector direction = getDirection(from, to);
        double dist = direction.length();
        direction = direction.normalize().multiply(0.1);

        for (double length = 0; length + step < dist; length += step) {
            pos = pos.add(direction);
            if (!pos.getBlock().getType().equals(Material.AIR)) {
                return false;
            }
        }
        return true;
    }

    public static Vector getDirection(Location from, Location to) {
        return to.toVector().subtract(from.toVector());
    }

    private static Pair<Location, Location> getFixedPoints(Location loc1, Location loc2) {
        Location temp1 = loc1.clone().add(0.5, 0.5, 0.5);
        Location temp2 = loc2.clone().add(0.5, 0.5, 0.5);

        if (temp1.getX() < temp2.getX()) {
            temp1.setX(temp1.getX() + 0.5);
            temp2.setX(temp2.getX() - 0.5);
        } else if (temp1.getX() > temp2.getX()) {
            temp1.setX(temp1.getX() - 0.5);
            temp2.setX(temp2.getX() + 0.5);
        }

        if (temp1.getZ() < temp2.getZ()) {
            temp1.setZ(temp1.getZ() + 0.5);
            temp2.setZ(temp2.getZ() - 0.5);
        } else if (temp1.getZ() > temp2.getZ()) {
            temp1.setZ(temp1.getZ() - 0.5);
            temp2.setZ(temp2.getZ() + 0.5);
        }

        return new Pair<>(temp1, temp2);
    }
}
