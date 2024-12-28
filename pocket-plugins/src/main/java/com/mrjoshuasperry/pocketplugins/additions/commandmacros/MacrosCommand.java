package com.mrjoshuasperry.pocketplugins.additions.commandmacros;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MacrosCommand implements CommandExecutor, TabCompleter {
  private final Map<String, MacroData> macros;

  public MacrosCommand(Map<String, MacroData> macros) {
    this.macros = macros;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sender.sendMessage(Component.text("Usage: /macro <list|run> [macro-name]").color(NamedTextColor.RED));
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "list":
        listMacros(sender);
        break;
      case "run":
        if (args.length < 2) {
          sender.sendMessage(Component.text("Usage: /macro run <macro-name>").color(NamedTextColor.RED));
          return true;
        }
        runMacro(sender, args[1].toLowerCase());
        break;
      default:
        sender.sendMessage(Component.text("Unknown subcommand. Use 'list' or 'run'").color(NamedTextColor.RED));
    }
    return true;
  }

  private void listMacros(CommandSender sender) {
    sender.sendMessage(
        Component.text("════ Command Macros ════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    sender.sendMessage(Component.empty());

    boolean isOp = !(sender instanceof Player) || ((Player) sender).isOp();

    for (Map.Entry<String, MacroData> entry : macros.entrySet()) {
      MacroData macro = entry.getValue();

      // Skip OP-only macros for non-OP players
      if (macro.isOpOnly() && !isOp)
        continue;

      sender.sendMessage(
          Component.text("  • ").color(NamedTextColor.GRAY)
              .append(Component.text("/macro run " + entry.getKey()).color(NamedTextColor.YELLOW))
              .append(Component.text(macro.isOpOnly() ? " (OP Only)" : "").color(NamedTextColor.RED)));

      for (String cmd : macro.getCommands()) {
        sender.sendMessage(
            Component.text("    → ").color(NamedTextColor.GRAY)
                .append(Component.text(cmd).color(NamedTextColor.WHITE)));
      }
      sender.sendMessage(Component.empty());
    }
  }

  private void runMacro(CommandSender sender, String macroName) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(Component.text("Only players can run macros!").color(NamedTextColor.RED));
      return;
    }

    Player player = (Player) sender;
    MacroData macro = macros.get(macroName);

    if (macro == null) {
      player.sendMessage(Component.text("That macro doesn't exist!").color(NamedTextColor.RED));
      return;
    }

    if (macro.isOpOnly() && !player.isOp()) {
      player.sendMessage(Component.text("You don't have permission to use this macro!").color(NamedTextColor.RED));
      return;
    }

    for (String cmd : macro.getCommands()) {
      player.performCommand(cmd);
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      completions.add("list");
      completions.add("run");
    } else if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
      boolean isOp = !(sender instanceof Player) || ((Player) sender).isOp();

      for (Map.Entry<String, MacroData> entry : macros.entrySet()) {
        if (!entry.getValue().isOpOnly() || isOp) {
          completions.add(entry.getKey());
        }
      }
    }

    return completions;
  }
}