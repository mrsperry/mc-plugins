package com.mrjoshuasperry.pocketplugins.modules.autobreeding;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;
import com.mrjoshuasperry.pocketplugins.utils.PathfinderUtil;

/** @author TimPCunningham */
public class AutoBreeding extends Module {
  private static long BREED_TASK_INTERVAL = 5;
  private static double TARGET_CHANCE = 0.25;

  private final JavaPlugin plugin;
  private final Random random;
  private final Map<Animals, Item> breedingTargets;

  /**
   * Every dropped item currently loose in the world. Whether a given item is food
   * depends on the animal looking at it, so this cannot be filtered down by
   * material up front; the search below starts from these instead of from every
   * animal in every world.
   */
  private final Set<Item> droppedItems = new HashSet<>();

  private double minDelay;
  private double maxDelay;
  private double range;

  public AutoBreeding(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.plugin = PocketPlugins.getInstance();
    this.random = this.getPlugin().getRandom();
    this.breedingTargets = new HashMap<>();

    this.minDelay = readableConfig.getDouble("min-delay", 0.25f);
    this.maxDelay = readableConfig.getDouble("max-delay", 2f);
    this.range = readableConfig.getInt("range", 8);

    this.tick();
  }

  private void tick() {
    Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
      tickEntityHasTarget();
      tickEntityFindTarget();
    }, 0, BREED_TASK_INTERVAL);
  }

  private void tickEntityFindTarget() {
    // Picked up, despawned, or chunk unloaded. This has to run to completion
    // before the search below, which returns early as soon as it starts an animal
    this.droppedItems.removeIf((item) -> !item.isValid());

    for (Item target : this.droppedItems) {
      Collection<Animals> entities = target.getWorld().getNearbyEntitiesByType(Animals.class, target.getLocation(),
          this.range);
      entities
          .removeIf((entity) -> this.breedingTargets.containsKey(entity) || entity.isLoveMode() || !entity.canBreed());

      for (Animals entity : entities) {
        if (this.random.nextDouble() > TARGET_CHANCE) {
          continue;
        }

        if (!isValidTarget(entity, target)) {
          continue;
        }

        this.scheduleBreeding(entity, target);
        // Only ever start one animal per pass, as before
        return;
      }
    }
  }

  private void scheduleBreeding(Animals entity, Item target) {
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
  }

  @EventHandler
  public void trackSpawnedItem(ItemSpawnEvent event) {
    this.droppedItems.add(event.getEntity());
  }

  /**
   * Items already on the ground when a chunk comes back never fire
   * {@link ItemSpawnEvent}, so they have to be picked up here.
   */
  @EventHandler
  public void trackLoadedItems(EntitiesLoadEvent event) {
    for (Entity entity : event.getEntities()) {
      if (entity instanceof Item) {
        this.droppedItems.add((Item) entity);
      }
    }
  }

  private void tickEntityHasTarget() {
    Iterator<Map.Entry<Animals, Item>> iterator = this.breedingTargets.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<Animals, Item> entry = iterator.next();
      Animals entity = entry.getKey();

      // A dead or unloaded animal leaves its target perfectly valid, so without
      // this the entry - and the animal it keys on - would be held forever
      if (!entity.isValid()) {
        iterator.remove();
        continue;
      }

      if (!isValidTarget(entity, entry.getValue())) {
        entity.getPathfinder().stopPathfinding();
        iterator.remove();
      }
    }
  }

  private boolean isValidTarget(Animals entity, Item target) {
    return target != null && target.getItemStack().getAmount() > 0 && entity.isBreedItem(target.getItemStack())
        && !target.isDead();
  }
}
