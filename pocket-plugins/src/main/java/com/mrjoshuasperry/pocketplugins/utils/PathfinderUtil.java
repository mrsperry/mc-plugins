package com.mrjoshuasperry.pocketplugins.utils;

import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.Pathfinder.PathResult;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

public class PathfinderUtil {
  public static void pathTo(Mob mob, Location target, double threshold, IPathfindCallback callback) {
    PathfinderUtil.pathTo(mob, target, threshold, 1, false, callback);
  }

  public static void pathTo(Mob mob, Location target, double threshold, double speed, boolean continous,
      IPathfindCallback callback) {
    Pathfinder pathfinder = mob.getPathfinder();
    PathResult pathResult = pathfinder.findPath(target);

    if (pathResult == null || !pathResult.canReachFinalPoint()) {
      pathfinder.stopPathfinding();
      callback.execute(mob, false);
    }

    pathfinder.moveTo(target, speed);

    final Date start = new Date();
    final BukkitTask[] taskHolder = new BukkitTask[1];
    taskHolder[0] = Bukkit.getScheduler().runTaskTimer(PocketPlugins.getInstance(), () -> {

      if (mob.getLocation().distance(target) <= threshold) {
        callback.execute(mob, true);
        Bukkit.getLogger().info("Execute 1");
        taskHolder[0].cancel();
        return;
      }

      if (continous) {
        Date current = new Date();
        long elapsedTicks = (current.getTime() - start.getTime()) / (1000 * 20);

        if (elapsedTicks % 5 == 0) {
          PathResult updatedResult = pathfinder.findPath(target);
          if (updatedResult == null || !updatedResult.canReachFinalPoint()) {
            pathfinder.stopPathfinding();
            callback.execute(mob, false);
            Bukkit.getLogger().info("Execute 2");
            taskHolder[0].cancel();
          }

          pathfinder.moveTo(target, speed);
        }
      }
    }, 0, 1);
  }
}
