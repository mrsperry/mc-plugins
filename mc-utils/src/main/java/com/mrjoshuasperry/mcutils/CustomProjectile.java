package com.mrjoshuasperry.mcutils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CustomProjectile extends BukkitRunnable {
    private final JavaPlugin plugin;
    private int runnableID;
    private double radius;
    private int lifespan;
    private Vector acceleration;
    private double maxSpeed;
    private double gravity;
    private double downForce;
    private Location location;
    private final Vector direction;
    private DisplayParticle display;
    private EntityCollision entityCollision;
    private BlockCollision blockCollision;
    private final Map<String, Object> metaData;

    public CustomProjectile(JavaPlugin plugin, Location location, Vector direction, double radius, int lifespan) {
        this.plugin = plugin;
        this.location = location;
        this.direction = direction.clone().normalize();
        this.radius = radius;
        this.lifespan = lifespan;
        this.gravity = 0;
        this.downForce = 0;
        this.display = projectile -> {
        };
        this.entityCollision = (projectile, entities) -> {
        };
        this.blockCollision = (projectile, block) -> {
        };
        this.metaData = new HashMap<>();
    }

    public CustomProjectile startingSpeed(double startingSpeed) {
        this.direction.multiply(startingSpeed);
        return this;
    }

    public CustomProjectile addGravity(double gravity) {
        this.gravity = gravity;
        return this;
    }

    public CustomProjectile addAcceleration(double acceleration, double maxSpeed) {
        this.acceleration = this.direction.clone().normalize().multiply(acceleration);
        this.maxSpeed = maxSpeed;
        return this;
    }

    public CustomProjectile onDisplay(DisplayParticle display) {
        this.display = display;
        return this;
    }

    public CustomProjectile onBlockCollision(BlockCollision collision) {
        this.blockCollision = collision;
        return this;
    }

    public CustomProjectile onEntityCollision(EntityCollision collision) {
        this.entityCollision = collision;
        return this;
    }

    public void launch() {
        this.runnableID = this.runTaskTimer(this.plugin, 0, 1).getTaskId();
    }

    public void update() {

        if (this.direction.clone().add(this.acceleration).length() <= this.maxSpeed) {
            this.direction.add(this.acceleration);
        }
        this.downForce -= this.gravity;

        this.location.add(this.direction);
        this.location.add(0, this.downForce, 0);
        Block block = this.location.getBlock();
        Collection<Entity> entities = this.location.getWorld().getNearbyEntities(this.location, this.radius,
                this.radius, this.radius);
        if (!entities.isEmpty()) {
            this.entityCollision.execute(this, entities);
        }
        if (!block.getType().equals(Material.AIR)) {
            this.blockCollision.execute(this, block);
        }
    }

    public void destroy() {
        Bukkit.getScheduler().cancelTask(this.runnableID);
    }

    public void run() {
        if (this.lifespan <= 0) {
            this.destroy();
            return;
        }

        this.display.execute(this);
        this.update();
        this.lifespan--;
    }

    public boolean hasMetadata(String key) {
        return this.metaData.containsKey(key);
    }

    public Object getMetadata(String key) {
        return this.metaData.get(key);
    }

    public void setMetaData(String key, Object value) {
        this.metaData.put(key, value);
    }

    public Location getLocation() {
        return this.location.clone();
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getRadius() {
        return this.radius;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    public int getLifespan() {
        return this.lifespan;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public interface EntityCollision {
        void execute(CustomProjectile projectile, Collection<Entity> hit);
    }

    public interface BlockCollision {
        void execute(CustomProjectile projectile, Block block);
    }

    public interface DisplayParticle {
        void execute(CustomProjectile projectile);
    }
}
