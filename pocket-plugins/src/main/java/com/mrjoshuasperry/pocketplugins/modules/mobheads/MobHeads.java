package com.mrjoshuasperry.pocketplugins.modules.mobheads;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MobHeads extends Module {
  private static final Map<EntityType, Material> VANILLA_HEADS = new EnumMap<>(Map.of(
      EntityType.CREEPER, Material.CREEPER_HEAD,
      EntityType.SKELETON, Material.SKELETON_SKULL,
      EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL,
      EntityType.ZOMBIE, Material.ZOMBIE_HEAD,
      EntityType.PIGLIN, Material.PIGLIN_HEAD,
      EntityType.ENDER_DRAGON, Material.DRAGON_HEAD));

  private static final Set<EntityType> NATURAL_HEAD_DROPPERS = EnumSet.of(EntityType.WITHER_SKELETON);

  private static final String TEXTURE_URL = "https://textures.minecraft.net/texture/";

  private final double mobChance;
  private final double mobPlayerChance;
  private final double playerChance;
  private final double playerPlayerChance;
  private final double lootingBonus;
  private final boolean lootingAffectsChance;

  private final Set<EntityType> excludedEntities;
  private final Map<EntityType, String> textures;

  public MobHeads(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.mobChance = readableConfig.getDouble("mob-chance", 0.0);
    this.mobPlayerChance = readableConfig.getDouble("mob-player-chance", 2.5);
    this.playerChance = readableConfig.getDouble("player-chance", 0.0);
    this.playerPlayerChance = readableConfig.getDouble("player-player-chance", 100.0);
    this.lootingBonus = readableConfig.getDouble("looting-bonus", 1.0);
    this.lootingAffectsChance = readableConfig.getBoolean("looting-affects-chance", true);

    this.excludedEntities = this.readExcludedEntities(readableConfig);
    this.textures = this.readTextures(readableConfig);
  }

  private Set<EntityType> readExcludedEntities(ConfigurationSection config) {
    Set<EntityType> excluded = EnumSet.noneOf(EntityType.class);

    for (String name : config.getStringList("excluded-entities")) {
      EntityType type = this.parseEntityType(name);
      if (type != null) {
        excluded.add(type);
      }
    }

    return excluded;
  }

  private Map<EntityType, String> readTextures(ConfigurationSection config) {
    Map<EntityType, String> parsed = new EnumMap<>(EntityType.class);

    ConfigurationSection section = config.getConfigurationSection("textures");
    if (section == null) {
      return parsed;
    }

    for (String name : section.getKeys(false)) {
      EntityType type = this.parseEntityType(name);
      String texture = section.getString(name);

      if (type != null && texture != null && !texture.isBlank()) {
        parsed.put(type, texture);
      }
    }

    return parsed;
  }

  private EntityType parseEntityType(String name) {
    NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
    EntityType type = key == null ? null : Registry.ENTITY_TYPE.get(key);

    if (type == null) {
      this.getPlugin().getLogger().warning("Unknown entity type in MobHeads config: " + name);
    }

    return type;
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();

    if (this.excludedEntities.contains(entity.getType())) {
      return;
    }

    // Keep vanilla charged creeper head behavior
    if (isChargedCreeperKill(entity)) {
      return;
    }

    this.stripNaturalHeadDrop(event);

    Player killer = entity.getKiller();
    boolean isPlayerVictim = entity instanceof Player;

    double baseChance;
    if (isPlayerVictim) {
      baseChance = killer == null ? this.playerChance : this.playerPlayerChance;
    } else {
      baseChance = killer == null ? this.mobChance : this.mobPlayerChance;
    }

    int lootingLevel = this.lootingAffectsChance ? getLootingLevel(killer) : 0;
    double chance = chanceFor(baseChance, lootingLevel, this.lootingBonus);

    if (this.getPlugin().getRandom().nextDouble() * 100.0 >= chance) {
      return;
    }

    ItemStack head = this.createHead(entity);
    if (head != null) {
      event.getDrops().add(head);
    }
  }

  private void stripNaturalHeadDrop(EntityDeathEvent event) {
    EntityType type = event.getEntity().getType();
    if (!NATURAL_HEAD_DROPPERS.contains(type)) {
      return;
    }

    Material head = VANILLA_HEADS.get(type);
    List<ItemStack> drops = event.getDrops();

    for (int index = 0; index < drops.size(); index++) {
      if (drops.get(index).getType() == head) {
        drops.remove(index);
        return;
      }
    }
  }

  private static boolean isChargedCreeperKill(LivingEntity entity) {
    return entity.getLastDamageCause() instanceof EntityDamageByEntityEvent cause
        && cause.getDamager() instanceof Creeper creeper
        && creeper.isPowered()
        && cause.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
  }

  private static int getLootingLevel(Player killer) {
    if (killer == null) {
      return 0;
    }

    return killer.getInventory().getItemInMainHand()
        .getEnchantmentLevel(Enchantment.LOOTING);
  }

  static double chanceFor(double baseChance, int lootingLevel, double lootingBonus) {
    double chance = baseChance + (lootingLevel * lootingBonus);
    return Math.max(0.0, Math.min(100.0, chance));
  }

  private ItemStack createHead(LivingEntity entity) {
    if (entity instanceof Player player) {
      return this.createPlayerHead(player);
    }

    Material vanilla = VANILLA_HEADS.get(entity.getType());
    if (vanilla != null) {
      return ItemStack.of(vanilla);
    }

    String texture = this.textures.get(entity.getType());
    if (texture == null) {
      return null;
    }

    return this.createTexturedHead(entity.getType(), texture);
  }

  private ItemStack createPlayerHead(Player player) {
    ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
    head.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(player.getPlayerProfile()));
    return head;
  }

  private ItemStack createTexturedHead(EntityType type, String texture) {
    // Derive the texture from to create a consistent ID
    UUID id = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
    PlayerProfile profile = this.getPlugin().getServer().createProfile(id, null);

    try {
      profile.getTextures().setSkin(URI.create(TEXTURE_URL + texture).toURL());
    } catch (MalformedURLException | IllegalArgumentException ex) {
      this.getPlugin().getLogger().log(Level.WARNING,
          "Invalid head texture configured for " + type.key().value(), ex);
      return null;
    }

    ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
    head.editMeta(SkullMeta.class, meta -> {
      meta.setPlayerProfile(profile);
      meta.itemName(Component.text(headName(type.key().value()), NamedTextColor.WHITE)
          .decoration(TextDecoration.ITALIC, false));
    });

    return head;
  }

  static String headName(String key) {
    StringBuilder name = new StringBuilder();

    for (String word : key.split("_")) {
      if (word.isEmpty()) {
        continue;
      }

      name.append(Character.toUpperCase(word.charAt(0)))
          .append(word.substring(1))
          .append(' ');
    }

    return name.append("Head").toString();
  }
}
