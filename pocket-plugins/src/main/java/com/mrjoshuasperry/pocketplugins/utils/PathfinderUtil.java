package com.mrjoshuasperry.pocketplugins.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.Pathfinder.PathResult;
import com.google.common.collect.Lists;
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

  protected void runPathingChecks(Mob mob, Location target, Consumer<Boolean> onPathComplete) {
    Pathfinder pathfinder = mob.getPathfinder();

    // The mob has reached the target
    if (mob.getLocation().distance(target) < 1) {
      onPathComplete.accept(true);
      return;
    }

    PathResult currentPath = pathfinder.getCurrentPath();
    if (currentPath == null) {
      onPathComplete.accept(false);
      return;
    }

    // Ensure the final point of the path is the target
    List<Location> validLocations = Lists.newArrayList(target.getBlock().getLocation());
    List<BlockFace> validFaces = Lists.newArrayList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
        BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    Block block = target.getBlock();

    for (BlockFace face : validFaces) {
      Block relativeBlock = block.getRelative(face);

      if (relativeBlock.isPassable()) {
        validLocations.add(relativeBlock.getLocation());
      }
    }

    if (!validLocations.contains(currentPath.getFinalPoint())) {
      onPathComplete.accept(false);
      return;
    }
  }

  public void pathToExact(Mob mob, Location target, double speed, Consumer<Boolean> callback) {
    this.pathToExact(mob, target, speed, callback, null);
  }

  public void pathToExact(Mob mob, Location target, double speed, Consumer<Boolean> callback,
      Consumer<Consumer<Boolean>> additionalChecks) {
    this.pathTo(mob, target, speed, callback, (Consumer<Boolean> onPathComplete) -> {
      PathResult currentPath = mob.getPathfinder().getCurrentPath();
      // Ensure the mob can reach the final point (e.g. if it was blocked by a wall
      // after moving)
      if (!currentPath.canReachFinalPoint()) {
        onPathComplete.accept(false);
        return;
      }

      if (additionalChecks != null) {
        additionalChecks.accept(onPathComplete);
      }
    });
  }

  public void pathTo(Mob mob, Location target, double speed, Consumer<Boolean> callback) {
    this.pathTo(mob, target, speed, callback, null);
  }

  public void pathTo(Mob mob, Location target, double speed, Consumer<Boolean> callback,
      Consumer<Consumer<Boolean>> additionalChecks) {
    Pathfinder pathfinder = mob.getPathfinder();

    pathfinder.moveTo(target, speed);

    Consumer<Boolean> onPathComplete = (Boolean reachedTarget) -> {
      this.scheduler.cancelTask(this.taskId);
      pathfinder.stopPathfinding();
      callback.accept(reachedTarget);
    };

    this.taskId = this.scheduler.runTaskTimer(this.plugin, () -> {
      this.runPathingChecks(mob, target, onPathComplete);

      if (additionalChecks != null) {
        additionalChecks.accept(onPathComplete);
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
