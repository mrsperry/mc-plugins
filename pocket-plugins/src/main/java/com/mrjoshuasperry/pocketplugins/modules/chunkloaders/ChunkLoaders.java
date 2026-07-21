package com.mrjoshuasperry.pocketplugins.modules.chunkloaders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/**
 * Turns a player-placed end crystal into a chunk loader: the crystal's chunk and
 * the ring around it (a {@code radius}-wide square, so {@code 3x3} by default)
 * are force-loaded and keep ticking. Chunks are always force-loaded as a full
 * vertical column, so coverage runs the whole world height regardless of where
 * the crystal sits.
 *
 * <p>
 * The explosion is left vanilla on purpose — blowing a crystal up is how you take
 * a loader down, and the risk is what makes placement a decision. Crystals that
 * spawn with the world (the End's obsidian pillars) never fire
 * {@link EntityPlaceEvent}, so they are not treated as loaders.
 *
 * <p>
 * Loaders are recorded to the writable config so they survive a restart; Paper
 * also persists the force-load flags itself, but the record is what tells us
 * which chunks are ours to release and lets overlapping loaders reference-count
 * their shared chunks.
 */
public class ChunkLoaders extends Module {
  protected int radius;

  /** Each loader crystal, keyed by the exact block it was placed on. */
  protected final Set<Anchor> anchors;

  public ChunkLoaders(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.radius = readableConfig.getInt("radius", 1);
    this.anchors = new HashSet<>();

    this.loadAnchors();
  }

  @Override
  public void onEnable() {
    super.onEnable();

    // Reapply force-loading for every saved loader. Worlds that failed to load are
    // skipped rather than dropped, so their loaders come back if the world returns.
    for (Anchor anchor : this.anchors) {
      World world = Bukkit.getWorld(anchor.world());
      if (world != null) {
        this.setForced(world, anchor, true);
      }
    }
  }

  @EventHandler
  public void onEntityPlace(EntityPlaceEvent event) {
    if (event.getEntityType() != EntityType.END_CRYSTAL) {
      return;
    }

    Anchor anchor = Anchor.of(event.getEntity());
    if (this.anchors.add(anchor)) {
      this.setForced(event.getEntity().getWorld(), anchor, true);
      this.persist();
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    Entity entity = event.getEntity();
    if (entity.getType() != EntityType.END_CRYSTAL) {
      return;
    }

    // Any damage destroys an end crystal, so the anchor is on its way out. Drop it
    // and release only the chunks no surviving loader still covers.
    Anchor anchor = Anchor.of(entity);
    if (this.anchors.remove(anchor)) {
      this.releaseUncovered(entity.getWorld(), anchor);
      this.persist();
    }
  }

  /** Force-loads or clears the square of chunks the given anchor covers. */
  protected void setForced(World world, Anchor anchor, boolean forced) {
    for (int x = anchor.chunkX() - this.radius; x <= anchor.chunkX() + this.radius; x++) {
      for (int z = anchor.chunkZ() - this.radius; z <= anchor.chunkZ() + this.radius; z++) {
        world.setChunkForceLoaded(x, z, forced);
      }
    }
  }

  /**
   * Clears the removed anchor's chunks, but keeps any that a still-present loader
   * covers so overlapping loaders don't yank each other's chunks.
   */
  protected void releaseUncovered(World world, Anchor removed) {
    for (int x = removed.chunkX() - this.radius; x <= removed.chunkX() + this.radius; x++) {
      for (int z = removed.chunkZ() - this.radius; z <= removed.chunkZ() + this.radius; z++) {
        if (!this.isCovered(world, x, z)) {
          world.setChunkForceLoaded(x, z, false);
        }
      }
    }
  }

  /** Whether any remaining loader in this world force-loads chunk {@code (x, z)}. */
  protected boolean isCovered(World world, int x, int z) {
    for (Anchor anchor : this.anchors) {
      if (anchor.world().equals(world.getName())
          && Math.abs(anchor.chunkX() - x) <= this.radius
          && Math.abs(anchor.chunkZ() - z) <= this.radius) {
        return true;
      }
    }

    return false;
  }

  protected void loadAnchors() {
    List<?> saved = this.getWritableConfig().getList("loaders");
    if (saved == null) {
      return;
    }

    for (Object entry : saved) {
      if (entry instanceof Map<?, ?> map) {
        Anchor anchor = Anchor.deserialize(map);
        if (anchor != null) {
          this.anchors.add(anchor);
        }
      }
    }
  }

  protected void persist() {
    List<Object> serialized = new ArrayList<>();
    for (Anchor anchor : this.anchors) {
      serialized.add(anchor.serialize());
    }

    this.getWritableConfig().set("loaders", serialized);
    this.saveConfig();
  }
}
