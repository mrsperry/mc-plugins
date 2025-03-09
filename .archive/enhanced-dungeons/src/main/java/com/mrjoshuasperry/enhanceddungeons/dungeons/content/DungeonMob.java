package com.mrjoshuasperry.enhanceddungeons.dungeons.content;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.logging.Level;

public class DungeonMob {
    /** A list of all entities spawned with this configuration */
    private final Set<Entity> entities;

    /** The name of the entity */
    private String name;
    /** If the entity's name should always be displayed */
    private boolean nameDisplay;
    /** The number of blocks away a player must be to spawn this entity */
    private int proximity;
    /** The number of blocks away a player must be to turn this entity's AI on */
    private int aiProximity;
    /** The amount of entities to spawn */
    private int count;
    /** The delay between entities spawns */
    private int interval;
    /** A list of tags applied to this entity */
    private final Set<String> tags;
    /** A list of all data specifications to apply to this entity */
    private final Map<String, Object> data;
    /** The type of entities to spawn */
    private EntityType type;
    /** The location to spawn the entities */
    private Location location;
    /** The equipment used to put on the entities */
    private ItemStack[] equipment;
    /** The main hand item for the entities */
    private ItemStack mainHand;
    /** The off hand item for the entities */
    private ItemStack offHand;
    /** If this entity should drop its default items */
    private boolean includeDefaultDrops;
    /** A list of items and their chances that this entity will drop */
    private Map<Double, Set<ItemStack>> drops;
    /** A list of effects to apply to the entities */
    private Set<PotionEffect> effects;

    /** If this mob has already spawned */
    private boolean hasSpawned;
    /** If the mob has AI enabled */
    private boolean hasAI;
    /** The task used to spawn entities at an interval */
    private int spawnTask;

    /** Creates a new DungeonMob */
    public DungeonMob() {
        this.entities = new HashSet<>();

        this.nameDisplay = false;
        this.proximity = -1;
        this.aiProximity = -1;
        this.count = 1;
        this.interval = -1;
        this.tags = new HashSet<>();
        this.data = new HashMap<>();
        this.equipment = new ItemStack[4];
        for (int index = 0; index < 4; index++) {
            this.equipment[index] = new ItemStack(Material.AIR);
        }
        this.mainHand = new ItemStack(Material.AIR);
        this.offHand = new ItemStack(Material.AIR);
        this.includeDefaultDrops = true;
        this.drops = new HashMap<>();

        this.hasSpawned = false;
        this.hasAI = false;
        this.spawnTask = -1;
    }

    /**
     * Creates a copy of a DungeonMob
     * @param template The template to copy
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public DungeonMob(final DungeonMob template) {
        this();

        this.name = template.name;
        this.nameDisplay = template.nameDisplay;
        this.proximity = template.proximity;
        this.aiProximity = template.aiProximity;
        this.count = template.count;
        this.tags.addAll(template.tags);
        this.data.putAll(template.data);
        this.type = template.type;
        this.location = template.location;
        this.equipment = template.equipment;
        this.mainHand = template.mainHand;
        this.offHand = template.offHand;
        this.includeDefaultDrops = template.includeDefaultDrops;
        this.drops.putAll(template.drops);
        this.effects = template.effects;
    }

    /**
     * Checks all proximity detectors on this mob or spawns entities with no proximity if the player is null
     * @param player The player to check the proximities against
     */
    public void checkProximities(final Player player)  {
        // Spawn entities with no proximity if no player is provided
        if (player == null) {
            if (this.proximity == -1) {
                this.prespawn();
            }

            return;
        }

        final double distance = player.getLocation().distance(this.location);

        if (distance <= this.proximity) {
            this.prespawn();
        }

        if (!this.hasAI && distance <= this.aiProximity) {
            this.hasAI = true;

            for (final Entity entity : this.entities) {
                if (entity instanceof LivingEntity) {
                    final LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.setAI(true);
                    livingEntity.setInvulnerable(false);
                }
            }
        }
    }

