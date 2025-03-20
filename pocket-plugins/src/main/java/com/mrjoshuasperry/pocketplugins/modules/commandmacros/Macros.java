package com.mrjoshuasperry.pocketplugins.modules.commandmacros;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class Macros extends Module {
  private final Map<String, MacroData> macros;

  public Macros() {
    super("commandmacros");
    this.macros = new HashMap<>();
  }

  @Override
  public void initialize(YamlConfiguration config) {
    super.initialize(config);
    loadMacros(config);
    MacrosCommand executor = new MacrosCommand(macros);
    this.getPlugin().getCommand("macro").setExecutor(executor);
    this.getPlugin().getCommand("macro").setTabCompleter(executor);
  }

  private void loadMacros(YamlConfiguration config) {
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
