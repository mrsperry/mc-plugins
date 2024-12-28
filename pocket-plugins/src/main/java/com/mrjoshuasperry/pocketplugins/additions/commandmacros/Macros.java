package com.mrjoshuasperry.pocketplugins.additions.commandmacros;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class Macros extends Module {
  private final Map<String, MacroData> macros;
  private PocketPlugins plugin;

  public Macros() {
    super("commandmacros");
    this.macros = new HashMap<>();
    this.plugin = PocketPlugins.getInstance();
  }

  @Override
  public void init(YamlConfiguration config) {
    super.init(config);
    loadMacros(config);
    MacrosCommand executor = new MacrosCommand(macros);
    this.plugin.getCommand("macro").setExecutor(executor);
    this.plugin.getCommand("macro").setTabCompleter(executor);
  }

  private void loadMacros(YamlConfiguration config) {
    ConfigurationSection macrosSection = config.getConfigurationSection("macros");
    if (macrosSection == null)
      return;

    for (String key : macrosSection.getKeys(false)) {
      ConfigurationSection macroSection = macrosSection.getConfigurationSection(key);
      if (macroSection != null) {
        macros.put(key.toLowerCase(), new MacroData(
            macroSection.getBoolean("op_only"),
            macroSection.getStringList("commands")));
      }
    }
  }
}
