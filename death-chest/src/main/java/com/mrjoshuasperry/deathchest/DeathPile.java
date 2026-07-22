package com.mrjoshuasperry.deathchest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.util.Vector;

/**
 * Pure geometry and serialization for a death pile — no live world required, so all
 * of it is unit-testable. A pile is a set of stacks arranged into stacked rings that
 * slowly orbit a center point; {@link DeathPileManager} owns the spawned entities and
 * drives the rotation.
 */
public final class DeathPile {
    /** Stacks per ring before a new ring is started. */
    static final int RING_CAPACITY = 8;
    /** Horizontal radius of the widest (equatorial) ring, in blocks. */
    static final double EQUATOR_RADIUS = 0.65;
    /** Vertical half-extent of the sphere from its center to a pole ring, in blocks. */
    static final double VERTICAL_RADIUS = 0.55;
    /** Height of the sphere's center (the equatorial ring) above the death block. */
    static final double CENTER_HEIGHT = 1.4;
    /** How far toward the poles the outermost rings sit; smaller = more column-like. */
    static final double MAX_LATITUDE = Math.toRadians(60);

    /** One stack's fixed place in the pile: which ring (via height) and where on it. */
    public record Slot(double radius, double height, double theta0) {
    }

    /** An orbit blob decoded back into its center and slot. */
    public record Orbit(double centerX, double centerY, double centerZ, Slot slot) {
    }

    private DeathPile() {
    }

    /**
     * Assigns {@code count} stacks to slots arranged on a sphere: the first ring is the
     * equator (widest, at {@link #CENTER_HEIGHT}) and each further ring is pushed
     * alternately above and below it, inset toward the poles so the pile reads as a
     * sphere rather than a column. Each ring holds up to {@link #RING_CAPACITY} stacks,
     * spread evenly around it.
     */
    public static List<Slot> layout(int count) {
        List<Slot> slots = new ArrayList<>();

        int ringCount = (count + RING_CAPACITY - 1) / RING_CAPACITY;
        int maxLevel = ringCount / 2;

        int placed = 0;
        for (int ring = 0; ring < ringCount; ring++) {
            double latitude = maxLevel == 0 ? 0 : (double) signedLevel(ring) / maxLevel * MAX_LATITUDE;
            double radius = EQUATOR_RADIUS * Math.cos(latitude);
            double height = CENTER_HEIGHT + VERTICAL_RADIUS * Math.sin(latitude);

            int inThisRing = Math.min(RING_CAPACITY, count - placed);
            for (int index = 0; index < inThisRing; index++) {
                double theta0 = 2.0 * Math.PI * index / inThisRing;
                slots.add(new Slot(radius, height, theta0));
            }

            placed += inThisRing;
        }

        return slots;
    }

    /** Ring fill order as signed levels around the equator: 0, +1, -1, +2, -2, ... */
    private static int signedLevel(int ring) {
        int magnitude = (ring + 1) / 2;
        return ring % 2 == 1 ? magnitude : -magnitude;
    }

    /**
     * The offset from a pile's center for a slot at the given rotation phase. Adding
     * this to the center gives the item's world position.
     */
    public static Vector position(Slot slot, double phase) {
        double angle = slot.theta0() + phase;
        return new Vector(slot.radius() * Math.cos(angle), slot.height(), slot.radius() * Math.sin(angle));
    }

    /**
     * Packs a member's center and slot into the byte blob stored on its display, so a
     * pile can be reconstructed after a server restart from the entity alone.
     */
    public static byte[] writeOrbit(double centerX, double centerY, double centerZ, Slot slot) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeDouble(centerX);
            out.writeDouble(centerY);
            out.writeDouble(centerZ);
            out.writeDouble(slot.radius());
            out.writeDouble(slot.height());
            out.writeDouble(slot.theta0());
        }

        return bytes.toByteArray();
    }

    /** Inverse of {@link #writeOrbit}. */
    public static Orbit readOrbit(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            double centerX = in.readDouble();
            double centerY = in.readDouble();
            double centerZ = in.readDouble();
            Slot slot = new Slot(in.readDouble(), in.readDouble(), in.readDouble());
            return new Orbit(centerX, centerY, centerZ, slot);
        }
    }
}
