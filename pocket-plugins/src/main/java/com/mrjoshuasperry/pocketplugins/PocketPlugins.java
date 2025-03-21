package com.mrjoshuasperry.pocketplugins;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.pocketplugins.modules.craftingkeeper.CraftingKeeperManager;
import com.mrjoshuasperry.pocketplugins.modules.explorersatlas.WaypointManager;
import com.mrjoshuasperry.pocketplugins.utils.DebuggerDisplay;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class PocketPlugins extends JavaPlugin implements Listener {
    private Random random = new Random();

    private List<NamespacedKey> registeredCraftingKeys;

    @Override
    public void onEnable() {
        this.registeredCraftingKeys = new ArrayList<>();

        this.saveDefaultConfig();

        List<Module> modules = this.loadModules();

        for (Module module : modules) {
            String moduleName = module.getModuleName().toLowerCase();
            YamlConfiguration writableConfig = this.loadWritableConfig(moduleName);

            ConfigurationSection readableSection = this.getConfig().getConfigurationSection(moduleName);
            ConfigurationSection writableSection = writableConfig.getRoot();

            if (readableSection == null) {
                YamlConfiguration config = new YamlConfiguration();
                config.set("enabled", true);
                readableSection = config.getRoot();
            }

            module.initialize(readableSection, writableSection);
        }

        loadWaypoints();
        loadCrafting();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveWaypoints();
        saveCrafting();
        DebuggerDisplay.getInstance().removeAll();
    }

    private List<Module> loadModules() {
        List<Module> modules = new ArrayList<>();

        try {
            String modulesPath = this.getClass().getPackageName().replace(".", "/") + "/modules";
            List<URL> resources = Collections.list(this.getClassLoader().getResources(modulesPath));

            for (URL url : resources) {
                if (!url.getProtocol().equals("jar")) {
                    continue;
                }

                JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                JarFile jarFile = jarConnection.getJarFile();

                List<JarEntry> entries = Collections.list(jarFile.entries());
                for (JarEntry entry : entries) {
                    String entryName = entry.getName();

                    if (!entryName.startsWith(modulesPath)) {
                        continue;
                    }

                    if (!entryName.endsWith(".class")) {
                        continue;
                    }

                    String fullClassName = entryName.replace("/", ".").replace(".class", "");
                    String[] parts = fullClassName.split("\\.");

                    String className = parts[parts.length - 1];
                    String packageName = parts[parts.length - 2];

                    if (!className.equalsIgnoreCase(packageName)) {
                        continue;
                    }

                    Class<?> clazz = this.getClassLoader().loadClass(fullClassName);
                    if (!Module.class.isAssignableFrom(clazz)) {
                        continue;
                    }

                    Module moduleInstance = (Module) clazz.getDeclaredConstructor().newInstance();
                    modules.add(moduleInstance);
                }
            }
        } catch (Exception ex) {
            this.getLogger().severe("Failed to load module: " + ex.getMessage());
            ex.printStackTrace();
        }

        return modules;
    }

    private YamlConfiguration loadWritableConfig(String configName) {
        String resourceName = "configs/" + configName + ".yml";
        File dataFile = new File(this.getDataFolder(), resourceName);

        if (!dataFile.exists() && this.getResource(resourceName) != null) {
            dataFile.getParentFile().mkdirs();
            this.saveResource("configs/" + configName + ".yml", false);
        }

        return YamlConfiguration.loadConfiguration(dataFile);
    }

    public static PocketPlugins getInstance() {
        return JavaPlugin.getPlugin(PocketPlugins.class);
    }

    public Random getRandom() {
        return this.random;
    }

    private void saveCrafting() {
        CraftingKeeperManager manager = CraftingKeeperManager.getInstance();
        FileConfiguration config = new YamlConfiguration();

        config.set("tables", manager);
        try {
            config.save(new File(getDataFolder(), "crafting_tables.yml"));
        } catch (Exception e) {
            getLogger().warning("Error saving crafting tables!");
        }
    }

    private void loadCrafting() {
        try {
            FileConfiguration config = YamlConfiguration
                    .loadConfiguration(new File(getDataFolder(), "crafting_tables.yml"));
            config.get("tables");
        } catch (Exception e) {
            getLogger().warning("Error loading crafting tables!");
            e.printStackTrace();
        }
    }

    private void saveWaypoints() {
        WaypointManager manager = WaypointManager.getInstance();
        manager.saveWaypoints(new File(getDataFolder(), "waypoints.yml"));
    }

    private void loadWaypoints() {
        WaypointManager manager = WaypointManager.getInstance();
        manager.loadWaypoints(new File(getDataFolder(), "waypoints.yml"));
    }

    public void addDiscoverableCraftingKey(NamespacedKey key) {
        this.registeredCraftingKeys.add(key);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (NamespacedKey key : registeredCraftingKeys) {
            if (!event.getPlayer().hasDiscoveredRecipe(key)) {
                event.getPlayer().discoverRecipe(key);
            }
        }
    }
}