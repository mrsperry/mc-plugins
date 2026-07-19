package com.mrjoshuasperry.pocketplugins.modules.biomebombs;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mojang.brigadier.Command;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** @author TimPCunningham */
public class BiomeBombs extends Module {
  private NamespacedKey biomeBombTypeKey;
  private NamespacedKey biomeBombColorKey;
  private int explosionRange;
  private List<BiomeBombData> biomeBombsData;

  public BiomeBombs(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.biomeBombTypeKey = this.createKey("biome-bomb-type");
    this.biomeBombColorKey = this.createKey("biome-bomb-color");
    this.explosionRange = readableConfig.getInt("bomb-range");
  
    this.registerCraftingRecipes(readableConfig, writableConfig);

    BiomeBombCommand biomeBombCommand = new BiomeBombCommand(this.biomeBombsData);
    this.registerCommand(() -> Commands.literal("biomebombs")
        .executes(context -> {
          biomeBombCommand.sendBombList(context.getSource().getSender());
          return Command.SINGLE_SUCCESS;
        }), "Lists all available biome bombs and their crafting ingredients", List.of("bb"));
  }

  private void registerCraftingRecipes(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    // Assigned before any early return below, since the command reads it whether
    // or not there turned out to be anything to craft
    this.biomeBombsData = new ArrayList<>();

    Logger logger = this.getPlugin().getLogger();
    ConfigurationSection biomeConfigSection = writableConfig.getConfigurationSection("biomes");
    if (biomeConfigSection == null) {
      logger.warning("No biomes are configured; no biome bombs will be craftable");
      return;
    }

    int craftingAmount = readableConfig.getInt("crafting-output");

    for (String key : biomeConfigSection.getKeys(false)) {
      ConfigurationSection biomeSection = biomeConfigSection.getConfigurationSection(key);
      if (biomeSection == null) {
        logger.warning("Skipping biome bomb '" + key + "': not a configuration section");
        continue;
      }

      try {
        biomeBombsData.add(new BiomeBombData(biomeSection));
      } catch (IllegalArgumentException ex) {
        logger.warning("Skipping biome bomb '" + key + "': " + ex.getMessage());
      }
    }

    for (BiomeBombData data : biomeBombsData) {
      ItemStack result = new ItemStack(Material.FIREWORK_STAR);
      FireworkEffectMeta meta = (FireworkEffectMeta) result.getItemMeta();

      meta.displayName(
          Component.text(data.getBiomeName()).color(data.getTextColor())
              .append(Component.text(" Biome Bomb").color(NamedTextColor.GRAY)));
      meta.lore(List.of(
          Component.text("Type: ").color(NamedTextColor.GRAY).append(
              Component.text(data.getBiomeName()).color(NamedTextColor.GOLD))));

      PersistentDataContainer container = meta.getPersistentDataContainer();
      container.set(this.biomeBombTypeKey, PersistentDataType.STRING, data.getBiomeType());
      container.set(this.biomeBombColorKey, PersistentDataType.INTEGER, data.getColor().asARGB());

      FireworkEffect effect = FireworkEffect.builder().withColor(data.getColor()).build();
      meta.setEffect(effect);

      result.setAmount(craftingAmount);
      result.setItemMeta(meta);

      ShapelessRecipe recipe = new ShapelessRecipe(this.createKey("biome-bomb-" + data.getBiomeType()), result);
      recipe.addIngredient(Material.EGG);
      recipe.addIngredient(8, data.getCatalyst());
      this.registerCraftingRecipe(recipe);
    }
  }

  @EventHandler
  public void biomeBombUse(PlayerInteractEvent event) {
    if (event.getHand() == EquipmentSlot.OFF_HAND || event.getItem() == null) {
      return;
    }

    ItemStack item = event.getItem();
    if (item == null) {
      return;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return;
    }

    PersistentDataContainer container = meta.getPersistentDataContainer();

    String type = container.get(biomeBombTypeKey, PersistentDataType.STRING);
    Integer color = container.get(biomeBombColorKey, PersistentDataType.INTEGER);

    // Both keys are written together, but an item carrying only one of them (an
    // older bomb, or one edited by hand) would otherwise unbox null and throw.
    if (type == null || color == null)
      return;

    BiomeBombProjectile projectile = new BiomeBombProjectile(this.explosionRange, type, Color.fromARGB(color),
        item.clone());

    int amount = item.getAmount();
    item.setAmount(amount - 1);

    projectile.launchProjectile(event.getPlayer());
  }

}