    /** Checks requirements before spawning the mob */
    private void prespawn() {
        // Ensure the mob doesn't spawn more than once per run
        if (this.hasSpawned) {
            return;
        }

        // Check if there is an interval available
        if (this.interval != -1) {
            // Only have one spawn task running at any time
            if (this.spawnTask == -1) {
                final BukkitScheduler scheduler = Bukkit.getScheduler();

                // Create a new spawn task to spawn entities on a timer
                this.spawnTask = scheduler.runTaskTimer(Main.getInstance(), new Runnable() {
                    private int spawnsLeft = count;

                    @Override
                    public void run() {
                        spawn();

                        if (this.spawnsLeft-- == 0) {
                            scheduler.cancelTask(spawnTask);
                            spawnTask = -1;
                        }
                    }
                }, 0, this.interval).getTaskId();
            }
        } else {
            // Spawn the entities instantly
            for (int index = 0; index < this.count; index++) {
                this.spawn();
            }
        }
    }

    /** Spawns the mob */
    protected Entity spawn() {
        this.hasSpawned = true;

        if (this.location == null) {
            Utils.log(Level.SEVERE, "Mob spawn location was null!");
            return null;
        }

        final World world = this.location.getWorld();
        if (world == null) {
            Utils.log(Level.SEVERE, "Mob spawn world was null!");
            return null;
        }

        final Entity entity = world.spawnEntity(this.location, this.type);
        entity.setPersistent(true);
        this.entities.add(entity);

        if (!(entity instanceof LivingEntity)) {
            return null;
        }

        // Make the entity an adult by default
        if (entity instanceof Ageable) {
            ((Ageable) entity).setAdult();
        }

        // Set entity type specific data
        for (final String key : this.data.keySet()) {
            final Object data = this.data.get(key);

            switch (key) {
                // Handle entities that should be babies
                case "age":
                    if (entity instanceof Ageable) {
                        if (!(boolean) data) {
                            ((Ageable) entity).setBaby();
                        }
                    } else {
                        Utils.log(Level.SEVERE, "Could not set age for entity: " + this.type);
                    }
                    break;
                // Handle villager and zombie villager professions
                case "villager-profession":
                    final Villager.Profession profession = (Villager.Profession) data;

                    if (entity instanceof Villager) {
                        ((Villager) entity).setProfession(profession);
                    } else if (entity instanceof ZombieVillager) {
                        ((ZombieVillager) entity).setVillagerProfession(profession);
                    } else {
                        Utils.log(Level.SEVERE, "Could not set villager profession for entity: " + this.type);
                    }
                    break;
                // Handle villager and zombie villager type (biome they spawn in)
                case "villager-type":
                    final Villager.Type type = (Villager.Type) data;

                    if (entity instanceof Villager) {
                        ((Villager) entity).setVillagerType(type);
                    } else if (entity instanceof ZombieVillager) {
                        ((ZombieVillager) entity).setVillagerType(type);
                    } else {
                        Utils.log(Level.SEVERE, "Could not set villager type for entity: " + this.type);
                    }
                    break;
                // Handle slime sizes
                case "slime-size":
                    if (entity instanceof Slime) {
                        ((Slime) entity).setSize((int) data);
                    } else {
                        Utils.log(Level.SEVERE, "Could not set slime size for entity: " + this.type);
                    }
                    break;
            }
        }

        final LivingEntity livingEntity = (LivingEntity) entity;
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setCanPickupItems(false);

        // Turn off AI if a proximity was set
        if (this.aiProximity != -1) {
            livingEntity.setAI(false);
            livingEntity.setInvulnerable(true);
        } else {
            this.hasAI = true;
        }

        // Play a spawn sound
        world.playSound(this.location, Sound.ENTITY_WITHER_SHOOT, 0.35f, 0.1f);

        // Play spawn particle effects
        final Random random = Main.getRandom();
        for (int index = 0; index < 25; index++) {
            // Set count to 0 to use particle offset
            world.spawnParticle(Particle.EXPLOSION, this.location, 0,
                    // The offset starts in the center of the block and travels upwards
                    random.nextDouble() - 0.5, 1, random.nextDouble() - 0.5, 0.2);
        }

        if (this.name != null) {
            livingEntity.setCustomName(this.name);
            livingEntity.setCustomNameVisible(this.nameDisplay);
        }

        // Set each tag
        final PersistentDataContainer container = livingEntity.getPersistentDataContainer();
        for (final String tag : this.tags) {
            final NamespacedKey key = new NamespacedKey(Main.getInstance(), tag);
            container.set(key, PersistentDataType.BYTE, (byte) 1);
        }

        // Run a task after two ticks to clear the entity and apply custom equipment/effects
        // This is a workaround to Bukkit spawning entities with equipment event when clearing it
        // The alternative to this would be to use NMS and spawn a clean entity but make it version dependent
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            final EntityEquipment equipment = livingEntity.getEquipment();
            if (equipment != null) {
                equipment.clear();
                equipment.setArmorContents(this.equipment);
                equipment.setItemInMainHand(this.mainHand);
                equipment.setItemInOffHand(this.offHand);
            }

            livingEntity.getActivePotionEffects().clear();
            if (this.effects != null) {
                livingEntity.addPotionEffects(this.effects);
            }
        }, 2);

        return entity;
    }

    /** Removes any entities spawned by this configuration */
    public void remove() {
        // Make sure the spawn task is cancelled before removing
        if (this.spawnTask != -1) {
            Bukkit.getScheduler().cancelTask(this.spawnTask);
            this.spawnTask = -1;
        }

        this.hasSpawned = false;

        for (final Entity entity : this.entities) {
            entity.remove();
        }
    }

    /** @return A random list of custom drops for this mob */
    public Set<ItemStack> getCustomDrops() {
        final Set<ItemStack> items = new HashSet<>();
        final Random random = Main.getRandom();

        for (final double chance : this.drops.keySet()) {
            if (chance >= random.nextDouble() * 100) {
                items.addAll(this.drops.get(chance));
            }
        }

        return items;
    }

    /** @param name The name of the entities */
    public void setName(final String name) {
        this.name = name;
    }

    /** @param nameDisplay If the entity's custom name should always be displayed */
    public void setNameDisplay(final boolean nameDisplay) {
        this.nameDisplay = nameDisplay;
    }

    /** @param proximity The number of blocks away a player must be to spawn this entity */
    public void setProximity(final int proximity) {
        this.proximity = proximity;
    }

    /** @param aiProximity The number of blocks away a player must be to turn this entity's AI on */
    public void setAIProximity(final int aiProximity) {
        this.aiProximity = aiProximity;
    }

    /** @param count The number of entities to spawn */
    public void setCount(final int count) {
        this.count = count;
    }

    /** @param interval The delay between entity spawns */
    public void setInterval(final int interval) {
        this.interval = interval;
    }

    /** @param tags A list of tags applied to this entity */
    public void setTags(final Set<String> tags) {
        this.tags.addAll(tags);
    }

    /** @param data A list of entity type specific data to apply */
    public void setData(final Map<String, Object> data) {
        this.data.putAll(data);
    }

    /** @param type The type of entity to spawn */
    public void setType(final EntityType type) {
        this.type = type;
    }

    /** @param location The location to spawn the entities */
    public void setLocation(final Location location) {
        this.location = location;
    }

    /** @param equipment The armor applied to the entities */
    public void setEquipment(final ItemStack[] equipment) {
        this.equipment = equipment;
    }

    /** @param mainHand The main hand item */
    public void setMainHand(final ItemStack mainHand) {
        this.mainHand = mainHand;
    }

    /** @param offHand The off hand item */
    public void setOffHand(final ItemStack offHand) {
        this.offHand = offHand;
    }

    /** @param includeDefaultDrops If this entity should drop its default items */
    public void setIncludeDefaultDrops(final boolean includeDefaultDrops) {
        this.includeDefaultDrops = includeDefaultDrops;
    }

    /** @param drops A list of item stacks and their chance this entity will drop */
    public void setDrops(final Map<Double, Set<ItemStack>> drops) {
        this.drops = drops;
    }

    /** @param effects A list of potion effects to apply to the entities */
    public void setEffects(final Set<PotionEffect> effects) {
        this.effects = effects;
    }

    /**
     * @param tag The tag to check
     * @return If this entity has the specified tag
     */
    public boolean hasTag(final String tag) {
        return this.tags.contains(tag);
    }

    /** @return If this mob has been spawned */
    public boolean hasSpawned() {
        return this.hasSpawned;
    }

    /** @return If default drops should be included when the entity is killed */
    public boolean getIncludeDefaultDrops() {
        return this.includeDefaultDrops;
    }

    /** @return A list of entities spawned by this mob */
    public Set<Entity> getEntities() {
        return this.entities;
    }

    /** @return The number of entities this mob spawns */
    public int getCount() {
        return this.count;
    }
}
