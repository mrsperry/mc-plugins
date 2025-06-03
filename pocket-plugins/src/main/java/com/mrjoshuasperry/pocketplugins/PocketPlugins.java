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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.pocketplugins.utils.DebuggerDisplay;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class PocketPlugins extends JavaPlugin {
    private Random random = new Random();
    private List<Module> modules;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.modules = this.loadModules();
    }

    @Override
    public void onDisable() {
        DebuggerDisplay.removeAll();

        for (Module module : this.modules) {
            try {
                module.onDisable();
            } catch (Exception ex) {
                this.getLogger().severe("Failed to disable module: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
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

                    String moduleName = clazz.getSimpleName().toLowerCase();
                    YamlConfiguration writableConfig = this.loadWritableConfig(moduleName);

                    ConfigurationSection readableSection = this.getConfig().getConfigurationSection(moduleName);
                    ConfigurationSection writableSection = writableConfig.getRoot();

                    if (readableSection == null) {
                        YamlConfiguration config = new YamlConfiguration();
                        config.set("enabled", true);
                        readableSection = config.getRoot();
                    }

                    boolean enabled = readableSection.getBoolean("enabled", true);

                    if (!enabled) {
                        continue;
                    }
                    
                    try {
                        Module moduleInstance = (Module) clazz
                                .getDeclaredConstructor(ConfigurationSection.class, ConfigurationSection.class)
                                .newInstance(readableSection,
                                        writableSection);
                        modules.add(moduleInstance);
                    } catch (Exception ex) {
                        this.getLogger().severe("Failed to load module: " + ex.getMessage());
                        ex.printStackTrace();
                    }
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
}