package com.mrjoshuasperry.pocketplugins.modules.mobheads;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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
  private final Map<EntityType, VariantTextures> textures;

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

  private Map<EntityType, VariantTextures> readTextures(ConfigurationSection config) {
    Map<EntityType, VariantTextures> parsed = new EnumMap<>(EntityType.class);

    ConfigurationSection section = config.getConfigurationSection("textures");
    if (section == null) {
      return parsed;
    }

    for (String name : section.getKeys(false)) {
      EntityType type = this.parseEntityType(name);
      if (type == null) {
        continue;
      }

      VariantTextures variants = readVariantTextures(section, name);
      if (variants != null) {
        parsed.put(type, variants);
      }
    }

    return parsed;
  }

  /**
   * A texture entry is either a scalar hash (one skin for every variant) or a
   * section with a {@code default} hash plus per-variant overrides keyed by the
   * variant name, e.g. {@code warm}/{@code cold} for a cow or {@code light_blue}
   * for a sheep. Unlisted variants fall back to {@code default}.
   */
  private static VariantTextures readVariantTextures(ConfigurationSection parent, String name) {
    ConfigurationSection nested = parent.getConfigurationSection(name);
    if (nested == null) {
      String texture = parent.getString(name);
      return isBlank(texture) ? null : new VariantTextures(texture, Map.of());
    }

    String defaultTexture = nested.getString("default");
    if (isBlank(defaultTexture)) {
      defaultTexture = null;
    }

    Map<String, String> byVariant = new HashMap<>();
    for (String variant : nested.getKeys(false)) {
      if (variant.equals("default")) {
        continue;
      }

      String texture = nested.getString(variant);
      if (!isBlank(texture)) {
        byVariant.put(variant.toLowerCase(Locale.ROOT), texture);
      }
    }

    if (defaultTexture == null && byVariant.isEmpty()) {
      return null;
    }

    return new VariantTextures(defaultTexture, byVariant);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
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

    EntityType type = entity.getType();
    Material vanilla = VANILLA_HEADS.get(type);
    if (vanilla != null) {
      return ItemStack.of(vanilla);
    }

    VariantTextures variants = this.textures.get(type);
    if (variants == null) {
      return null;
    }

    String variant = variantKey(entity).orElse(null);
    String texture = variants.resolve(variant);
    if (texture == null) {
      return null;
    }

    // Name after the variant only when it has its own skin, so a fallback head
    // stays "Cow Head" rather than mislabelling a temperate cow as, say, warm.
    String label = variants.hasVariant(variant)
        ? variant + "_" + type.key().value()
        : type.key().value();

    return this.createTexturedHead(label, texture);
  }

  /**
   * The variant identifier for mobs that carry one, matching the keys used under
   * a texture entry. Registry-backed variants (cow, pig, chicken, frog, wolf,
   * cat) use their registry key; the rest use their enum name lowercased.
   */
  private static Optional<String> variantKey(LivingEntity entity) {
    return switch (entity) {
      case Cow cow -> Optional.of(keyOf(cow.getVariant()));
      case Pig pig -> Optional.of(keyOf(pig.getVariant()));
      case Chicken chicken -> Optional.of(keyOf(chicken.getVariant()));
      case Frog frog -> Optional.of(keyOf(frog.getVariant()));
      case Wolf wolf -> Optional.of(keyOf(wolf.getVariant()));
      case Cat cat -> Optional.of(keyOf(cat.getCatType()));
      case CopperGolem golem -> Optional.of(enumKey(golem.getWeatheringState()));
      case MushroomCow mooshroom -> Optional.of(enumKey(mooshroom.getVariant()));
      case Axolotl axolotl -> Optional.of(enumKey(axolotl.getVariant()));
      case Sheep sheep -> Optional.ofNullable(sheep.getColor()).map(MobHeads::enumKey);
      case Fox fox -> Optional.of(enumKey(fox.getFoxType()));
      case Rabbit rabbit -> Optional.of(enumKey(rabbit.getRabbitType()));
      case Parrot parrot -> Optional.of(enumKey(parrot.getVariant()));
      case Llama llama -> Optional.of(enumKey(llama.getColor()));
      default -> Optional.empty();
    };
  }

  private static String keyOf(Keyed keyed) {
    return keyed.key().value();
  }

  private static String enumKey(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }

  private ItemStack createPlayerHead(Player player) {
    ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
    head.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(player.getPlayerProfile()));
    return head;
  }

  private ItemStack createTexturedHead(String label, String texture) {
    // Derive the texture into a consistent ID so the same skin reuses one profile
    UUID id = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
    PlayerProfile profile = this.getPlugin().getServer().createProfile(id, null);

    // PlayerTextures.setSkin(url) silently no-ops on a nameless/incomplete profile,
    // leaving the head with a default skin. Encode the textures property directly,
    // which is the form the client actually reads, and it applies unconditionally.
    String texturesJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + TEXTURE_URL + texture + "\"}}}";
    String encoded = Base64.getEncoder().encodeToString(texturesJson.getBytes(StandardCharsets.UTF_8));
    profile.setProperty(new ProfileProperty("textures", encoded));

    ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
    head.editMeta(SkullMeta.class, meta -> {
      meta.setPlayerProfile(profile);
      // A player head derives its name ("%s's Head") from the profile, which overrides
      // item_name; custom_name wins over that, so set the label there instead.
      meta.displayName(Component.text(headName(label), NamedTextColor.WHITE)
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

  /**
   * The texture hash(es) for one mob: a {@code default} used when a mob has no
   * variant or an unlisted one, plus optional per-variant overrides.
   */
  private record VariantTextures(String defaultTexture, Map<String, String> byVariant) {
    String resolve(String variant) {
      if (variant != null) {
        String texture = this.byVariant.get(variant);
        if (texture != null) {
          return texture;
        }
      }

      return this.defaultTexture;
    }

    boolean hasVariant(String variant) {
      return variant != null && this.byVariant.containsKey(variant);
    }
  }
}
