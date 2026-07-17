package com.mrjoshuasperry.pocketplugins.modules.commandmacros;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.command.brigadier.Commands;

/** @author TimPCunningham */
public class CommandMacros extends Module {
  private final Map<String, MacroData> macros;

  public CommandMacros(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
    this.macros = new HashMap<>();

    this.loadMacros(writableConfig);

    MacrosCommand macrosCommand = new MacrosCommand(this.macros);
    this.registerCommand(() -> Commands.literal("macro")
        .then(Commands.literal("list")
            .executes(context -> {
              macrosCommand.listMacros(context.getSource().getSender());
              return Command.SINGLE_SUCCESS;
            }))
        .then(Commands.literal("run")
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> macrosCommand
                    .suggestMacroNames(context.getSource().getSender(), builder))
                .executes(context -> {
                  macrosCommand.runMacro(context.getSource().getSender(),
                      StringArgumentType.getString(context, "name"));
                  return Command.SINGLE_SUCCESS;
                }))),
        "Command macro management", List.of());
  }

  private void loadMacros(ConfigurationSection config) {
    ConfigurationSection macrosSection = config.getConfigurationSection("macros");
    if (macrosSection == null)
      return;

    for (String key : macrosSection.getKeys(false)) {
      ConfigurationSection macroSection = macrosSection.getConfigurationSection(key);
      if (macroSection != null) {
        macros.put(key.toLowerCase(), new MacroData(
            macroSection.getBoolean("op-only", false),
            macroSection.getStringList("commands")));
      }
    }
  }
}
