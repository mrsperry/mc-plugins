package com.mrjoshuasperry.mcutils.projectile;

import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;

public class ProjectileEvents {
    public interface ProjectileUpdateEvent {
        void execute(BaseProjectile projectile);
    }

    public interface ProjectileDrawEvent {
        void execute(BaseProjectile projectile, Location previousLocation);
    }

    public interface CollisionEvent<T> {
        void execute(BaseProjectile projectile, T collidedWith, RayTraceResult result, Location hitLocation);
    }

    public interface ProjectileDecayEvent {
        void execute(BaseProjectile projectile);
    }
}
