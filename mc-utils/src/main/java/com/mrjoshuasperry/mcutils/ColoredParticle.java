package com.mrjoshuasperry.mcutils;

import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Utility Class to draw shapes with particles
 */
public class ColoredParticle {
    /*
     * CONSTANT ORIENTATION THROUGHOUT CLASS
     *
     * LENGTH = X
     * WIDTH = Z
     * HEIGHT = Y
     */

    private ColoredParticle() {
    }

    /**
     * Draws a square with a particle given a start and end location
     *
     * @param start    Start location of the shape
     * @param end      End location of the shape
     * @param particle Particle to be used to draw the shape
     * @param spacing  The distance to the next particle (0.2 = 5 per block)
     * @param data     Any special options for the particle
     * @param <T>      Type of the options for the particle
     */
    public static <T> void displaySquare(Location start, Location end, Particle particle, double spacing, T data) {
        double length = Math.abs(start.getX() - end.getX());
        double width = Math.abs(start.getZ() - end.getZ());
        double height = Math.abs(start.getY() - end.getY());

        Location center = end.clone().subtract(start.clone()).multiply(0.5);
        center = center.add(start.clone());

        displaySquare(center, particle, length, width, height, spacing, data);
    }

    /**
     * Draws a square with a particle given a center location and dimensions
     *
     * @param center   Center of the square shape to be drawn with particles
     * @param particle Particle to be used to draw the shape
     * @param length   Length is in the X Axis
     * @param width    Width is in the Z Axis
     * @param height   Height is in the Y Axis
     * @param spacing  The distance to the next particle (0.2 = 5 per block)
     * @param data     Any special options for the particle
     * @param <T>      Type of the options for the particle
     */
    public static <T> void displaySquare(Location center, Particle particle, double length, double width, double height,
            double spacing, T data) {
        for (double yOffset = -(height / 2.0); yOffset <= (height / 2); yOffset += spacing) {
            for (double xOffset = -(length / 2.0); xOffset <= (length / 2.0); xOffset += spacing) {
                for (double zOffset = -(width / 2.0); zOffset <= (width / 2.0); zOffset += spacing) {
                    center.getWorld().spawnParticle(
                            particle,
                            center.getX() + xOffset,
                            center.getY() + yOffset,
                            center.getZ() + zOffset,
                            1,
                            data);
                }
            }
        }
    }

    /**
     * Draws a sphere with a particle using Fibonacci Spiral Sphere method
     * 
     * @param center        Center of the sphere
     * @param particle      Particle to draw the sphere
     * @param radius        Radius of the sphere
     * @param particleCount Number of particles that make up the sphere
     * @param data          Any special options for the particle
     * @param <T>           Type of the options for the particle
     */
    public static <T> void displaySphere(Location center, Particle particle, double radius, int particleCount, T data) {
        for (int particles = 1; particles < particleCount; particles++) {
            double lat = Math.asin(-1.0 + 2.0 * particles / (particleCount + 1));
            double lon = goldenAngle() * particles;

            double x = (radius * Math.cos(lon) * Math.cos(lat)) + center.getX();
            double y = (radius * Math.sin(lon) * Math.cos(lat)) + center.getY();
            double z = (radius * Math.sin(lat)) + center.getZ();
            center.getWorld().spawnParticle(particle, x, y, z, 1, data);
        }
    }

    /**
     * Golden Ratio
     * 
     * @return A double of the golden ratio
     */
    private static double goldenRatio() {
        return (Math.sqrt(5) + 1.0) / 2.0;
    }

    /**
     * Golden Angle
     * 
     * @return A double of the golden angle
     */
    private static double goldenAngle() {
        return (2.0 - goldenRatio()) * (2.0 * Math.PI);
    }

}
