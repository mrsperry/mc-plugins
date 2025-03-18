package com.mrjoshuasperry.pocketplugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.ConfigManager;
import com.mrjoshuasperry.pocketplugins.additions.armorstands.ArmorStandAdditions;
import com.mrjoshuasperry.pocketplugins.additions.autoBreeding.AutoBreeding;
import com.mrjoshuasperry.pocketplugins.additions.beeplanter.BeePlanter;
import com.mrjoshuasperry.pocketplugins.additions.biomebombs.BiombeBomb;
import com.mrjoshuasperry.pocketplugins.additions.cobblegenerator.CobbleGeneratorListener;
import com.mrjoshuasperry.pocketplugins.additions.commandmacros.Macros;
import com.mrjoshuasperry.pocketplugins.additions.concretemixer.ConcreteMixerListener;
import com.mrjoshuasperry.pocketplugins.additions.craftingkeeper.CraftingKeeperListener;
import com.mrjoshuasperry.pocketplugins.additions.craftingkeeper.CraftingKeeperManager;
import com.mrjoshuasperry.pocketplugins.additions.easypaintings.EasyPaintings;
import com.mrjoshuasperry.pocketplugins.additions.easysleep.EasySleepListener;
import com.mrjoshuasperry.pocketplugins.additions.experimental.SoundSynthExperiment;
import com.mrjoshuasperry.pocketplugins.additions.featherplucker.FeatherPlucker;
import com.mrjoshuasperry.pocketplugins.additions.igneousgenerator.IgneousGeneratorListener;
import com.mrjoshuasperry.pocketplugins.additions.improvedMaps.ExplorersAtlas;
import com.mrjoshuasperry.pocketplugins.additions.improvedMaps.WaypointManager;
import com.mrjoshuasperry.pocketplugins.additions.improvedshears.ShearListener;
import com.mrjoshuasperry.pocketplugins.additions.inventoryinspector.InventoryInspector;
import com.mrjoshuasperry.pocketplugins.additions.leadattacher.LeadAttacherListener;
import com.mrjoshuasperry.pocketplugins.additions.nameping.NamePing;
import com.mrjoshuasperry.pocketplugins.additions.noendermangriefing.NoEndermanGriefing;
import com.mrjoshuasperry.pocketplugins.additions.nosheepgriefing.NoSheepGriefingListener;
import com.mrjoshuasperry.pocketplugins.additions.slimyboots.SlimyBootsListener;
import com.mrjoshuasperry.pocketplugins.additions.woodpile.WoodPileListener;
import com.mrjoshuasperry.pocketplugins.utils.DebuggerDisplay;
import com.mrjoshuasperry.pocketplugins.utils.Module;

public class PocketPlugins extends JavaPlugin implements Listener {
    private static Random rand = new Random();
    private ConfigManager configManager;
    private List<NamespacedKey> registeredCraftingKeys;

    @Override
    public void onEnable() {
        this.registeredCraftingKeys = new ArrayList<>();
        saveDefaultConfig();

        ArrayList<Module> modules = Lists.newArrayList(
                new AutoBreeding(),
                new BeePlanter(),
                new ArmorStandAdditions(),
                new BiombeBomb(),
                new CobbleGeneratorListener(),
                new ConcreteMixerListener(),
                new CraftingKeeperListener(),
                new EasyPaintings(),
                new EasySleepListener(),
                new ExplorersAtlas(),
                new IgneousGeneratorListener(),
                new ShearListener(),
                new Macros(),
                new NamePing(),
                new SlimyBootsListener(),
                new WoodPileListener(),
                new LeadAttacherListener(),
                new NoEndermanGriefing(),
                new NoSheepGriefingListener(),
                new FeatherPlucker(),
                new InventoryInspector());
        ArrayList<String> names = new ArrayList<>();
        for (Module module : modules) {
            names.add(module.getName());
        }

        this.configManager = new ConfigManager(this, names, true);

        for (Module module : modules) {
            YamlConfiguration config = this.configManager.getConfig(module.getName().toLowerCase());
            module.init(config);
        }

        loadWaypoints();
        loadCrafting();
        initExperimental();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveWaypoints();
        saveCrafting();
        DebuggerDisplay.getInstance().removeAll();
    }

    public static PocketPlugins getInstance() {
        return JavaPlugin.getPlugin(PocketPlugins.class);
    }

    public static Random getRandom() {
        return rand;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public void initExperimental() {
        this.getCommand("synth").setExecutor(new SoundSynthExperiment());
        this.getLogger().info("Sound Synth enabled");
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