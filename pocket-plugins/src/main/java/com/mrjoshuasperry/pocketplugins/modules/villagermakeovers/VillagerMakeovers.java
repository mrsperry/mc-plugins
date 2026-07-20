package com.mrjoshuasperry.pocketplugins.modules.villagermakeovers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.scheduler.BukkitTask;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

/**
 * Re-skins villagers to match the biome they are standing in.
 *
 * <p>
 * Vanilla picks a villager's biome outfit once, when it spawns, and it never
 * changes again — a desert villager traded into a snowy village stays in desert
 * robes forever. This module re-evaluates the outfit as villagers move. A biome
 * with no configured outfit leaves the villager alone rather than resetting it,
 * so unmapped territory is inert instead of destructive.
 *
 * <p>
 * There is no "entity changed biome" event, so this polls. The whole villager
 * population is swept once every {@value #BATCHES_PER_CYCLE} seconds, split into
 * one batch per second so the per-tick cost stays flat instead of spiking on a
 * server with many villagers.
 */
public class VillagerMakeovers extends Module {
  private static final int BATCH_PERIOD_TICKS = 20;
  private static final int BATCHES_PER_CYCLE = 3;

  private final OutfitTable table;
  private final boolean includeZombieVillagers;
  private final Map<String, Villager.Type> outfits;
  private final Deque<LivingEntity> queue;

  private int batchSize;
  private int batch;
  private BukkitTask task;

  public VillagerMakeovers(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.includeZombieVillagers = readableConfig.getBoolean("include-zombie-villagers", true);
    this.table = OutfitTable.parse(readableConfig, typesByName().keySet());
    this.outfits = new HashMap<>();
    this.queue = new ArrayDeque<>();
  }

  /**
   * The seven villager outfits, keyed by the name used in config.
   *
   * <p>
   * Read from the registry rather than {@code Villager.Type.values()}, which is
   * deprecated for removal along with the rest of the old enum shims.
   */
  private static Map<String, Villager.Type> typesByName() {
    return RegistryAccess.registryAccess().getRegistry(RegistryKey.VILLAGER_TYPE).stream()
        .collect(Collectors.toMap(type -> type.getKey().value(), type -> type));
  }

  private static List<String> biomeKeys() {
    return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream()
        .map(biome -> biome.getKey().asString())
        .toList();
  }

  @Override
  public void onEnable() {
    super.onEnable();

    Map<String, Villager.Type> types = typesByName();
    for (Map.Entry<String, String> entry : this.table.getTypeByBiome().entrySet()) {
      this.outfits.put(entry.getKey(), types.get(entry.getValue()));
    }

    this.audit();

    this.task = this.getPlugin().getServer().getScheduler()
        .runTaskTimer(this.getPlugin(), this::sweep, BATCH_PERIOD_TICKS, BATCH_PERIOD_TICKS);
  }

  @Override
  public void onDisable() {
    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    }

    this.queue.clear();
    super.onDisable();
  }

  /** Reports config mistakes and, more importantly, biomes the config has not caught up with. */
  private void audit() {
    Logger logger = this.getPlugin().getLogger();
    List<String> known = biomeKeys();

    for (String warning : this.table.getWarnings()) {
      logger.warning("VillagerMakeovers: " + warning);
    }

    List<String> unknown = this.table.findUnknownBiomes(known);
    if (!unknown.isEmpty()) {
      logger.log(Level.WARNING, () -> "VillagerMakeovers: config names " + unknown.size()
          + " biome(s) that do not exist: " + String.join(", ", unknown));
    }

    List<String> unmapped = this.table.findUnmappedBiomes(known);
    if (!unmapped.isEmpty()) {
      logger.log(Level.WARNING, () -> "VillagerMakeovers: no outfit is configured for " + unmapped.size()
          + " biome(s): " + String.join(", ", unmapped)
          + ". Villagers entering them keep whatever they are wearing; list each one under `outfits`"
          + " or under `unstyled` in config.yml to silence this.");
    }
  }

  /**
   * Drains one batch per run, rebuilding the work queue at the start of each cycle.
   *
   * <p>
   * The queue is a snapshot: villagers that die or unload mid-cycle are dropped by
   * the validity check in {@link #applyOutfit}, and ones that spawn mid-cycle are
   * simply picked up by the next rebuild.
   */
  private void sweep() {
    if (this.batch % BATCHES_PER_CYCLE == 0) {
      this.refill();
    }
    this.batch++;

    for (int handled = 0; handled < this.batchSize && !this.queue.isEmpty(); handled++) {
      this.applyOutfit(this.queue.poll());
    }
  }

  private void refill() {
    this.queue.clear();

    for (World world : this.getPlugin().getServer().getWorlds()) {
      this.queue.addAll(world.getEntitiesByClass(Villager.class));

      if (this.includeZombieVillagers) {
        this.queue.addAll(world.getEntitiesByClass(ZombieVillager.class));
      }
    }

    this.batchSize = Math.ceilDiv(this.queue.size(), BATCHES_PER_CYCLE);
  }

  private void applyOutfit(LivingEntity entity) {
    if (entity == null || !entity.isValid()) {
      return;
    }

    String biome = entity.getLocation().getBlock().getBiome().getKey().asString().toLowerCase(Locale.ROOT);
    Villager.Type outfit = this.outfits.get(biome);

    if (outfit == null) {
      return;
    }

    switch (entity) {
      case Villager villager when villager.getVillagerType() != outfit -> villager.setVillagerType(outfit);
      case ZombieVillager zombie when zombie.getVillagerType() != outfit -> zombie.setVillagerType(outfit);
      default -> {
        // Already wearing the right outfit, or not something the queue styles.
      }
    }
  }
}
