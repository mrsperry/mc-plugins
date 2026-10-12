package com.mrjoshuasperry.pocketplugins.modules.extendedbeacons;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.event.block.BeaconActivatedEvent;
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent;

/**
 * Lets a beacon pyramid keep growing past vanilla's four layers, with every
 * layer adding range. Vanilla reaches {@code 10 + 10 x layers} blocks and stops
 * counting at four; here each complete layer of beacon base blocks under the
 * beacon adds {@code range-per-layer}, however many there are. Effect strength
 * and duration stay vanilla.
 *
 * <p>
 * The range goes through Paper's custom effect range, which reaches that far
 * horizontally and downward; upward always runs to the build limit, as in
 * vanilla. Paper saves that value with the beacon, so it is recounted whenever
 * the pyramid may have changed (a base block placed, broken, blown up or pushed
 * inside one, the beacon activating, or its effect being changed) and reset
 * whenever a beacon stops being tracked, so turning the module off leaves vanilla
 * beacons behind. Edits that fire no events, like WorldEdit, are picked up at the
 * next of those.
 *
 * <p>
 * Counting never loads chunks: a layer running into an unloaded chunk ends the
 * count there, and the beacon is recounted as more chunks load.
 */
public class ExtendedBeacons extends Module {
  protected double baseRange;
  protected double rangePerLayer;
  /** Most layers counted, or 0 for no limit. */
  protected int maxLayers;

  /**
   * Every beacon in a loaded chunk, mapped to whether its last count was cut
   * short by an unloaded chunk.
   */
  protected final Map<Block, Boolean> beacons;
  /** Beacons to recount next tick, once the change that queued them has landed. */
  protected final Set<Block> pending;

  public ExtendedBeacons(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.baseRange = Math.max(0, readableConfig.getDouble("base-range", 10));
    this.rangePerLayer = Math.max(0, readableConfig.getDouble("range-per-layer", 10));
    this.maxLayers = Math.max(0, readableConfig.getInt("max-layers", 0));
    this.beacons = new HashMap<>();
    this.pending = new HashSet<>();
  }

  @Override
  public void onEnable() {
    super.onEnable();

    for (World world : Bukkit.getWorlds()) {
      for (Chunk chunk : world.getLoadedChunks()) {
        this.trackBeaconsIn(chunk);
      }
    }
  }

