package com.mrjoshuasperry.pocketplugins.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.Pathfinder.PathResult;
import com.mrjoshuasperry.mcutils.classes.Pair;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class PathfinderUtil {
  private JavaPlugin plugin;
  private BukkitScheduler scheduler;
  private int taskId;

  public PathfinderUtil() {
    this.plugin = PocketPlugins.getInstance();
    this.scheduler = plugin.getServer().getScheduler();
    this.taskId = -1;
  }

  public void pathTo(Mob mob, Location target, double speed, Consumer<Boolean> callback) {
    Pathfinder pathfinder = mob.getPathfinder();
    PathResult pathResult = pathfinder.findPath(target);

    if (!pathResult.canReachFinalPoint()) {
      Bukkit.getLogger().info("Cannot reach final point " + mob.getLocation() + " : " + target);
      callback.accept(false);
      return;
    }

    pathfinder.moveTo(target, speed);

    Consumer<Boolean> onPathComplete = (Boolean reachedTarget) -> {
      this.scheduler.cancelTask(this.taskId);
      pathfinder.stopPathfinding();
      callback.accept(reachedTarget);
    };

    this.taskId = this.scheduler.runTaskTimer(this.plugin, () -> {
      // Ensure the mob is alive, not despawned, ect
      if (!mob.isValid()) {
        Bukkit.getLogger().info("Mob is not valid");
        onPathComplete.accept(false);
        return;
      }

      // The mob has reached the target
      if (mob.getLocation().distance(target) < 1) {
        onPathComplete.accept(true);
        return;
      }

      PathResult currentPath = pathfinder.getCurrentPath();
      if (currentPath == null) {
        Bukkit.getLogger().info("Current path is null");
        onPathComplete.accept(false);
        return;
      }

      // Ensure the mob can reach the final point (e.g. if it was blocked by a wall
      // after moving)
      if (!currentPath.canReachFinalPoint()) {
        Bukkit.getLogger().info("Cannot reach target");
        onPathComplete.accept(false);
        return;
      }

      // Ensure the final point of the path is the target
      if (!currentPath.getFinalPoint().equals(target.getBlock().getLocation())) {
        Bukkit.getLogger().info("Target is no longer final point");
        onPathComplete.accept(false);
        return;
      }
    }, 0, 1).getTaskId();
  }

  public void pathToMultiple(Mob mob, double speed, List<Pair<Location, Function<Boolean, Boolean>>> targets,
      Runnable interrupt) {
    if (targets.isEmpty()) {
      return;
    }

    Pair<Location, Function<Boolean, Boolean>> target = targets.remove(0);

    this.pathTo(mob, target.getKey(), speed, (Boolean success) -> {
      Boolean result = target.getValue().apply(success);
      if (!result) {
        interrupt.run();
        return;
      }

      this.pathToMultiple(mob, speed, targets, interrupt);
    });
  }
}
