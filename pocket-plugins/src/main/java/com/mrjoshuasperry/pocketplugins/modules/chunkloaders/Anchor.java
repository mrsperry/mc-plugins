package com.mrjoshuasperry.pocketplugins.modules.chunkloaders;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.entity.Entity;

/**
 * The block position of a single loader crystal. Identity is the exact block so
 * two crystals sharing a chunk stay distinct, while {@link #chunkX()} /
 * {@link #chunkZ()} give the chunk the coverage square is centered on.
 */
public record Anchor(String world, int x, int y, int z) {
  public static Anchor of(Entity entity) {
    return new Anchor(
        entity.getWorld().getName(),
        entity.getLocation().getBlockX(),
        entity.getLocation().getBlockY(),
        entity.getLocation().getBlockZ());
  }

  public int chunkX() {
    return this.x >> 4;
  }

  public int chunkZ() {
    return this.z >> 4;
  }

  public Map<String, Object> serialize() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("world", this.world);
    map.put("x", this.x);
    map.put("y", this.y);
    map.put("z", this.z);
    return map;
  }

  /** Returns {@code null} for a malformed entry rather than aborting the whole load. */
  public static Anchor deserialize(Map<?, ?> map) {
    Object world = map.get("world");
    Object x = map.get("x");
    Object y = map.get("y");
    Object z = map.get("z");

    if (!(world instanceof String worldName)
        || !(x instanceof Number)
        || !(y instanceof Number)
        || !(z instanceof Number)) {
      return null;
    }

    return new Anchor(worldName, ((Number) x).intValue(), ((Number) y).intValue(), ((Number) z).intValue());
  }
}
