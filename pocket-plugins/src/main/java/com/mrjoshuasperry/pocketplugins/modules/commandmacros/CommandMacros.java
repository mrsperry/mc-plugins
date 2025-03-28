package com.mrjoshuasperry.pocketplugins.modules.commandmacros;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author TimPCunningham */
public class CommandMacros extends Module {
  private final Map<String, MacroData> macros;

  public CommandMacros(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
    this.macros = new HashMap<>();

    this.loadMacros(writableConfig);
    MacrosCommand executor = new MacrosCommand(macros);
    this.getPlugin().getCommand("macro").setExecutor(executor);
    this.getPlugin().getCommand("macro").setTabCompleter(executor);
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
