package com.mrjoshuasperry.pocketplugins.modules.explorersatlas;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.pocketplugins.modules.explorersatlas.commands.MarkersCommand;
import com.mrjoshuasperry.pocketplugins.modules.explorersatlas.renderers.ExplorersAtlasRenderer;
import com.mrjoshuasperry.pocketplugins.modules.explorersatlas.renderers.SepiaAtlasRenderer;
import com.mrjoshuasperry.pocketplugins.utils.InventoryUtils;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** @author TimPCunningham */
public class ExplorersAtlas extends Module {
  private final NamespacedKey ATLAS_KEY;

  public ExplorersAtlas(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.ATLAS_KEY = this.createKey("explorer-atlas-id");

    // Register the markers command
    MarkersCommand markersCommand = new MarkersCommand(this.getPlugin().getRandom());
    this.getPlugin().getCommand("markers").setExecutor(markersCommand);
    this.getPlugin().getCommand("markers").setTabCompleter(markersCommand);

    this.registerCraftingRecipes();
    this.loadWaypoints();
  }

  @Override
  public void onDisable() {
    super.onDisable();
    this.saveWaypoints();
  }

  // TODO: Update to use new config system
  private void saveWaypoints() {
    WaypointManager manager = WaypointManager.getInstance();
    manager.saveWaypoints(new File(this.getPlugin().getDataFolder(), "waypoints.yml"));
  }

  // TODO: Update to use new config system
  private void loadWaypoints() {
    WaypointManager manager = WaypointManager.getInstance();
    manager.loadWaypoints(new File(this.getPlugin().getDataFolder(), "waypoints.yml"));
  }

  private void registerCraftingRecipes() {
    ItemStack result = new ItemStack(Material.FILLED_MAP);
    ItemMeta meta = result.getItemMeta();

    meta.displayName(Component.text("Explorer's Atlas").color(NamedTextColor.GOLD));
    meta.lore(
        Arrays.asList(
            Component.text("An explorer's best friend").color(NamedTextColor.GRAY)));

    result.setItemMeta(meta);

    ShapedRecipe recipe = new ShapedRecipe(this.createKey("explorers-atlas"), result);
    recipe.shape("PCP", "CMC", "PCP");
    recipe.setIngredient('C', Material.COMPASS);
    recipe.setIngredient('M', Material.MAP);
    recipe.setIngredient('P', Material.PAPER);
    this.registerCraftingRecipe(recipe);
  }

  public boolean isExplorersAtlas(ItemStack item) {
    if (item == null || item.getType() != Material.FILLED_MAP)
      return false;

    return InventoryUtils.hasPersistentData(item, ATLAS_KEY);
  }

  public boolean needsRenderers(ItemStack item) {
    MapMeta meta = (MapMeta) item.getItemMeta();
    MapView view = meta.getMapView();

    if (view == null)
      return false;

    for (MapRenderer renderer : view.getRenderers()) {
      if (renderer instanceof ExplorersAtlasRenderer || renderer instanceof SepiaAtlasRenderer) {
        return false;
      }
    }

    return true;
  }

  public void reattachRenderers(ItemStack item) {
    MapMeta meta = (MapMeta) item.getItemMeta();
    MapView view = meta.getMapView();

    if (view == null)
      return;

    UUID playerUUID = UUID.fromString(meta.getPersistentDataContainer().get(ATLAS_KEY, PersistentDataType.STRING));

    // Clear existing renderers to prevent duplicates
    view.getRenderers().clear();

    // Reattach our renderers
    view.addRenderer(new SepiaAtlasRenderer());
    view.addRenderer(new ExplorersAtlasRenderer(playerUUID));

    // Ensure map settings are correct
    view.setScale(Scale.CLOSEST);
    view.setTrackingPosition(true);
    view.setUnlimitedTracking(true);

    item.setItemMeta(meta);
  }

  @EventHandler
  public void onCraftItem(CraftItemEvent event) {
    if (!(event.getRecipe() instanceof ShapedRecipe))
      return;

    ShapedRecipe recipe = (ShapedRecipe) event.getRecipe();

    if (!recipe.getKey().getKey().equalsIgnoreCase("explorers_atlas"))
      return;

    Player player = (Player) event.getWhoClicked();
    ItemStack result = event.getCurrentItem();
    MapMeta meta = (MapMeta) result.getItemMeta();
    MapView view = Bukkit.createMap(player.getWorld());

    List<Component> lore = meta.lore();
    lore.add(Component.text("Atlas Owner: " + player.getName()).color(NamedTextColor.AQUA));
    meta.lore(lore);

    view.setScale(Scale.CLOSEST);
    view.setCenterX(player.getLocation().getBlockX());
    view.setCenterZ(player.getLocation().getBlockZ());
    view.setTrackingPosition(true);
    view.setUnlimitedTracking(true);
    view.addRenderer(new SepiaAtlasRenderer());
    view.addRenderer(new ExplorersAtlasRenderer(player.getUniqueId()));

    // Store UUID in persistent data
    meta.getPersistentDataContainer().set(
        ATLAS_KEY,
        PersistentDataType.STRING,
        player.getUniqueId().toString());
    meta.setMapView(view);
    result.setItemMeta(meta);
  }

  public void validateMaps(List<ItemStack> maps) {
    for (ItemStack item : maps) {
      if (isExplorersAtlas(item) && needsRenderers(item)) {
        reattachRenderers(item);
      }
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    List<ItemStack> maps = InventoryUtils.getItemsByType(event.getPlayer().getInventory(), Material.FILLED_MAP);
    validateMaps(maps);
  }

  @EventHandler
  public void onItemPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player))
      return;

    validateMaps(Arrays.asList((ItemStack) event.getItem().getItemStack()));
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    List<ItemStack> maps = InventoryUtils.getItemsByType(event.getInventory(), Material.FILLED_MAP);
    validateMaps(maps);
  }
}
