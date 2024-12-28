package com.mrjoshuasperry.pocketplugins.additions.biomebombs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class BiomeBombCommand implements CommandExecutor {
  private final List<BiomeBombData> biomeBombs;

  public BiomeBombCommand(List<BiomeBombData> biomeBombs) {
    this.biomeBombs = biomeBombs;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Header
    sender
        .sendMessage(Component.text("════ Biome Bombs ════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    sender.sendMessage(Component.empty());

    // Crafting Info
    sender.sendMessage(
        Component.text("Crafting Recipe:").color(NamedTextColor.YELLOW)
            .append(Component.text(" (Shapeless)").color(NamedTextColor.GRAY)));
    sender.sendMessage(
        Component.text("• ").color(NamedTextColor.GRAY)
            .append(Component.text("1x Egg").color(NamedTextColor.WHITE))
            .append(Component.text(" + ").color(NamedTextColor.GRAY))
            .append(Component.text("8x Catalyst").color(NamedTextColor.WHITE)));
    sender.sendMessage(Component.empty());

    // Available Biomes
    sender.sendMessage(Component.text("Available Biomes:").color(NamedTextColor.YELLOW));

    for (BiomeBombData data : biomeBombs) {
      String catalyst = data.getCatalyst().toString().toLowerCase().replace('_', ' ');
      String formattedCatalyst = Arrays.stream(catalyst.split(" "))
          .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
          .collect(Collectors.joining(" "));

      sender.sendMessage(
          Component.text("  ").append(Component.text("• ").color(NamedTextColor.GRAY))
              .append(Component.text(data.getBiomeName()).color(data.getTextColor()))
              .append(Component.text(" (").color(NamedTextColor.GRAY))
              .append(Component.text(formattedCatalyst).color(NamedTextColor.GRAY))
              .append(Component.text(")").color(NamedTextColor.GRAY)));
    }

    return true;
  }
}