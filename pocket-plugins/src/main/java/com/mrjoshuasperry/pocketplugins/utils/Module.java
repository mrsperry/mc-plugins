package com.mrjoshuasperry.pocketplugins.utils;

import java.util.ArrayList;
import java.util.Collection;
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

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/**
 * Base class for every pocket-plugins module.
 *
 * <p>
 * Modules are discovered by scanning the jar at runtime rather than from a
 * registry, which imposes conventions the compiler cannot enforce:
 *
 * <ul>
 * <li>The class name must match its package name, case-insensitively:
 * {@code CropTweaks} must live in package {@code croptweaks}, or it is not
 * picked up as a module at all.
 * <li>It must declare a
 * {@code (ConfigurationSection readable, ConfigurationSection writable)}
 * constructor.
 * <li>Its readable config is the lowercased class name's section of
 * {@code config.yml}; its writable config is
 * {@code configs/<lowercased class name>.yml}.
 * </ul>
 *
 * <p>
 * Subclasses must not name an event handler the same as one declared here.
 * Bukkit collects handlers by reflection, so a matching name silently replaces
 * the base handler rather than adding to it. Base handlers are {@code final} to
 * turn that into a compile error; note they cannot be made {@code private}
 * instead, as Bukkit would then stop finding them on subclass instances.
 */
public abstract class Module implements Listener {
    private final String name;

    private PocketPlugins plugin;

    private List<NamespacedKey> craftingKeys;
    private ConfigurationSection readableConfig;
    private ConfigurationSection writableConfig;
    private boolean enabled;

    /**
     * Must not register events or otherwise hand out {@code this}: subclasses
     * assign their own fields only after this returns, so anything reaching the
     * module before then sees them unset. The loader calls {@link #onEnable()}
     * once construction is complete.
     */
    public Module(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        this.name = this.getClass().getSimpleName();

        this.plugin = PocketPlugins.getInstance();

        this.craftingKeys = new ArrayList<>();
        this.readableConfig = readableConfig;
        this.writableConfig = writableConfig;
        this.enabled = readableConfig.getBoolean("enabled", true);
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

    /**
     * The description and aliases are what {@code plugin.yml} used to carry for a
     * command; registering here keeps them next to the command itself.
     */
    public final void registerCommand(Supplier<LiteralArgumentBuilder<CommandSourceStack>> commandSupplier,
            String description, Collection<String> aliases) {
        this.registerCommand(commandSupplier.get().build(), description, aliases);
    }

    public final void registerCommand(LiteralCommandNode<CommandSourceStack> command, String description,
            Collection<String> aliases) {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                (ReloadableRegistrarEvent<Commands> event) -> event.registrar().register(command, description,
                        aliases));
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
    public final void discoverRecipesOnJoin(PlayerJoinEvent event) {
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
