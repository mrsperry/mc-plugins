package com.mrjoshuasperry.miniadditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.miniadditions.additions.armorstands.ArmorStandAdditions;
import com.mrjoshuasperry.miniadditions.additions.cobblegenerator.CobbleGeneratorListener;
import com.mrjoshuasperry.miniadditions.additions.concretemixer.ConcreteMixerListener;
import com.mrjoshuasperry.miniadditions.additions.craftingkeeper.CraftingKeeperListener;
import com.mrjoshuasperry.miniadditions.additions.craftingkeeper.CraftingKeeperManager;
import com.mrjoshuasperry.miniadditions.additions.easypaintings.EasyPaintings;
import com.mrjoshuasperry.miniadditions.additions.easysleep.EasySleepListener;
import com.mrjoshuasperry.miniadditions.additions.experimental.ExperimentalCommands;
import com.mrjoshuasperry.miniadditions.additions.experimental.SoundSynthExperiment;
import com.mrjoshuasperry.miniadditions.additions.featherplucker.FeatherPlucker;
import com.mrjoshuasperry.miniadditions.additions.igneousgenerator.IgneousGeneratorListener;
import com.mrjoshuasperry.miniadditions.additions.improvedshears.ShearListener;
import com.mrjoshuasperry.miniadditions.additions.inventoryinspector.InventoryInspector;
import com.mrjoshuasperry.miniadditions.additions.leadattacher.LeadAttacherListener;
import com.mrjoshuasperry.miniadditions.additions.nameping.NamePing;
import com.mrjoshuasperry.miniadditions.additions.nosheepgriefing.NoSheepGriefingListener;
import com.mrjoshuasperry.miniadditions.additions.slimyboots.SlimyBootsListener;
import com.mrjoshuasperry.miniadditions.additions.woodpile.WoodPileListener;

import io.github.mrsperry.mcutils.ConfigManager;

public class MiniAdditions extends JavaPlugin {
    private static MiniAdditions self;
    private static Random rand;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        self = this;
        rand = new Random();

        saveDefaultConfig();

        ArrayList<Module> modules = Lists.newArrayList(
                new ArmorStandAdditions(),
                new CobbleGeneratorListener(),
                new ConcreteMixerListener(),
                new CraftingKeeperListener(),
                new EasyPaintings(),
                new EasySleepListener(),
                new IgneousGeneratorListener(),
                new ShearListener(),
                new NamePing(),
                new SlimyBootsListener(),
                new WoodPileListener(),
                new LeadAttacherListener(),
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

        loadCrafting();
        initExperimental();
    }

    @Override
    public void onDisable() {
        saveCrafting();
    }

    public static MiniAdditions getInstance() {
        return self;
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

        this.getCommand("shoot").setExecutor(new ExperimentalCommands());
        this.getLogger().info("Experimental Commands enabled");
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
}