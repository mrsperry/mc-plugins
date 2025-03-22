package com.mrjoshuasperry.pocketplugins.modules.customrecipes;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.menu.Menu;
import com.mrjoshuasperry.mcutils.menu.items.DecorMenuItem;
import com.mrjoshuasperry.mcutils.menu.items.MenuItem;
import com.mrjoshuasperry.mcutils.menu.items.StaticMenuItem;
import com.mrjoshuasperry.mcutils.menu.items.SwappableMenuItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RecipeCreationMenu implements Listener {
  private final List<Integer> craftingSlots = Lists.newArrayList(10, 11, 12, 19, 20, 21, 23, 28, 29, 30);
  private final int outputSlot = 23;
  private final int categorySlot = 16;
  private final int recipeTypeSlot = 17;
  private final int confirmSlot = 35;
  private final int cancelSlot = 34;
  private final int totalSlots = 45;

  protected Menu menu;

  protected boolean isShapelessRecipe;
  protected CraftingBookCategory category;

  public RecipeCreationMenu(JavaPlugin plugin) {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    this.menu = new Menu(Component.text("New Recipe"), this.totalSlots);

    this.isShapelessRecipe = true;
    this.category = CraftingBookCategory.MISC;

    MenuItem filler = new DecorMenuItem(Material.BLACK_STAINED_GLASS_PANE);

    MenuItem confirm = new StaticMenuItem(
        new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .setName(Component.text("Create recipe", NamedTextColor.GREEN))
            .build(),
        (Player player, Menu menu) -> {
          Inventory inventory = this.menu.getInventory();

          ItemStack result = inventory.getItem(this.outputSlot);
          if (result == null) {
            player.sendMessage(Component.text("You must have a result item in the output slot.", NamedTextColor.RED));
            return;
          }

          List<ItemStack> ingredients = Lists.newArrayList();
          for (int slot : this.craftingSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
              ingredients.add(item);
            }
          }

          if (ingredients.isEmpty()) {
            player.sendMessage(
                Component.text("You must have at least one ingredient in the crafting grid.", NamedTextColor.RED));
            return;
          }

          CraftingRecipe recipe;
          if (this.isShapelessRecipe) {
            recipe = new ShapelessRecipe(new NamespacedKey(plugin, "test"), result);
          } else {
            recipe = new ShapedRecipe(new NamespacedKey(plugin, "test"), result);
          }

          recipe.setCategory(this.category);
          CustomRecipes.addRecipe(recipe);

          RecipeListMenu newMenu = new RecipeListMenu(plugin);
          newMenu.open(player);
        });

    MenuItem cancel = new StaticMenuItem(
        new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
            .setName(Component.text("Cancel", NamedTextColor.RED))
            .build(),
        (Player player, Menu menu) -> {
          RecipeListMenu newMenu = new RecipeListMenu(plugin);
          newMenu.open(player);
        });

    MenuItem category = new SwappableMenuItem(
        Lists.newArrayList(
            new ItemBuilder(Material.BRICKS)
                .setName(Component.text("Category: Building"))
                .build(),
            new ItemBuilder(Material.IRON_AXE)
                .setName(Component.text("Category: Equipment"))
                .build(),
            new ItemBuilder(Material.LAVA_BUCKET)
                .setName(Component.text("Category: Misc"))
                .build(),
            new ItemBuilder(Material.REDSTONE)
                .setName(Component.text("Category: Redstone"))
                .build()),
        (Player player, Menu menu) -> {
          switch (this.menu.getInventory().getItem(this.recipeTypeSlot).getType()) {
            case BRICKS:
              this.category = CraftingBookCategory.BUILDING;
              break;
            case IRON_AXE:
              this.category = CraftingBookCategory.EQUIPMENT;
              break;
            case REDSTONE:
              this.category = CraftingBookCategory.REDSTONE;
              break;
            default:
              this.category = CraftingBookCategory.MISC;
              break;
          }
        });

    MenuItem recipeType = new SwappableMenuItem(
        Lists.newArrayList(
            new ItemBuilder(Material.WATER_BUCKET)
                .setName(Component.text("Recipe type: Shapeless"))
                .build(),
            new ItemBuilder(Material.BUCKET)
                .setName(Component.text("Recipe type: Shaped"))
                .build()),
        (Player player, Menu menu) -> {
          this.isShapelessRecipe = !this.isShapelessRecipe;
        });

    this.menu.fillInventory(filler);
    this.menu.setItem(this.categorySlot, category);
    this.menu.setItem(this.recipeTypeSlot, recipeType);
    this.menu.setItem(this.cancelSlot, cancel);
    this.menu.setItem(this.confirmSlot, confirm);

    for (int slot : this.craftingSlots) {
      this.menu.clearItem(slot);
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
    if (!this.craftingSlots.contains(slot)) {
      event.setCancelled(true);
      this.menu.clickedSlot((Player) event.getWhoClicked(), slot);
      return;
    }
  }
}
