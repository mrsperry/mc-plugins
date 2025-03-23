package com.mrjoshuasperry.pocketplugins.modules.explorersatlas.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCursor;
import org.bukkit.util.StringUtil;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.pocketplugins.modules.explorersatlas.Waypoint;
import com.mrjoshuasperry.pocketplugins.modules.explorersatlas.WaypointManager;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MarkersCommand implements CommandExecutor, TabCompleter {
  private final WaypointManager waypointManager;
  private final Random random;

  private final List<MapCursor.Type> cursorTypes;

  public MarkersCommand(Random random) {
    this.waypointManager = WaypointManager.getInstance();
    this.random = random;

    this.cursorTypes = Lists
        .newArrayList(RegistryAccess.registryAccess().getRegistry(RegistryKey.MAP_DECORATION_TYPE).iterator()).stream()
        .filter((MapCursor.Type type) -> type.getKey().key().toString().startsWith("BANNER_"))
        .toList();
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> completions = Arrays.asList("add", "list");
      return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
    }
    return new ArrayList<>();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can only be used by players");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 0) {
      player.sendMessage(Component.text("Usage: /markers <add|list>").color(NamedTextColor.RED));
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "add":
        return handleAddMarker(player, args);
      case "list":
        return handleListMarkers(player);
      case "confirm_remove":
        if (args.length == 2) {
          try {
            UUID markerId = UUID.fromString(args[1]);
            return handleConfirmRemove(player, markerId);
          } catch (IllegalArgumentException e) {
            return true;
          }
        }
        return true;
      case "remove":
        if (args.length == 2) {
          try {
            UUID markerId = UUID.fromString(args[1]);
            return handleRemoveMarker(player, markerId);
          } catch (IllegalArgumentException e) {
            return true;
          }
        }
        return true;
      case "toggle":
        if (args.length == 2) {
          try {
            UUID markerId = UUID.fromString(args[1]);
            return handleToggleMarker(player, markerId);
          } catch (IllegalArgumentException e) {
            return true;
          }
        }
        return true;
      default:
        player.sendMessage(Component.text("Unknown sub-command. Use: add or list").color(NamedTextColor.RED));
        return true;
    }
  }

  private boolean handleAddMarker(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(Component.text("Usage: /markers add <name>").color(NamedTextColor.RED));
      return true;
    }

    String markerName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
    Waypoint waypoint = new Waypoint(markerName, player.getLocation());
    waypoint.setCursorType(this.cursorTypes.get(this.random.nextInt(this.cursorTypes.size())));
    waypointManager.addWaypoint(player.getUniqueId(), waypoint);

    player.sendMessage(Component.text("Marker '").color(NamedTextColor.GREEN)
        .append(Component.text(markerName).color(NamedTextColor.GOLD))
        .append(Component.text("' created!").color(NamedTextColor.GREEN)));

    return true;
  }

  private boolean handleConfirmRemove(Player player, UUID markerId) {
    List<Waypoint> waypoints = waypointManager.getPlayerWaypoints(player.getUniqueId());

    for (Waypoint waypoint : waypoints) {
      if (waypoint.getId().equals(markerId)) {
        Component confirmButton = Component.text("[Yes]")
            .color(NamedTextColor.RED)
            .clickEvent(ClickEvent.runCommand("/markers remove " + markerId.toString()))
            .hoverEvent(HoverEvent.showText(Component.text("Click to confirm removal")));

        Component cancelButton = Component.text("[No]")
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.runCommand("/markers list"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to cancel")));

        player.sendMessage(
            Component.text("Are you sure you want to remove marker '")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(waypoint.getName()).color(NamedTextColor.GOLD))
                .append(Component.text("'? ").color(NamedTextColor.YELLOW))
                .append(confirmButton)
                .append(Component.text(" "))
                .append(cancelButton));
        return true;
      }
    }
    return true;
  }

  private boolean handleRemoveMarker(Player player, UUID markerId) {
    waypointManager.removeWaypoint(player.getUniqueId(), markerId);
    player.sendMessage(Component.text("Marker removed!").color(NamedTextColor.GREEN));
    return true;
  }

  private boolean handleToggleMarker(Player player, UUID markerId) {
    List<Waypoint> waypoints = waypointManager.getPlayerWaypoints(player.getUniqueId());

    for (Waypoint waypoint : waypoints) {
      if (waypoint.getId().equals(markerId)) {
        waypointManager.toggleWaypoint(player.getUniqueId(), markerId);
        String status = waypoint.isEnabled() ? "enabled" : "disabled";
        player.sendMessage(Component.text("Marker ").color(NamedTextColor.GREEN)
            .append(Component.text(waypoint.getName()).color(NamedTextColor.GOLD))
            .append(Component.text(" " + status + "!").color(NamedTextColor.GREEN)));
        return true;
      }
    }
    return true;
  }

  private boolean handleListMarkers(Player player) {
    List<Waypoint> waypoints = waypointManager.getPlayerWaypoints(player.getUniqueId());

    if (waypoints.isEmpty()) {
      player.sendMessage(Component.text("You have no markers!").color(NamedTextColor.YELLOW));
      return true;
    }

    player.sendMessage(Component.text("Your Markers:").color(NamedTextColor.GOLD));

    for (Waypoint waypoint : waypoints) {
      Component toggleButton = Component.text("[⚡]")
          .color(waypoint.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)
          .clickEvent(ClickEvent.runCommand("/markers toggle " + waypoint.getId().toString()))
          .hoverEvent(HoverEvent.showText(Component.text("Click to toggle")));

      Component removeButton = Component.text("[✖]")
          .color(NamedTextColor.RED)
          .clickEvent(ClickEvent.runCommand("/markers confirm_remove " + waypoint.getId().toString()))
          .hoverEvent(HoverEvent.showText(Component.text("Click to remove")));

      Component locationText = Component.text(" at ")
          .append(Component.text(String.format("(%d, %d, %d)",
              waypoint.getLocation().getBlockX(),
              waypoint.getLocation().getBlockY(),
              waypoint.getLocation().getBlockZ())))
          .color(NamedTextColor.GRAY);

      player.sendMessage(
          toggleButton
              .append(Component.text(" "))
              .append(removeButton)
              .append(Component.text(" "))
              .append(Component.text(waypoint.getName()).color(NamedTextColor.YELLOW))
              .append(locationText));
    }

    return true;
  }
}