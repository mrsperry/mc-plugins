package com.mrjoshuasperry.pocketplugins.modules.commandmacros;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MacrosCommand {
  private static final String OP_PERMISSION = "pocketplugins.macros.op";

  private final Map<String, MacroData> macros;

  public MacrosCommand(Map<String, MacroData> macros) {
    this.macros = macros;
  }

  public void listMacros(CommandSender sender) {
    sender.sendMessage(
        Component.text("════ Command Macros ════").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    sender.sendMessage(Component.empty());

    boolean isOp = canUseOpMacros(sender);

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

  public void runMacro(CommandSender sender, String macroName) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can run macros!").color(NamedTextColor.RED));
      return;
    }

    // Macros are keyed lowercase, but the name is whatever the player typed
    MacroData macro = macros.get(macroName.toLowerCase());

    if (macro == null) {
      player.sendMessage(Component.text("That macro doesn't exist!").color(NamedTextColor.RED));
      return;
    }

    if (macro.isOpOnly() && !canUseOpMacros(player)) {
      player.sendMessage(Component.text("You don't have permission to use this macro!").color(NamedTextColor.RED));
      return;
    }

    for (String cmd : macro.getCommands()) {
      player.performCommand(cmd);
    }
  }

  /**
   * Brigadier does not filter suggestions against what has been typed so far, so
   * the prefix match here is what keeps the list narrowing as the player types.
   */
  public CompletableFuture<Suggestions> suggestMacroNames(CommandSender sender, SuggestionsBuilder builder) {
    String typed = builder.getRemainingLowerCase();
    boolean isOp = canUseOpMacros(sender);

    for (Map.Entry<String, MacroData> entry : macros.entrySet()) {
      if (entry.getValue().isOpOnly() && !isOp) {
        continue;
      }

      if (entry.getKey().startsWith(typed)) {
        builder.suggest(entry.getKey());
      }
    }

    return builder.buildFuture();
  }

  // The console is not a Player but holds every permission, so it still sees everything
  private boolean canUseOpMacros(CommandSender sender) {
    return sender.hasPermission(OP_PERMISSION);
  }
}
