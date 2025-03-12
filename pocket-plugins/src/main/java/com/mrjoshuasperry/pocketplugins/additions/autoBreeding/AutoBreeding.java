package com.mrjoshuasperry.pocketplugins.additions.autoBreeding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;
import com.mrjoshuasperry.pocketplugins.utils.PathfinderUtil;

public class AutoBreeding extends Module {
  private static long BREED_TASK_INTERVAL = 5;
  private static double TARGET_CHANCE = 0.25;

  private final JavaPlugin plugin;
  private final Random random;
  private final Map<Animals, Item> breedingTargets;

  private double minDelay;
  private double maxDelay;
  private double range;

  public AutoBreeding() {
    super("AutoBreeding");

    this.plugin = PocketPlugins.getInstance();
    this.random = new Random();
    this.breedingTargets = new HashMap<>();
  }

  @Override
  public void init(YamlConfiguration configuration) {
    super.init(configuration);

    this.minDelay = configuration.getDouble("min-delay", 0.25f);
    this.maxDelay = configuration.getDouble("max-delay", 2f);
    this.range = configuration.getInt("range", 8);

    this.tick();
  }

  private void tick() {
    Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
      tickEntityHasTarget();
      tickEntityFindTarget();
    }, 0, BREED_TASK_INTERVAL);
  }

  private void tickEntityFindTarget() {
    for (World world : Bukkit.getWorlds()) {
      Collection<Animals> entities = world.getEntitiesByClass(Animals.class);
      entities
          .removeIf((entity) -> this.breedingTargets.containsKey(entity) || entity.isLoveMode() || !entity.canBreed());

      for (Animals entity : entities) {
        if (this.random.nextDouble() > TARGET_CHANCE) {
          continue;
        }

        Collection<Item> targets = entity.getWorld().getNearbyEntitiesByType(Item.class, entity.getLocation(),
            this.range);
        for (Item target : targets) {
          if (!isValidTarget(entity, target)) {
            continue;
          }

          Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.breedingTargets.put(entity, target);
            PathfinderUtil pathfinder = new PathfinderUtil();
            pathfinder.pathTo(
                entity,
                target.getLocation(),
                1,
                (Boolean success) -> {
                  if (!success) {
                    this.breedingTargets.remove(entity);
                    return;
                  }

                  if (!isValidTarget(entity, target)) {
                    return;
                  }

                  target.getItemStack().setAmount(target.getItemStack().getAmount() - 1);
                  entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().clone().add(0, 1.5, 0), 10);
                  entity.setLoveModeTicks(600);

                  entity.getWorld().playSound(entity, Sound.ENTITY_ARMADILLO_EAT, 1, 1);
                  this.breedingTargets.remove(entity);
                });
          }, Math.round(this.random.nextDouble(minDelay, maxDelay) * 20));
          return;
        }
      }
    }
  }

  private void tickEntityHasTarget() {
    List<Animals> animalsMarkedForRemoval = new ArrayList<>();

    for (Animals entity : this.breedingTargets.keySet()) {
      Item target = this.breedingTargets.get(entity);

      if (!isValidTarget(entity, target)) {
        entity.getPathfinder().stopPathfinding();
        animalsMarkedForRemoval.add(entity);
      }
    }

    for (Animals entity : animalsMarkedForRemoval) {
      this.breedingTargets.remove(entity);
    }
  }

  private boolean isValidTarget(Animals entity, Item target) {
    return target != null && target.getItemStack().getAmount() > 0 && entity.isBreedItem(target.getItemStack())
        && !target.isDead();
  }
}
