package com.mrjoshuasperry.worldclusters.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Everything about a player that belongs to one cluster.
 *
 * <p>
 * This is the unit that gets swapped at a boundary: captured from the player on
 * the way out of a cluster and applied on the way back in, so the two sides
 * never share anything.
 *
 * <p>
 * Statistics and advancements are deliberately <b>not</b> included — they are
 * server-global in vanilla and splitting them would break progression tracking
 * in ways players wouldn't expect.
 */
public class PlayerProfile {
    private List<ItemStack> inventory = new ArrayList<>();
    private List<ItemStack> enderChest = new ArrayList<>();
    private int heldSlot;

    private double health = 20.0D;
    private double maxHealth = 20.0D;
    private double absorption;
    private int foodLevel = 20;
    private float saturation = 5.0F;
    private float exhaustion;
    private int remainingAir;
    private int maximumAir = 300;
    private float fallDistance;
    private int fireTicks;
    private int freezeTicks;

    private int level;
    private float exp;
    private int totalExperience;

    private GameMode gameMode = GameMode.SURVIVAL;
    private boolean allowFlight;
    private boolean flying;

    private List<PotionEffect> potionEffects = new ArrayList<>();

    private Location lastLocation;
    private Location respawnLocation;

    /** Snapshots a player's current state. */
    public static PlayerProfile capture(Player player) {
        PlayerProfile profile = new PlayerProfile();

        // getContents is all 41 slots: main inventory, armor and offhand.
        profile.inventory = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        profile.enderChest = new ArrayList<>(Arrays.asList(player.getEnderChest().getContents()));
        profile.heldSlot = player.getInventory().getHeldItemSlot();

        profile.health = player.getHealth();
        profile.maxHealth = maxHealthOf(player);
        profile.absorption = player.getAbsorptionAmount();
        profile.foodLevel = player.getFoodLevel();
        profile.saturation = player.getSaturation();
        profile.exhaustion = player.getExhaustion();
        profile.remainingAir = player.getRemainingAir();
        profile.maximumAir = player.getMaximumAir();
        profile.fallDistance = player.getFallDistance();
        profile.fireTicks = player.getFireTicks();
        profile.freezeTicks = player.getFreezeTicks();

        profile.level = player.getLevel();
        profile.exp = player.getExp();
        profile.totalExperience = player.getTotalExperience();

        profile.gameMode = player.getGameMode();
        profile.allowFlight = player.getAllowFlight();
        profile.flying = player.isFlying();

        profile.potionEffects = new ArrayList<>(player.getActivePotionEffects());

        profile.lastLocation = player.getLocation();
        profile.respawnLocation = player.getRespawnLocation();

        return profile;
    }

    /**
     * Writes this profile onto a player.
     *
     * <p>
     * Order matters and is load-bearing:
     * <ul>
     * <li>game mode before flight, because changing mode resets
     * {@code allowFlight}</li>
     * <li>max health before health, because health clamps to the current
     * maximum</li>
     * <li>existing effects cleared before new ones, or the old ones linger</li>
     * </ul>
     */
    public void apply(Player player) {
        player.getInventory().setContents(this.inventory.toArray(new ItemStack[0]));
        player.getEnderChest().setContents(this.enderChest.toArray(new ItemStack[0]));
        player.getInventory().setHeldItemSlot(Math.clamp(this.heldSlot, 0, 8));

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(this.maxHealth);
        }
        player.setHealth(Math.clamp(this.health, 0.0D, this.maxHealth));
        player.setAbsorptionAmount(this.absorption);
        player.setFoodLevel(this.foodLevel);
        player.setSaturation(this.saturation);
        player.setExhaustion(this.exhaustion);
        player.setMaximumAir(this.maximumAir);
        player.setRemainingAir(this.remainingAir);
        player.setFallDistance(this.fallDistance);
        player.setFireTicks(this.fireTicks);
        player.setFreezeTicks(this.freezeTicks);

        player.setLevel(this.level);
        player.setExp(this.exp);
        player.setTotalExperience(this.totalExperience);

        player.setGameMode(this.gameMode);
        player.setAllowFlight(this.allowFlight);
        player.setFlying(this.allowFlight && this.flying);

        for (PotionEffect effect : List.copyOf(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : this.potionEffects) {
            player.addPotionEffect(effect);
        }

        player.setRespawnLocation(this.respawnLocation, true);
    }

