package com.mrjoshuasperry.pocketplugins.utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

public class NMSModule extends Module {
  private static final String VERSION = Bukkit.getBukkitVersion();
  protected static final Map<String, Class<? extends Module>> nmsModuleHandlers = new HashMap<>();
  private Module loadedModule;

  static {
    // Add module versions here.
    // nmsModuleHandlers.put("V_1_21_R1", MyModule.class)
  }

  public NMSModule(String name) {
    super(name);
  }

  @Override
  public void init(YamlConfiguration configuration) {
    super.init(configuration);

    // See if we even need to go through the trouble initializing.
    if (this.isEnabled()) {

      // Module will have a static delcaration adding to map of supported versions
      if (isVersionSupported()) {

        // Find correct class version
        Class<? extends Module> handlerClzz = nmsModuleHandlers.get(VERSION);

        try {

          // Get constructor matching Module(String, NMSModule) { ... }
          // Initialize with (moduleName_VERSION, this)
          this.loadedModule = handlerClzz.getDeclaredConstructor(String.class, NMSModule.class)
              .newInstance(this.getName() + "_" + VERSION, this);

          // Make sure this module is initialized as well
          this.loadedModule.init(configuration);
        } catch (Exception e) {

          // Cleanup if there is an error intializing module version
          Bukkit.getLogger()
              .warning("There was a problem enabling module \"" + this.getName() + "_" + VERSION + "\" disabling...");
          Bukkit.getLogger().warning(e.getMessage());
          e.printStackTrace();
          this.disableModule();
        }
      } else {
        Bukkit.getLogger().info("Module version for " + VERSION + " not found!");
        this.disableModule();
      }
    }
  }

  public boolean isVersionSupported() {
    return nmsModuleHandlers.containsKey(VERSION);
  }
}
