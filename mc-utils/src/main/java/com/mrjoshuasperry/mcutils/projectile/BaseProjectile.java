package com.mrjoshuasperry.mcutils.projectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.mcutils.projectile.ProjectileEvents.CollisionEvent;
import com.mrjoshuasperry.mcutils.projectile.ProjectileEvents.ProjectileDecayEvent;
import com.mrjoshuasperry.mcutils.projectile.ProjectileEvents.ProjectileDrawEvent;
import com.mrjoshuasperry.mcutils.projectile.ProjectileEvents.ProjectileUpdateEvent;

public class BaseProjectile extends BukkitRunnable {
    // Projectile Properties
    private final JavaPlugin plugin;
    private Location position;
    private float radius;
    private float maxSpeed;
    private int lifespan;
    private int age = 0;
    private Map<String, Object> metaData;
    boolean shouldCancel = false;

    private Vector velocity;
    private Vector acceleration;

    List<ProjectileUpdateEvent> updateEventList;
    List<ProjectileDrawEvent> drawEventList;
    List<CollisionEvent<Block>> blockHitEventList;
    List<CollisionEvent<Entity>> entityHitEventList;
    List<ProjectileDecayEvent> decayEventList;

    public BaseProjectile(JavaPlugin plugin, float radius, int lifespan, float maxSpeed) {
        this.plugin = plugin;
        this.radius = radius;
        this.maxSpeed = maxSpeed;
        this.lifespan = lifespan;

        this.velocity = new Vector(0, 0, 0);
        this.acceleration = new Vector(0, 0, 0);

        this.updateEventList = new ArrayList<>();
        this.drawEventList = new ArrayList<>();
        this.blockHitEventList = new ArrayList<>();
        this.entityHitEventList = new ArrayList<>();
        this.decayEventList = new ArrayList<>();

        this.metaData = new HashMap<>();
    }

    public void applyForce(Vector force) {
        this.acceleration.add(force);
    }

    public void updateVelocity() {
        this.velocity.add(this.acceleration);
        if (this.velocity.length() > maxSpeed) {
            this.velocity.normalize().multiply(maxSpeed);
        }
        this.position.add(this.velocity);
        this.acceleration.zero();
    }

    public Vector getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public void addMetaData(String key, Object value) {
        this.metaData.put(key, value);
    }

    public boolean hasMetaData(String key) {
        return this.metaData.containsKey(key);
    }

    public Object getMetaData(String key) {
        if (this.hasMetaData(key)) {
            return this.metaData.get(key);
        }
        return null;
    }

    public void setPosition(Location location) {
        this.position = location.clone();
    }

    public Location getPosition() {
        return this.position;
    }

    public double getRadius() {
        return this.radius;
    }

    public void queueCancel() {
        this.shouldCancel = true;
    }

    public int getAge() {
        return this.age;
    }

    private void tick() {
        Location prev = this.position.clone();
        for (ProjectileUpdateEvent listener : updateEventList) {
            listener.execute(this);
        }
        this.updateVelocity();
        performRayTrace(prev);

        for (ProjectileDrawEvent listener : drawEventList) {
            listener.execute(this, prev);
        }

        if (this.shouldCancel) {
            this.cleanup();
        }
    }

    private void performRayTrace(Location prev) {
        Vector dir = this.position.clone().subtract(prev).toVector();
        double dist = dir.length();
        dir = dir.normalize();

        RayTraceResult rayTraceResult = this.position.getWorld().rayTrace(prev, dir, dist, FluidCollisionMode.NEVER,
                true, this.radius, null);

        if (rayTraceResult != null) {
            Location hitPosition = rayTraceResult.getHitPosition().toLocation(this.getPosition().getWorld());

            if (!entityHitEventList.isEmpty() && rayTraceResult.getHitEntity() != null) {
                for (CollisionEvent<Entity> entityListener : this.entityHitEventList) {
                    entityListener.execute(this, rayTraceResult.getHitEntity(), rayTraceResult, hitPosition);
                }
            }

            if (!blockHitEventList.isEmpty() && rayTraceResult.getHitBlock() != null) {
                for (CollisionEvent<Block> blockListener : this.blockHitEventList) {
                    blockListener.execute(this, rayTraceResult.getHitBlock(), rayTraceResult, hitPosition);
                }
            }
        }
    }

    public void addUpdateListener(ProjectileUpdateEvent listener) {
        updateEventList.add(listener);
    }

    public void addProjectileDrawListener(ProjectileDrawEvent listener) {
        drawEventList.add(listener);
    }

    public void addProjectileHitBlockListener(CollisionEvent<Block> listener) {
        blockHitEventList.add(listener);
    }

    public void addProjectileHitEntityListener(CollisionEvent<Entity> listener) {
        entityHitEventList.add(listener);
    }

    public void addProjectileDecayListener(ProjectileDecayEvent listener) {
        decayEventList.add(listener);
    }

    public void launch(Location position) {
        this.position = position.clone();
        this.runTaskTimer(this.plugin, 0, 1);
    }

    private void cleanup() {
        this.shouldCancel = false;
        this.age = 0;

        for (ProjectileDecayEvent listener : decayEventList) {
            listener.execute(this);
        }

        this.cancel();
    }

    @Override
    public void run() {
        this.tick();

        this.age++;
        if (lifespan != -1 && this.age > this.lifespan) {
            this.cleanup();
        }
    }
}
