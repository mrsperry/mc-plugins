package com.mrjoshuasperry.pocketplugins.additions.improvedMaps;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.map.MapCursor;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

@SerializableAs("WaypointManager")
public class WaypointManager implements ConfigurationSerializable {
  private Map<UUID, List<Waypoint>> waypoints;
  private static WaypointManager self;

  private WaypointManager() {
    waypoints = new HashMap<>();
  }

  public static WaypointManager getInstance() {
    if (self == null) {
      self = new WaypointManager();
    }

    return self;
  }

  public void addWaypoint(UUID playerUUID, Waypoint point) {
    List<Waypoint> playerWaypoints = getPlayerWaypoints(playerUUID);

    playerWaypoints.add(point);
    waypoints.put(playerUUID, playerWaypoints);
  }

  public List<Waypoint> getPlayerWaypoints(UUID playerUUID) {
    return waypoints.getOrDefault(playerUUID, new ArrayList<Waypoint>());
  }

  /**
   * Removes a specific waypoint for a player
   * 
   * @param playerId   The UUID of the player
   * @param waypointId The UUID of the waypoint to remove
   * @return true if the waypoint was found and removed, false otherwise
   */
  public boolean removeWaypoint(UUID playerId, UUID waypointId) {
    List<Waypoint> playerWaypoints = waypoints.get(playerId);
    if (playerWaypoints == null) {
      return false;
    }

    return playerWaypoints.removeIf(waypoint -> waypoint.getId().equals(waypointId));
  }

  /**
   * Toggles the enabled status of a specific waypoint
   * 
   * @param playerId   The UUID of the player
   * @param waypointId The UUID of the waypoint to toggle
   * @return true if the waypoint was found and toggled, false otherwise
   */
  public boolean toggleWaypoint(UUID playerId, UUID waypointId) {
    List<Waypoint> playerWaypoints = waypoints.get(playerId);
    if (playerWaypoints == null) {
      return false;
    }

    for (Waypoint waypoint : playerWaypoints) {
      if (waypoint.getId().equals(waypointId)) {
        waypoint.setEnabled(!waypoint.isEnabled());
        return true;
      }
    }

    return false;
  }

  private void setWaypoints(Map<UUID, List<Waypoint>> waypoints) {
    this.waypoints = waypoints;
  }

  @Override
  public Map<String, Object> serialize() {
    Map<String, Object> result = new HashMap<>();
    List<String> playerIds = new ArrayList<>();

    for (Map.Entry<UUID, List<Waypoint>> entry : this.waypoints.entrySet()) {
      String playerId = entry.getKey().toString();
      List<Map<String, Object>> playerWaypoints = new ArrayList<>();

      for (Waypoint waypoint : entry.getValue()) {
        Map<String, Object> serializedWaypoint = new HashMap<>();
        serializedWaypoint.put("id", waypoint.getId().toString());
        serializedWaypoint.put("name", waypoint.getName());
        serializedWaypoint.put("location", waypoint.getLocation().serialize());
        serializedWaypoint.put("enabled", waypoint.isEnabled());
        serializedWaypoint.put("cursorType", waypoint.getCursorType().toString());
        playerWaypoints.add(serializedWaypoint);
      }

      result.put(playerId, playerWaypoints);
      playerIds.add(playerId);
    }

    result.put("players", playerIds);
    return result;
  }

  @SuppressWarnings("unchecked")
  public static WaypointManager deserialize(Map<String, Object> args) {
    WaypointManager manager = getInstance();
    Map<UUID, List<Waypoint>> savedWaypoints = new HashMap<>();
    List<String> playerIds = (ArrayList<String>) args.get("players");

    for (String playerId : playerIds) {
      UUID playerUUID = UUID.fromString(playerId);
      List<Waypoint> playerWaypoints = new ArrayList<>();
      List<Map<String, Object>> serializedWaypoints = (List<Map<String, Object>>) args.get(playerId);

      for (Map<String, Object> waypointData : serializedWaypoints) {
        Location loc = Location.deserialize(
            (Map<String, Object>) waypointData.get("location"));
        String name = (String) waypointData.get("name");
        boolean enabled = (boolean) waypointData.get("enabled");

        Waypoint waypoint = new Waypoint(name, loc);
        waypoint.setEnabled(enabled);

        try {
          String cursorTypeStr = (String) waypointData.get("cursorType");
          if (cursorTypeStr != null) {
            waypoint.setCursorType(MapCursor.Type.valueOf(cursorTypeStr));
          }
        } catch (IllegalArgumentException e) {
          PocketPlugins.getInstance().getLogger()
              .warning("Invalid cursor type found for waypoint: " + name);
        }

        playerWaypoints.add(waypoint);
      }

      savedWaypoints.put(playerUUID, playerWaypoints);
    }

    manager.setWaypoints(savedWaypoints);
    return manager;
  }

  public void saveWaypoints(File file) {
    YamlConfiguration config = new YamlConfiguration();
    config.set("waypoints", this);
    try {
      config.save(file);
    } catch (Exception e) {
      PocketPlugins.getInstance().getLogger().warning("Error saving waypoints!");
      e.printStackTrace();
    }
  }

  public void loadWaypoints(File file) {
    try {
      YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
      config.get("waypoints"); // This triggers the deserialization
    } catch (Exception e) {
      PocketPlugins.getInstance().getLogger().warning("Error loading waypoints!");
      e.printStackTrace();
    }
  }
}
