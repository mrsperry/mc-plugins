package com.mrjoshuasperry.mcutils;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class LocationUtils {
    private static Random random = new Random();

    /**
     * Checks if a given location is safe for a player to stand on
     * <br>
     * <br>
     * This checks if the block below the location is <b>not</b> passable and
     * <b>not</b> water or lava.
     * <br>
     * <br>
     * Additionally, it will check the block on the location and the block above (to
     * allow a two block tall player) to see if they <b>are</b> passable and
     * <b>not</b> lava.
     * <br>
     * <br>
     * The block on the location <b>may be</b> water however the block at eye level
     * <b>must</b> be air.
     * 
     * @param location The location to check
     * @return If the location is safe for a player to stand on
     */
    private static boolean isLocationSafe(final Location location) {
        final Block block = location.getBlock();
        // Clone of the origin location; using `.add()` and `.subtract()` modify the
        // variable
        final Location checker = location.clone();

        // Check if the feet location is a valid block (water is allowed)
        if (!block.isPassable() || block.getType() == Material.LAVA) {
            return false;
        }

        // Check if the block below the origin is a valid block
        final Block below = checker.subtract(0, 1, 0).getBlock();
        final Material belowType = below.getType();
        if (below.isPassable() || belowType == Material.WATER || below.getType() == Material.LAVA) {
            return false;
        }

        // Check if the block above the origin is a valid block
        final Block eyes = checker.add(0, 2, 0).getBlock();
        final Material eyeType = eyes.getType();
        return eyes.isPassable() && eyeType != Material.WATER && eyeType != Material.LAVA;
    }

    /**
     * Gets a random safe location within the given radius
     * 
     * @param location The origin location
     * @param radius   How many blocks on each axis (negative and positive axis as
     *                 well) should be searched for a new location
     * @param tries    The number of tries before giving up
     * @return A random safe location within the given radius; or null if the number
     *         of tries ran out or a safe location could not be found
     */
    public static Location getRandomSafeLocation(final Location location, final double radius, final int tries) {
        return LocationUtils.getRandomSafeLocation(location, radius, radius, radius, tries);
    }

    /**
     * Gets a random safe location within the given radii
     * 
     * @param location The origin location
     * @param xAmount  The maximum number of blocks on the X axis to search
     * @param yAmount  The maximum number of blocks on the Y axis to search
     * @param zAmount  The maximum number of blocks on the Z axis to search
     * @param tries    The number of tries before giving up
     * @return A random safe location within the given radii; or null if the number
     *         of tries ran out or a safe location could not be found
     */
    public static Location getRandomSafeLocation(final Location location, final double xAmount, final double yAmount,
            final double zAmount, final int tries) {
        for (int totalTries = 0; totalTries < tries; totalTries++) {
            // Get a new random position
            final Location target = LocationUtils.randomize(location, xAmount, yAmount, zAmount);

            // Check if the location is safe
            if (LocationUtils.isLocationSafe(target)) {
                return target;
            }
        }

        return null;
    }

    /**
     * Gives a new location within the offset amount
     * 
     * @param location The origin location
     * @param amount   The amount of space the new location can move on the X, Y and
     *                 Z axis
     * @return The new random location
     */
    public static Location randomize(Location location, double amount) {
        return LocationUtils.randomize(location, amount, amount, amount);
    }

    /**
     * Gives a new location within the offset amounts
     * 
     * @param location The origin location
     * @param xAmount  The amount of space the new location can move of the X axis
     * @param yAmount  The amount of space the new location can move of the Y axis
     * @param zAmount  The amount of space the new location can move of the Z axis
     * @return The new random location
     */
    public static Location randomize(Location location, double xAmount, double yAmount, double zAmount) {
        Random random = LocationUtils.random;
        return new Location(location.getWorld(),
                location.getX() - xAmount + (random.nextDouble() * (xAmount * 2)),
                location.getY() - yAmount + (random.nextDouble() * (yAmount * 2)),
                location.getZ() - zAmount + (random.nextDouble() * (zAmount * 2)));
    }

    /**
     * Sets the X, Y, and Z coordinates of a given location
     * 
     * @param location The original location
     * @param x        X coordinate to be set
     * @param y        Y coordinate to be set
     * @param z        Z coordinate to be set
     */
    public static void setXYZ(final Location location, final double x, final double y, final double z) {
        location.setX(x);
        location.setY(y);
        location.setZ(z);
    }
}
