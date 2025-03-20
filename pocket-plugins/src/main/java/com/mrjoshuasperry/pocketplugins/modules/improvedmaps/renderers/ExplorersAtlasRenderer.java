package com.mrjoshuasperry.pocketplugins.modules.improvedmaps.renderers;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.mrjoshuasperry.pocketplugins.modules.improvedmaps.Waypoint;
import com.mrjoshuasperry.pocketplugins.modules.improvedmaps.WaypointManager;

public class ExplorersAtlasRenderer extends MapRenderer {
  private final UUID owner;
  private final WaypointManager waypointManager;

  public ExplorersAtlasRenderer(UUID owner) {
    this.owner = owner;
    this.waypointManager = WaypointManager.getInstance();
  }

  private MapCursorCollection createCursors(MapView map) {
    List<Waypoint> waypoints = this.waypointManager.getPlayerWaypoints(this.owner);
    MapCursorCollection cursors = new MapCursorCollection();

    int mapCenterX = map.getCenterX();
    int mapCenterZ = map.getCenterZ();

    for (Waypoint waypoint : waypoints) {
      // Skip disabled waypoints
      if (!waypoint.isEnabled()) {
        continue;
      }

      int waypointX = waypoint.getLocation().getBlockX();
      int waypointZ = waypoint.getLocation().getBlockZ();

      int relativeX = (waypointX - mapCenterX) * 2;
      int relativeZ = (waypointZ - mapCenterZ) * 2;

      byte direction = 0;

      // If waypoint is outside map bounds, calculate edge position and direction
      if (relativeX < -128 || relativeX > 127 || relativeZ < -128 || relativeZ > 127) {
        // Calculate angle from center (0,0) to the relative point
        double angle = Math.atan2(relativeZ, relativeX);
        direction = (byte) (((angle * 16) / (2 * Math.PI) + 16) % 16);

        // Find intersection with map border
        double absX = Math.abs(relativeX);
        double absZ = Math.abs(relativeZ);
        double scale = Math.max(absX / 128.0, absZ / 128.0);

        relativeX = (int) (relativeX / scale);
        relativeZ = (int) (relativeZ / scale);

        // Ensure we stay within bounds
        relativeX = Math.max(-128, Math.min(127, relativeX));
        relativeZ = Math.max(-128, Math.min(127, relativeZ));
      }

      cursors.addCursor(
          new MapCursor(
              (byte) relativeX,
              (byte) relativeZ,
              direction,
              waypoint.getCursorType(),
              true,
              waypoint.getName()));
    }

    return cursors;
  }

  @Override
  public void render(MapView map, MapCanvas canvas, Player player) {
    map.setCenterX(player.getLocation().getBlockX());
    map.setCenterZ(player.getLocation().getBlockZ());

    MapCursorCollection cursors = this.createCursors(map);

    canvas.setCursors(cursors);
  }
}
