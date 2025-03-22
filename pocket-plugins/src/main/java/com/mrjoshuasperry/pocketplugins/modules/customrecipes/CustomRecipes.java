package com.mrjoshuasperry.pocketplugins.modules.customrecipes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingRecipe;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

// recipes:
// clean stained glass
// quartz blocks
// cobweb
// name tags
//chain mail

public class CustomRecipes extends Module implements BasicCommand {
  protected static List<CraftingRecipe> recipes = new ArrayList<>();

  public CustomRecipes() {
    super("CustomRecipes");

    this.registerBasicCommand("customrecipes", this);
  }

  @Override
  public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super.initialize(readableConfig, writableConfig);
  }

  @Override
  public void execute(CommandSourceStack commandSourceStack, String[] args) {
    CommandSender sender = commandSourceStack.getSender();

    if (args.length != 0) {
      sender.sendMessage(Component.text("Invalid arguments, usage: /customrecipes", NamedTextColor.RED));
      return;
    }

    if (!(sender instanceof Player)) {
      sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
      return;
    }

    Player player = (Player) sender;
    RecipeListMenu menu = new RecipeListMenu(this.getPlugin(), 1);
    menu.open(player);
  }

  public static void addRecipe(CraftingRecipe recipe) {
    recipes.add(recipe);
  }

  public static List<CraftingRecipe> getRecipes() {
    return recipes;
  }
}
