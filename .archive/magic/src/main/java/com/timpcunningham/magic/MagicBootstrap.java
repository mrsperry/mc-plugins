package com.timpcunningham.magic;

import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public class MagicBootstrap implements PluginBootstrap {
  @Override
  public void bootstrap(@NotNull BootstrapContext context) {
    final RegistryKeySet<ItemType> items = RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.BLAZE_ROD,
        ItemTypeKeys.BONE,
        ItemTypeKeys.STICK);

    context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> {
      event.registry().register(
          TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("magic:magically_imbued")),
          ench -> ench.description(Component.text("Magically Imbued"))
              .supportedItems(items)
              .anvilCost(30)
              .maxLevel(5)
              .weight(5)
              .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
              .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
              .activeSlots(EquipmentSlotGroup.HAND));
    }));

  }
}
