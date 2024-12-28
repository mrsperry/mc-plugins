package com.mrjoshuasperry.pocketplugins.additions.improvedMaps;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.map.MapCursor;

public class Waypoint {
  private final UUID id;
  private String name;
  private Location location;
  private boolean enabled;
  private MapCursor.Type cursorType;

  public Waypoint(String name, Location location) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.location = location;
    this.enabled = true;
    this.cursorType = MapCursor.Type.RED_MARKER;
  }

  public String getName() {
    return name;
  }

  public Location getLocation() {
    return location;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public UUID getId() {
    return id;
  }

  public MapCursor.Type getCursorType() {
    return cursorType;
  }

  public void setCursorType(MapCursor.Type cursorType) {
    this.cursorType = cursorType;
  }
}