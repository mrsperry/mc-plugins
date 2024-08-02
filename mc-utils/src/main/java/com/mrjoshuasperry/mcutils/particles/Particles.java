package com.mrjoshuasperry.mcutils.particles;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.mcutils.VectorUtils;

public class Particles {
    private static Random random = new Random();

    public static void drawLine(Particle particle, Location from, Location to, double stepSize) {
        drawLine(particle, from, to, stepSize, 1, 0, 0, 0, 0);
    }

    public static void drawLine(Particle particle, Location from, Location to, double stepSize, int count, double offX,
            double offY, double offZ, double extra) {
        Vector direction = VectorUtils.getDirection(from, to);
        double dist = direction.length();
        direction = direction.normalize().multiply(stepSize);
        Location pos = from.clone();

        for (double magnitude = 0; magnitude + stepSize <= dist; magnitude += stepSize) {
            pos.add(direction);
            pos.getWorld().spawnParticle(particle, pos, count, offX, offY, offZ, extra);
        }
    }

    public static void drawPlane(Particle particle, Location from, Location to, double stepSize) {
        drawPlane(particle, from, to, stepSize, 1, 0, 0, 0, 0);
    }

    public static void drawPlane(Particle particle, Location from, Location to, double stepSize, int count, double offX,
            double offY, double offZ, double extra) {
        if (from.getY() == to.getY()) {
            drawHorizontalPlane(particle, from.clone(), to.clone(), stepSize, count, offX, offY, offZ, extra);
        } else {
            drawVerticalPlane(particle, from.clone(), to.clone(), stepSize, count, offX, offY, offZ, extra);
        }
    }

    private static void drawHorizontalPlane(Particle particle, Location from, Location to, double stepSize, int count,
            double offX, double offY, double offZ, double extra) {
        double minX = (from.getX() < to.getX()) ? from.getX() : to.getX();
        double maxX = (from.getX() > to.getX()) ? from.getX() : to.getX();

        for (double x = minX; x <= maxX; x += stepSize) {
            from.setX(x);
            to.setX(x);
            drawLine(particle, from.clone(), to.clone(), stepSize, count, offX, offY, offZ, extra);
        }
    }

    private static void drawVerticalPlane(Particle particle, Location from, Location to, double stepSize, int count,
            double offX, double offY, double offZ, double extra) {
        double minY = (from.getY() < to.getY()) ? from.getY() : to.getY();
        double maxY = (from.getY() > to.getY()) ? from.getY() : to.getY();

        for (double y = minY; y <= maxY; y += stepSize) {
            from.setY(y);
            to.setY(y);
            drawLine(particle, from.clone(), to.clone(), stepSize, count, offX, offY, offZ, extra);
        }
    }

    public static void drawCube(Particle particle, Location from, Location to, double stepSize) {
        drawCube(particle, from, to, stepSize, 1, 0, 0, 0, 0);
    }

    public static void drawCube(Particle particle, Location from, Location to, double stepSize, int count, double offX,
            double offY, double offZ, double extra) {
        double minY = (from.getY() < to.getY()) ? from.getY() : to.getY();
        double maxY = (from.getY() > to.getY()) ? from.getY() : to.getY();
        Location fromTemp = from.clone();
        Location toTemp = to.clone();

        for (double y = minY; y < maxY; y += stepSize) {
            fromTemp.setY(y);
            toTemp.setY(y);
            drawHorizontalPlane(particle, fromTemp.clone(), toTemp.clone(), stepSize, count, offX, offY, offZ, extra);
        }
    }

    public static void drawRandomSphere(Particle particle, Location center, double radius, boolean fill, int count) {
        drawRandomSphere(particle, center, radius, fill, count, 0, 0, 0, 0);
    }

    public static void drawRandomSphere(Particle particle, Location center, double radius, boolean fill, int count,
            double offX, double offY, double offZ, double extra) {
        drawRoundedObject(center, radius, fill, count, (point) -> center.getWorld().spawnParticle(particle, point.x,
                point.y, point.z, 0, offX, offY, offZ, extra));
    }

    public static void drawRandomCircle(Particle particle, Location center, double radius, boolean fill, int count) {
        drawRandomCircle(particle, center, radius, fill, count, 0, 0, 0, 0);
    }

    public static void drawRandomCircle(Particle particle, Location center, double radius, boolean fill, int count,
            double offX, double offY, double offZ, double extra) {
        drawRoundedObject(center, radius, fill, count, (point) -> center.getWorld().spawnParticle(particle, point.x,
                center.getY(), point.z, 0, offX, offY, offZ, extra));
    }

    public static void drawRandomCylinder(Particle particle, Location center, double radius, boolean fill,
            double height, int count) {
        drawRandomCylinder(particle, center, radius, fill, height, count, 0, 0, 0, 0);
    }

    public static void drawRandomCylinder(final Particle particle, final Location center, double radius, boolean fill,
            double height, int count, final double offX, final double offY, final double offZ, final double extra) {
        double y = center.getY() + (random.nextDouble() * height);
        drawRoundedObject(center, radius, fill, count,
                (point) -> center.getWorld().spawnParticle(particle, point.x, y, point.z, 0, offX, offY, offZ, extra));
    }

    private static void drawRoundedObject(Location center, double radius, boolean fill, int count,
            RoundedShapeCallback task) {
        for (int current = 0; current < count; current++) {
            CircleVars point = CircleVars.getRandom(center, radius, fill);
            task.execute(point);
        }
    }

    private interface RoundedShapeCallback {
        void execute(CircleVars point);
    }

    private static class CircleVars {
        public double r, x, y, z;

        public static CircleVars getRandom(Location center, double radius, boolean fill) {
            CircleVars result = new CircleVars();
            double theta = random.nextDouble() * Math.PI * 2;
            double alpha = random.nextDouble() * Math.PI;
            result.r = fill ? random.nextDouble() * radius : radius;
            result.x = center.getX() + (result.r * Math.cos(theta) * Math.sin(alpha));
            result.y = center.getY() + (result.r * Math.sin(theta) * Math.sin(alpha));
            result.z = center.getZ() + (result.r * Math.cos(alpha));

            return result;
        }
    }
}
