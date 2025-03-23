package com.mrjoshuasperry.pocketplugins.modules.biomebombs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.pocketplugins.utils.CraftingUtil;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/** @author TimPCunningham */
public class BiomeBombs extends Module {
  private final NamespacedKey biomeBombTypeKey;
  private final NamespacedKey biomeBombColorKey;
  private int explosionRage;
  private List<BiomeBombData> biomeBombsData;

  public BiomeBombs() {
    super("BiomeBombs");
    biomeBombTypeKey = this.createKey("biomb_bomb_type");
    biomeBombColorKey = this.createKey("biomb_bomb_color");
  }

  @Override
  public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super.initialize(readableConfig, writableConfig);

    this.explosionRage = readableConfig.getInt("bomb-range");

    registerCraftingRecipes(readableConfig, writableConfig);
    this.getPlugin().getCommand("biomebombs").setExecutor(new BiomeBombCommand(biomeBombsData));
  }

  private void registerCraftingRecipes(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    ConfigurationSection biomeConfigSection = writableConfig.getConfigurationSection("biomes");
    biomeBombsData = new ArrayList<>();
    int craftingAmount = readableConfig.getInt("crafting-output");

    for (String key : biomeConfigSection.getKeys(false)) {
      biomeBombsData.add(new BiomeBombData(biomeConfigSection.getConfigurationSection(key)));
    }

    for (BiomeBombData data : biomeBombsData) {
      ItemStack result = new ItemStack(Material.FIREWORK_STAR);
      FireworkEffectMeta meta = (FireworkEffectMeta) result.getItemMeta();
      Map<Material, Integer> ingredients = new HashMap<>();

      ingredients.put(Material.EGG, 1);
      ingredients.put(data.getCatalyst(), 8);

      meta.displayName(
          Component.text(data.getBiomeName()).color(data.getTextColor())
              .append(Component.text(" Biome Bomb").color(NamedTextColor.GRAY)));
      meta.lore(Arrays.asList(
          Component.text("Type: ").color(NamedTextColor.GRAY).append(
              Component.text(data.getBiomeName()).color(NamedTextColor.GOLD)),
          Component.text("biome_bomb_type:" + data.getBiomeType()),
          Component.text("biome_bomb_color:" + data.getColor().asARGB())));

      FireworkEffect effect = FireworkEffect.builder().withColor(data.getColor()).build();
      meta.setEffect(effect);

      result.setAmount(craftingAmount);
      result.setItemMeta(meta);

      CraftingUtil.addShapelessCrafting("BIOME_BOMB_" + data.getBiomeType(), ingredients, result);
    }
  }

  @EventHandler
  public void onItemCraft(CraftItemEvent event) {
    ItemStack result = event.getCurrentItem();

    if (!result.displayName().toString().contains("Biome Bomb")) {
      return;
    }

    ItemMeta meta = result.getItemMeta();
    PersistentDataContainer container = meta.getPersistentDataContainer();

    List<Component> lore = meta.lore();
    List<Component> updatedLore = new ArrayList<>();

    for (Component comp : lore) {
      TextComponent text = (TextComponent) comp;
      String loreLine = text.content();

      if (loreLine.contains("biome_bomb_type")) {
        String type = loreLine.split(":")[1];
        container.set(biomeBombColorKey, PersistentDataType.STRING, type);
      } else if (loreLine.contains("biome_bomb_color")) {
        int color = Integer.parseInt(loreLine.split(":")[1]);
        container.set(biomeBombColorKey, PersistentDataType.INTEGER, color);
      } else {
        updatedLore.add(comp);
      }
    }

    meta.lore(updatedLore);
    result.setItemMeta(meta);
  }

  @EventHandler
  public void biomeBombUse(PlayerInteractEvent event) {
    if (event.getHand() == EquipmentSlot.OFF_HAND || event.getItem() == null)
      return;

    ItemStack item = event.getItem();
    if (item == null) {
      return;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return;
    }

    PersistentDataContainer container = meta.getPersistentDataContainer();

    if (!container.has(biomeBombTypeKey, PersistentDataType.STRING))
      return;

    String type = (String) container.get(biomeBombTypeKey, PersistentDataType.STRING);
    int color = (int) container.get(biomeBombColorKey, PersistentDataType.INTEGER);

    BiomeBombProjectile projectile = new BiomeBombProjectile(this.explosionRage, type, Color.fromARGB(color),
        item.clone());

    int amount = item.getAmount();
    item.setAmount(amount - 1);

    projectile.launchProjectile(event.getPlayer());
  }

}
