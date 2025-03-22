package com.mrjoshuasperry.pocketplugins.modules.customrecipes;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.menu.Menu;
import com.mrjoshuasperry.mcutils.menu.items.DecorMenuItem;
import com.mrjoshuasperry.mcutils.menu.items.MenuItem;
import com.mrjoshuasperry.mcutils.menu.items.StaticMenuItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class RecipeListMenu implements Listener {
  protected static int recipesPerPage = 21;

  protected Menu menu;

  public RecipeListMenu(JavaPlugin plugin) {
    this(plugin, 1);
  }

  public RecipeListMenu(JavaPlugin plugin, int page) {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    List<CraftingRecipe> recipes = CustomRecipes.getRecipes();
    int maxPages = (int) Math.ceil((double) recipes.size() / (double) RecipeListMenu.recipesPerPage);
    int size = 27 + ((int) Math.ceil(RecipeListMenu.recipesPerPage / 9d) * 9);

    TextComponent.Builder title = Component.text().append(Component.text("Custom Recipes"));

    if (maxPages > 1) {
      title.append(Component.text(" - (" + page + "/" + maxPages + ")"));
    }

    this.menu = new Menu(title.build(), size);

    MenuItem filler = new DecorMenuItem(Material.BLACK_STAINED_GLASS_PANE);

    MenuItem nextPage = new StaticMenuItem(
        new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .setName(Component.text("Next page"))
            .build(),
        (Player player, Menu menu) -> {
          RecipeListMenu newMenu = new RecipeListMenu(plugin, page + 1);
          newMenu.open(player);
        });

    MenuItem previousPage = new StaticMenuItem(
        new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
            .setName(Component.text("Previous page"))
            .build(),
        (Player player, Menu menu) -> {
          RecipeListMenu newMenu = new RecipeListMenu(plugin, page - 1);
          newMenu.open(player);
        });

    MenuItem createRecipe = new StaticMenuItem(
        new ItemBuilder(Material.NETHER_STAR)
            .setName(Component.text("Create new recipe"))
            .build(),
        (Player player, Menu menu) -> {
          RecipeCreationMenu newMenu = new RecipeCreationMenu(plugin);
          newMenu.open(player);
        });

    this.menu.fillInventory(filler);
    this.menu.clearItems(19, 25);
    this.menu.clearItems(28, 34);
    this.menu.clearItems(37, 43);
    this.menu.setItem(4, createRecipe);

    if (page > 1) {
      this.menu.setItem(0, previousPage);
    }

    if (page < maxPages - 1) {
      this.menu.setItem(8, nextPage);
    }

    for (int index = 0; index < RecipeListMenu.recipesPerPage; index++) {
      int recipeIndex = (page - 1) * RecipeListMenu.recipesPerPage + index;

      if (recipeIndex >= recipes.size()) {
        break;
      }

      CraftingRecipe recipe = recipes.get(recipeIndex);
      ItemStack item = recipe.getResult();

      MenuItem recipeItem = new StaticMenuItem(
          item,
          (Player player, Menu menu) -> {
            player.sendMessage(Component.text("Recipe: " + recipe.getKey().getKey()));
          });

      this.menu.setItem(19 + index, recipeItem);
    }
  }

  public void open(Player player) {
    player.openInventory(this.menu.getInventory());
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory inventory = event.getClickedInventory();

    if (inventory == null) {
      return;
    }

    if (!(inventory.getHolder() instanceof Menu)) {
      return;
    }

    int slot = event.getSlot();
    this.menu.clickedSlot((Player) event.getWhoClicked(), slot);
    event.setCancelled(true);
  }
}
