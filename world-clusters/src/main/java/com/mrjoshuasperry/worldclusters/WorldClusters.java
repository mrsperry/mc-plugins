package com.mrjoshuasperry.worldclusters;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.mcutils.confirm.ConfirmationManager;
import com.mrjoshuasperry.mcutils.menu.MenuManager;
import com.mrjoshuasperry.worldclusters.cluster.ClusterRegistry;
import com.mrjoshuasperry.worldclusters.command.WcCommand;
import com.mrjoshuasperry.worldclusters.command.WorldsCommand;
import com.mrjoshuasperry.worldclusters.perms.ClusterPermissions;
import com.mrjoshuasperry.worldclusters.profile.BoundaryListener;
import com.mrjoshuasperry.worldclusters.profile.BoundaryService;
import com.mrjoshuasperry.worldclusters.profile.ProfileStore;
import com.mrjoshuasperry.worldclusters.travel.PortalListener;
import com.mrjoshuasperry.worldclusters.travel.RespawnListener;
import com.mrjoshuasperry.worldclusters.travel.TeleportService;
import com.mrjoshuasperry.worldclusters.world.WorldManager;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

/**
 * Groups worlds into clusters that share one player state, with a hard boundary
 * between clusters.
 *
 * <p>
 * See {@code README.md} for the config format and the reasoning behind the
 * cluster model.
 */
public class WorldClusters extends JavaPlugin {
    private static final String CLUSTERS_KEY = "clusters";
    private static final String TIMEOUT_KEY = "settings.confirmation-timeout-seconds";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private ClusterRegistry registry;
    private ProfileStore store;
    private ClusterPermissions permissions;
    private BoundaryService boundary;
    private WorldManager worlds;
    private TeleportService teleports;
    private MenuManager menus;
    private ConfirmationManager confirmations;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.registry = new ClusterRegistry(this.getLogger());
        this.registry.load(this.getConfig().getConfigurationSection(CLUSTERS_KEY));

        this.store = new ProfileStore(this);
        this.permissions = new ClusterPermissions(this, this.registry);
        this.boundary = new BoundaryService(this.registry, this.store, this.permissions);
        this.worlds = new WorldManager(this, this.registry);
        this.teleports = new TeleportService(this.registry, this.boundary);

        this.menus = new MenuManager(this);
        this.confirmations = new ConfirmationManager(this);
        this.confirmations.registerCommands();
        this.confirmations.setTimeoutSeconds(
                this.getConfig().getInt(TIMEOUT_KEY, DEFAULT_TIMEOUT_SECONDS));

        this.worlds.loadManagedWorlds();

        this.getServer().getPluginManager().registerEvents(
                new BoundaryListener(this.registry, this.store, this.boundary, this.permissions), this);
        this.getServer().getPluginManager().registerEvents(new PortalListener(this.registry), this);
        this.getServer().getPluginManager().registerEvents(new RespawnListener(this.registry), this);

        this.registerCommands();
    }

    @Override
    public void onDisable() {
        if (this.store != null) {
            this.store.saveAll();
        }
        if (this.menus != null) {
            this.menus.shutdown();
        }
        if (this.confirmations != null) {
            this.confirmations.shutdown();
        }
    }

    private void registerCommands() {
        WorldsCommand worldsCommand = new WorldsCommand(this.registry, this.teleports, this);
        WcCommand wcCommand = new WcCommand(this, this.registry, this.worlds, this.teleports, this.confirmations);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                (ReloadableRegistrarEvent<Commands> event) -> {
                    event.registrar().register(worldsCommand.build().build(),
                            "Opens the world picker");
                    event.registrar().register(wcCommand.build().build(),
                            "Manages worlds and clusters");
                });
    }

    /** Writes the registry back to config.yml. */
    public void saveClusters() {
        ConfigurationSection clusters = this.getConfig().getConfigurationSection(CLUSTERS_KEY);
        if (clusters == null) {
            clusters = this.getConfig().createSection(CLUSTERS_KEY);
        }

        this.registry.save(clusters);
        this.saveConfig();
    }

    /** Re-reads config.yml and re-applies permissions to everyone online. */
    public void reloadClusters() {
        this.reloadConfig();
        this.registry.load(this.getConfig().getConfigurationSection(CLUSTERS_KEY));
        this.confirmations.setTimeoutSeconds(
                this.getConfig().getInt(TIMEOUT_KEY, DEFAULT_TIMEOUT_SECONDS));

        this.worlds.loadManagedWorlds();
        this.permissions.refreshAll();
    }

    public ClusterRegistry getRegistry() {
        return this.registry;
    }

    public ProfileStore getStore() {
        return this.store;
    }

    public BoundaryService getBoundary() {
        return this.boundary;
    }

    public WorldManager getWorlds() {
        return this.worlds;
    }

    public TeleportService getTeleports() {
        return this.teleports;
    }
}
