package com.mrjoshuasperry.pocketplugins.additions.improvedMaps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.additions.improvedMaps.commands.MarkersCommand;
import com.mrjoshuasperry.pocketplugins.additions.improvedMaps.renderers.ExplorersAtlasRenderer;
import com.mrjoshuasperry.pocketplugins.additions.improvedMaps.renderers.SepiaAtlasRenderer;
import com.mrjoshuasperry.pocketplugins.utils.CraftingUtil;
import com.mrjoshuasperry.pocketplugins.utils.InventoryUtils;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ExplorersAtlas extends Module {
  private final WaypointManager waypointManager;
  private final NamespacedKey ATLAS_KEY;

  public ExplorersAtlas() {
    super("ExplorersAtlas");

    this.waypointManager = WaypointManager.getInstance();
    ATLAS_KEY = new NamespacedKey(PocketPlugins.getInstance(), "explorer_atlas_id");

    // Register the markers command
    MarkersCommand markersCommand = new MarkersCommand();
    PocketPlugins.getInstance().getCommand("markers").setExecutor(markersCommand);
    PocketPlugins.getInstance().getCommand("markers").setTabCompleter(markersCommand);

    registerCraftingRecipes();
  }

  private void registerCraftingRecipes() {
    Map<Character, Material> craftingRecipe = new HashMap<>();
    craftingRecipe.put('C', Material.COMPASS);
    craftingRecipe.put('M', Material.MAP);
    craftingRecipe.put('P', Material.PAPER);

    ItemStack result = new ItemStack(Material.FILLED_MAP);
    ItemMeta meta = result.getItemMeta();

    meta.displayName(Component.text("Explorer's Atlas").color(NamedTextColor.GOLD));
    meta.lore(
        Arrays.asList(
            Component.text("An explorer's best friend").color(NamedTextColor.GRAY)));

    result.setItemMeta(meta);

    CraftingUtil.addShapedCrafting("explorers_atlas", craftingRecipe, result, "PCP", "CMC",
        "PCP");
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
  public void onCraft(CraftItemEvent event) {
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
