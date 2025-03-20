package com.mrjoshuasperry.pocketplugins.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class NMSModule extends Module {
  protected static final Map<String, Class<? extends Module>> nmsModuleHandlers;

  protected final String bukkitVersion;

  static {
    nmsModuleHandlers = new HashMap<>();

    // Add module versions here.
    // nmsModuleHandlers.put("V_1_21_R1", MyModule.class)
  }

  public NMSModule(String name) {
    super(name);

    this.bukkitVersion = Bukkit.getBukkitVersion();
  }

  @Override
  public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super.initialize(readableConfig, writableConfig);

    if (!this.isEnabled()) {
      return;
    }

    Logger logger = this.getPlugin().getLogger();

    if (!NMSModule.nmsModuleHandlers.containsKey(bukkitVersion)) {
      logger.warning(this.getModuleName() + " version for " + bukkitVersion + " not found!");
      this.disableModule();
      return;
    }

    Class<? extends Module> handlerClass = nmsModuleHandlers.get(bukkitVersion);
    try {
      Module module = handlerClass.getDeclaredConstructor(String.class, NMSModule.class)
          .newInstance(this.getModuleName() + "_" + bukkitVersion, this);

      module.initialize(readableConfig, writableConfig);
    } catch (Exception ex) {
      logger.warning("An error occurred while enabling: \"" + this.getModuleName() + "_" + bukkitVersion);
      ex.printStackTrace();
      this.disableModule();
    }
  }
}