    /**
     * A profile for a player who has never been to a cluster: empty inventory,
     * no XP, full health, the cluster's default game mode.
     */
    public static PlayerProfile fresh(GameMode gameMode) {
        PlayerProfile profile = new PlayerProfile();
        profile.gameMode = gameMode;
        profile.allowFlight = gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
        profile.remainingAir = profile.maximumAir;
        return profile;
    }

    private static double maxHealthOf(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 20.0D : attribute.getBaseValue();
    }

    public void writeTo(ConfigurationSection section) {
        section.set("inventory", this.inventory);
        section.set("ender-chest", this.enderChest);
        section.set("held-slot", this.heldSlot);

        section.set("health", this.health);
        section.set("max-health", this.maxHealth);
        section.set("absorption", this.absorption);
        section.set("food-level", this.foodLevel);
        section.set("saturation", this.saturation);
        section.set("exhaustion", this.exhaustion);
        section.set("remaining-air", this.remainingAir);
        section.set("maximum-air", this.maximumAir);
        section.set("fall-distance", this.fallDistance);
        section.set("fire-ticks", this.fireTicks);
        section.set("freeze-ticks", this.freezeTicks);

        section.set("level", this.level);
        section.set("exp", this.exp);
        section.set("total-experience", this.totalExperience);

        section.set("gamemode", this.gameMode.name());
        section.set("allow-flight", this.allowFlight);
        section.set("flying", this.flying);

        section.set("potion-effects", this.potionEffects);

        section.set("last-location", this.lastLocation);
        section.set("respawn-location", this.respawnLocation);
    }

    @SuppressWarnings("unchecked")
    public static PlayerProfile readFrom(ConfigurationSection section) {
        PlayerProfile profile = new PlayerProfile();

        profile.inventory = new ArrayList<>((List<ItemStack>) (List<?>) section.getList("inventory", List.of()));
        profile.enderChest = new ArrayList<>((List<ItemStack>) (List<?>) section.getList("ender-chest", List.of()));
        profile.heldSlot = section.getInt("held-slot");

        profile.maxHealth = section.getDouble("max-health", 20.0D);
        profile.health = section.getDouble("health", profile.maxHealth);
        profile.absorption = section.getDouble("absorption");
        profile.foodLevel = section.getInt("food-level", 20);
        profile.saturation = (float) section.getDouble("saturation", 5.0D);
        profile.exhaustion = (float) section.getDouble("exhaustion");
        profile.maximumAir = section.getInt("maximum-air", 300);
        profile.remainingAir = section.getInt("remaining-air", profile.maximumAir);
        profile.fallDistance = (float) section.getDouble("fall-distance");
        profile.fireTicks = section.getInt("fire-ticks");
        profile.freezeTicks = section.getInt("freeze-ticks");

        profile.level = section.getInt("level");
        profile.exp = (float) section.getDouble("exp");
        profile.totalExperience = section.getInt("total-experience");

        profile.gameMode = parseGameMode(section.getString("gamemode"));
        profile.allowFlight = section.getBoolean("allow-flight");
        profile.flying = section.getBoolean("flying");

        profile.potionEffects = new ArrayList<>(
                (List<PotionEffect>) (List<?>) section.getList("potion-effects", List.of()));

        profile.lastLocation = section.getLocation("last-location");
        profile.respawnLocation = section.getLocation("respawn-location");

        return profile;
    }

    private static GameMode parseGameMode(String name) {
        if (name == null) {
            return GameMode.SURVIVAL;
        }

        try {
            return GameMode.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return GameMode.SURVIVAL;
        }
    }

    public Location getLastLocation() {
        return this.lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    public Location getRespawnLocation() {
        return this.respawnLocation;
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    // ArrayList copies rather than List.copyOf: an inventory has null entries for
    // empty slots, and List.copyOf rejects nulls.
    public List<ItemStack> getInventory() {
        return new ArrayList<>(this.inventory);
    }

    public List<ItemStack> getEnderChest() {
        return new ArrayList<>(this.enderChest);
    }

    public int getHeldSlot() {
        return this.heldSlot;
    }

    public double getHealth() {
        return this.health;
    }

    public double getMaxHealth() {
        return this.maxHealth;
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public int getLevel() {
        return this.level;
    }

    public float getExp() {
        return this.exp;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public List<PotionEffect> getPotionEffects() {
        return List.copyOf(this.potionEffects);
    }

    public int getFireTicks() {
        return this.fireTicks;
    }

    public float getFallDistance() {
        return this.fallDistance;
    }
}
