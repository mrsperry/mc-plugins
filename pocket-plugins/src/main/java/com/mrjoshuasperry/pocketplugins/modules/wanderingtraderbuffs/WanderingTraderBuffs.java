package com.mrjoshuasperry.pocketplugins.modules.wanderingtraderbuffs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.MerchantRecipe;

import com.mrjoshuasperry.pocketplugins.utils.Module;

/** @author mrsperry */
public class WanderingTraderBuffs extends Module {
  private final List<TradeSpec> trades;
  private final boolean replaceVanillaTrades;

  public WanderingTraderBuffs(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.replaceVanillaTrades = readableConfig.getBoolean("replace-vanilla-trades", false);
    this.trades = new ArrayList<>();

    List<Map<?, ?>> rawTrades = readableConfig.getMapList("trades");
    for (int index = 0; index < rawTrades.size(); index++) {
      try {
        this.trades.add(TradeSpec.parse(rawTrades.get(index)));
      } catch (IllegalArgumentException ex) {
        this.getPlugin().getLogger()
            .warning("Skipping wandering trader trade #" + (index + 1) + ": " + ex.getMessage());
      }
    }

    if (this.replaceVanillaTrades && this.trades.isEmpty()) {
      this.getPlugin().getLogger().warning(
          "replace-vanilla-trades is set but no valid trades are configured; leaving vanilla trades alone");
    }
  }

  @EventHandler
  public void onWanderingTraderSpawn(CreatureSpawnEvent event) {
    if (this.trades.isEmpty() || !(event.getEntity() instanceof WanderingTrader trader)) {
      return;
    }

    // Vanilla fills in the trader's offers as part of spawning it, so the recipe
    // list is only complete once this tick finishes.
    Bukkit.getScheduler().runTask(this.getPlugin(), () -> this.applyTrades(trader));
  }

  private void applyTrades(WanderingTrader trader) {
    if (!trader.isValid()) {
      return;
    }

    List<MerchantRecipe> recipes = new ArrayList<>();
    if (!this.replaceVanillaTrades) {
      recipes.addAll(trader.getRecipes());
    }

    for (TradeSpec trade : this.trades) {
      recipes.add(trade.toRecipe(this.getPlugin().getRandom()));
    }

    trader.setRecipes(recipes);
  }
}