  @Override
  public void onDisable() {
    for (Block beacon : this.beacons.keySet()) {
      this.resetRange(beacon);
    }
    this.beacons.clear();
    this.pending.clear();

    super.onDisable();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlockPlaced();
    if (block.getType() == Material.BEACON) {
      this.track(block);
    } else if (Tag.BEACON_BASE_BLOCKS.isTagged(block.getType())) {
      this.queuePyramidsAt(block);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    this.queueIfRelevant(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    event.blockList().forEach(this::queueIfRelevant);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    event.blockList().forEach(this::queueIfRelevant);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPistonExtend(BlockPistonExtendEvent event) {
    this.queueMoved(event.getBlocks(), event.getDirection());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPistonRetract(BlockPistonRetractEvent event) {
    this.queueMoved(event.getBlocks(), event.getDirection());
  }

  @EventHandler
  public void onBeaconActivated(BeaconActivatedEvent event) {
    this.track(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBeaconEffectChange(PlayerChangeBeaconEffectEvent event) {
    this.track(event.getBeacon());
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    this.trackBeaconsIn(event.getChunk());

    // A count cut short by an unloaded chunk may reach further now
    World world = event.getWorld();
    this.beacons.forEach((beacon, truncated) -> {
      if (truncated && beacon.getWorld().equals(world)) {
        this.queue(beacon);
      }
    });
  }

  @EventHandler
  public void onChunkUnload(ChunkUnloadEvent event) {
    Chunk chunk = event.getChunk();
    // Reset before the chunk saves, so the custom range isn't written out with it;
    // the beacon is recounted when the chunk loads again
    this.beacons.keySet().removeIf(beacon -> {
      if (!beacon.getWorld().equals(chunk.getWorld())
          || beacon.getX() >> 4 != chunk.getX() || beacon.getZ() >> 4 != chunk.getZ()) {
        return false;
      }

      this.resetRange(beacon);
      this.pending.remove(beacon);
      return true;
    });
  }

  @EventHandler
  public void onWorldUnload(WorldUnloadEvent event) {
    World world = event.getWorld();
    this.beacons.keySet().removeIf(beacon -> {
      if (!beacon.getWorld().equals(world)) {
        return false;
      }

      this.resetRange(beacon);
      this.pending.remove(beacon);
      return true;
    });
  }

  protected void trackBeaconsIn(Chunk chunk) {
    for (BlockState state : chunk.getTileEntities(block -> block.getType() == Material.BEACON, false)) {
      this.track(state.getBlock());
    }
  }

  protected void track(Block beacon) {
    this.beacons.putIfAbsent(beacon, false);
    this.queue(beacon);
  }

  /** Only a beacon or a base block can change a pyramid's count. */
  protected void queueIfRelevant(Block block) {
    Material type = block.getType();
    if (type == Material.BEACON || Tag.BEACON_BASE_BLOCKS.isTagged(type)) {
      this.queuePyramidsAt(block);
    }
  }

  protected void queueMoved(List<Block> blocks, BlockFace direction) {
    for (Block block : blocks) {
      if (Tag.BEACON_BASE_BLOCKS.isTagged(block.getType())) {
        // Both where the block was and where it lands; which way a retract moves
        // depends on the piston, so cover both neighbours
        this.queuePyramidsAt(block);
        this.queuePyramidsAt(block.getRelative(direction));
        this.queuePyramidsAt(block.getRelative(direction.getOppositeFace()));
      }
    }
  }

  /** Queues every tracked beacon whose pyramid could include this block. */
  protected void queuePyramidsAt(Block block) {
    for (Block beacon : this.beacons.keySet()) {
      if (beacon.equals(block) || (beacon.getWorld().equals(block.getWorld())
          && isUnderPyramid(block.getX() - beacon.getX(), beacon.getY() - block.getY(),
              block.getZ() - beacon.getZ(), this.maxLayers))) {
        this.queue(beacon);
      }
    }
  }

  protected void queue(Block beacon) {
    if (this.pending.isEmpty()) {
      Bukkit.getScheduler().runTask(this.getPlugin(), this::recountPending);
    }
    this.pending.add(beacon);
  }

  protected void recountPending() {
    for (Block beacon : this.pending) {
      this.recount(beacon);
    }
    this.pending.clear();
  }

  protected void recount(Block block) {
    World world = block.getWorld();
    if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
        || !(block.getState(false) instanceof Beacon beacon)) {
      this.beacons.remove(block);
      return;
    }

    LayerCount count = countLayers(this.maxLayers, block.getY() - world.getMinHeight(),
        layer -> this.layerState(block, layer));
    this.beacons.put(block, count.truncated());

    if (count.layers() == 0) {
      beacon.resetEffectRange();
    } else {
      beacon.setEffectRange(rangeFor(count.layers(), this.baseRange, this.rangePerLayer));
    }
  }

  protected void resetRange(Block block) {
    if (block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
        && block.getState(false) instanceof Beacon beacon) {
      beacon.resetEffectRange();
    }
  }

  /** Layer {@code n} is the square of radius {@code n}, {@code n} blocks below the beacon. */
  protected LayerState layerState(Block beacon, int layer) {
    World world = beacon.getWorld();
    int y = beacon.getY() - layer;
    int minX = beacon.getX() - layer;
    int maxX = beacon.getX() + layer;
    int minZ = beacon.getZ() - layer;
    int maxZ = beacon.getZ() + layer;

    for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
      for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
          return LayerState.UNLOADED;
        }
      }
    }

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        if (!Tag.BEACON_BASE_BLOCKS.isTagged(world.getType(x, y, z))) {
          return LayerState.INCOMPLETE;
        }
      }
    }

    return LayerState.COMPLETE;
  }

  /**
   * Counts complete layers from the top down, stopping at the first that isn't
   * complete, at {@code maxLayers} (0 for no limit), or at the world floor.
   *
   * @param layersToFloor how many layers fit between the beacon and the bottom
   *                      of the world
   */
  protected static LayerCount countLayers(int maxLayers, int layersToFloor, IntFunction<LayerState> layerState) {
    int limit = maxLayers == 0 ? layersToFloor : Math.min(maxLayers, layersToFloor);

    for (int layer = 1; layer <= limit; layer++) {
      LayerState state = layerState.apply(layer);
      if (state != LayerState.COMPLETE) {
        return new LayerCount(layer - 1, state == LayerState.UNLOADED);
      }
    }

    return new LayerCount(limit, false);
  }

  protected static double rangeFor(int layers, double baseRange, double rangePerLayer) {
    return baseRange + rangePerLayer * layers;
  }

  /**
   * Whether a block offset from a beacon by {@code (dx, layer, dz)}, with
   * {@code layer} counted downward, falls inside its pyramid.
   */
  protected static boolean isUnderPyramid(int dx, int layer, int dz, int maxLayers) {
    return layer >= 1 && (maxLayers == 0 || layer <= maxLayers)
        && Math.abs(dx) <= layer && Math.abs(dz) <= layer;
  }

  protected enum LayerState {
    COMPLETE, INCOMPLETE, UNLOADED
  }

  protected record LayerCount(int layers, boolean truncated) {
  }
}
