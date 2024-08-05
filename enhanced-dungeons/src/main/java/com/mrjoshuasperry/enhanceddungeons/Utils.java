package com.mrjoshuasperry.enhanceddungeons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.logging.Level;

public class Utils {
    /**
     * Shorthand for logging with plugin prefix
     * @param level The message level identifier
     * @param message The message to log
     */
    public static void log(final Level level, final String message) {
        Main.getInstance().getLogger().log(level, message);
    }

    /**
     * Converts an ID string into a display string
     * @param id The ID string to convert
     * @return The formatted ID string
     */
    public static String idToDisplay(final String id) {
        String result = "";
        for (final String piece : id.split("-")) {
            result = result.concat(piece.substring(0, 1).toUpperCase() + piece.substring(1)) + " ";
        }

        return result.trim();
    }

    /**
     * Converts a display string into an ID string
     * @param display The display string to convert
     * @return The formatted display string
     */
    public static String displayToID(final String display) {
        String result = "";
        for (final String piece : display.split(" ")) {
            result = result.concat(piece + "-");
        }

        return result.toLowerCase().substring(0, result.length() - 1);
    }

    /**
     * Centers a block location by adding or subtracting 0.5 from each coordinate
     * @param location The original location
     * @return The centered location
     */
    public static Location centerLocation(final Location location) {
        return location.clone().add(
                location.getX() >= 0 ? 0.5 : -0.5,
                0.5,
                location.getZ() >= 0 ? 0.5 : -0.5);
    }

    /**
     * Normalizes a location coordinate to a block coordinate
     * @param location The location to normalize
     * @return The normalized block coordinate location
     */
    public static Location locationToBlockCoordinates(final Location location) {
        // Normalize the location
        return location.clone().subtract(
                location.getBlockX() < 0 ? 1 : 0,
                0,
                location.getBlockZ() < 0 ? 1 : 0);
    }

    /**
     * Compares two locations with a two block center axis
     * @param location1 The first location
     * @param location2 The second location
     * @return If the locations are the same block
     */
    public static boolean compareLocations(final Location location1, final Location location2) {
        final Location copy = Utils.locationToBlockCoordinates(location1);

        // Check if the locations point to the same block
        return copy.getBlockX() == location2.getBlockX()
                && copy.getBlockY() == location2.getBlockY()
                && copy.getBlockZ() == location2.getBlockZ();
    }
}