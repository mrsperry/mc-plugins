package com.mrjoshuasperry.pocketplugins.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingRecipe;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class Module implements Listener {
    private final String name;

    private PocketPlugins plugin;

    private List<NamespacedKey> craftingKeys;
    private ConfigurationSection readableConfig;
    private ConfigurationSection writableConfig;
    private boolean enabled;

    public Module(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        this.name = this.getClass().getSimpleName();

        this.plugin = PocketPlugins.getInstance();

        this.craftingKeys = new ArrayList<>();
        this.readableConfig = readableConfig;
        this.writableConfig = writableConfig;
        this.enabled = readableConfig.getBoolean("enabled", true);

        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public void onEnable() {
        this.plugin.getLogger().info(this.name + " enabled!");
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    public void onDisable() {
        this.plugin.getLogger().info(this.name + " disabled!");
        HandlerList.unregisterAll(this);
        this.saveConfig();
    }

    public final void enableModule() {
        this.enabled = true;
        this.onEnable();
    }

    public final void disableModule() {
        this.enabled = false;
        this.onDisable();
    }

    public final void registerCommand(Supplier<LiteralArgumentBuilder<CommandSourceStack>> commandSupplier) {
        this.registerCommand(commandSupplier.get().build());
    }

    public final void registerCommand(LiteralCommandNode<CommandSourceStack> command) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                (ReloadableRegistrarEvent<Commands> event) -> event.registrar().register(command));
    }

    // TODO: remove in favor of non-basic commands
    public final void registerBasicCommand(String command, BasicCommand commandClass) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                (ReloadableRegistrarEvent<Commands> event) -> event.registrar().register(command, commandClass));
    }

    public final void saveConfig() {
        YamlConfiguration config = new YamlConfiguration();
        Set<String> keys = this.writableConfig.getKeys(false);

        if (keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            config.set(key, this.writableConfig.get(key));
        }

        try {
            config.save(this.plugin.getDataFolder() + "/configs/" + this.name.toLowerCase() + ".yml");
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Could not save " + this.name + " configuration!");
            ex.printStackTrace();
        }
    }

    public final NamespacedKey createKey(String name) {
        return new NamespacedKey(this.plugin, name);
    }

    public void registerCraftingRecipe(CraftingRecipe recipe) {
        this.craftingKeys.add(recipe.getKey());
        this.getPlugin().getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (NamespacedKey key : this.craftingKeys) {
            if (!event.getPlayer().hasDiscoveredRecipe(key)) {
                event.getPlayer().discoverRecipe(key);
            }
        }
    }

    public final String getModuleName() {
        return this.name;
    }

    public final PocketPlugins getPlugin() {
        return this.plugin;
    }

    public final ConfigurationSection getReadableConfig() {
        return this.readableConfig;
    }

    public final ConfigurationSection getWritableConfig() {
        return this.writableConfig;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }
}
