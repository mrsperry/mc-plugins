package com.mrjoshuasperry.deathchest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mrjoshuasperry.deathchest.DeathPile.Orbit;
import com.mrjoshuasperry.deathchest.DeathPile.Slot;
import com.mrjoshuasperry.mcutils.audit.Audit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Owns the live death piles: spawns them, keeps an in-memory registry keyed by group
 * UUID, orbits their items on a repeating task, and hands stacks back on pickup. The
 * registry is rebuilt from entity PDC on enable and as chunks load, so piles survive
 * restarts.
 *
 * <p>
 * Besides its stacks, a pile has the owner's head spinning at its center and their
 * name above it. The name is hidden by default and shown only to a player who is
 * looking at one of the stacks from close enough to pick it up.
 */
public class DeathPileManager {
    /** Ticks between orbit updates. Displays interpolate across the gap, so this can be coarse. */
    private static final long UPDATE_INTERVAL = 2L;
    /**
     * Ticks each display interpolates toward its new spot. Kept longer than
     * {@link #UPDATE_INTERVAL} on purpose: the client is then always still
     * interpolating when the next update lands, so it never reaches the target and
     * stalls for a tick — that stall is what read as jitter.
     */
    private static final int TELEPORT_DURATION = 4;
    /** Ticks for one full revolution. */
    private static final double REVOLUTION_TICKS = 200.0;
    private static final double PHASE_STEP = 2.0 * Math.PI / REVOLUTION_TICKS * UPDATE_INTERVAL;

    private static final float ITEM_SCALE = 0.375f;
    // Block item-models render as full 3D cubes, so they look about twice the size of a
    // flat item sprite at the same scale; halving the block scale evens them out.
    private static final float BLOCK_SCALE = ITEM_SCALE / 2f;
    private static final float HITBOX_WIDTH = 0.5f;
    private static final float HITBOX_HEIGHT = 0.5f;

    /** Half the head item's own size, so a quarter-block head inside the rings. */
    private static final float HEAD_SCALE = 0.5f;
    /** A head item model sits on the floor of its space; lifting it by half its height centers it. */
    private static final float HEAD_LIFT = 0.25f * HEAD_SCALE;
    /** A little bigger than the quarter-block head, so it's easy to aim at. */
    private static final float HEAD_HITBOX_SIZE = 0.35f;

    private static final String ROLE_HEAD = "head";
    private static final String ROLE_LABEL = "label";
    /** Players further than this from a pile can't be in reach of it, so skip their ray trace. */
    private static final double LABEL_CHECK_RADIUS_SQUARED = 8 * 8;
    /** Survival entity reach, for a player somehow missing the attribute. */
    private static final double DEFAULT_REACH = 3.0;

    /**
     * Chance per update that a pile sheds an end rod sparkle: one or two a second. End
     * rod particles have no size setting, so frequency is the only way to tone them down.
     */
    private static final double SPARKLE_CHANCE = 0.15;
    /** How hard a sparkle starts falling; end rod particles slow down and fade on their own. */
    private static final double SPARKLE_FALL_SPEED = 0.04;

    private final Main plugin;
    private final Map<UUID, Group> groups = new HashMap<>();
    private double phase;

    /** A tracked stack: its hitbox and its fixed slot in the ring. */
    private record Member(UUID interactionId, Slot slot) {
    }

    /** All stacks of one death, sharing a center, plus its head and name tag. */
    private static final class Group {
        private final Location center;
        private final Map<UUID, Member> members = new HashMap<>();
        private UUID ownerId;
        private UUID headId;
        private UUID headHitboxId;
        private UUID labelId;

        private Group(Location center) {
            this.center = center;
        }

        private boolean isHitbox(Entity entity) {
            UUID id = entity.getUniqueId();
            return id.equals(headHitboxId)
                    || members.values().stream().anyMatch(member -> member.interactionId().equals(id));
        }
    }

    public DeathPileManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Adopts already-loaded piles and starts the orbit task. Call once from onEnable. */
    public void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Display display : world.getEntitiesByClass(Display.class)) {
                adopt(display);
            }
        }

        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    /** Spawns a pile for the player's inventory at their death spot. No-op if empty-handed. */
    public void spawn(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                stacks.add(item);
            }
        }

        if (stacks.isEmpty()) {
            return;
        }

        World world = player.getWorld();
        Location center = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        center.setYaw(0);
        center.setPitch(0);

        UUID groupId = UUID.randomUUID();
        Group group = new Group(center);
        group.ownerId = player.getUniqueId();
        List<Slot> slots = DeathPile.layout(stacks.size());
        for (int index = 0; index < stacks.size(); index++) {
            spawnMember(world, center, stacks.get(index), slots.get(index), groupId, group);
        }

        spawnHead(world, center, player, groupId, group);
        group.labelId = spawnLabel(world, center, player, groupId);

        groups.put(groupId, group);
        // Read now, before vanilla empties the inventory these stacks came from
        Audit.event(plugin, "pile-created").with("owner", player).at(center).with("items", stacks).record();
        player.sendMessage(Component.text(
                "Your items are floating at (" + center.getBlockX() + ", " + center.getBlockY() + ", "
                        + center.getBlockZ() + ")",
                NamedTextColor.RED));
    }

    private void spawnMember(World world, Location center, ItemStack stack, Slot slot, UUID groupId, Group group) {
        Location itemLocation = center.clone().add(DeathPile.position(slot, 0));

        byte[] orbit = encodeOrbit(center, slot);
        if (orbit == null) {
            return;
        }

        float scale = stack.getType().isBlock() ? BLOCK_SCALE : ITEM_SCALE;
        ItemDisplay display = world.spawn(itemLocation, ItemDisplay.class, (ItemDisplay entity) -> {
            entity.setItemStack(stack);
            entity.setTransformationMatrix(new Matrix4f().scale(scale));
            entity.setBillboard(Billboard.VERTICAL);
            entity.setTeleportDuration(TELEPORT_DURATION);

            PersistentDataContainer container = entity.getPersistentDataContainer();
            container.set(plugin.getPileGroupKey(), PersistentDataType.STRING, groupId.toString());
            container.set(plugin.getPileOrbitKey(), PersistentDataType.BYTE_ARRAY, orbit);
            container.set(plugin.getPileOwnerKey(), PersistentDataType.STRING, group.ownerId.toString());
        });

        Interaction box = world.spawn(hitboxLocation(itemLocation), Interaction.class, (Interaction entity) -> {
            entity.setInteractionWidth(HITBOX_WIDTH);
            entity.setInteractionHeight(HITBOX_HEIGHT);
            entity.setResponsive(true);

            PersistentDataContainer container = entity.getPersistentDataContainer();
            container.set(plugin.getPileMemberKey(), PersistentDataType.BOOLEAN, true);
            container.set(plugin.getPileGroupKey(), PersistentDataType.STRING, groupId.toString());
            container.set(plugin.getPileDisplayIdKey(), PersistentDataType.STRING, display.getUniqueId().toString());
        });

        display.getPersistentDataContainer().set(plugin.getPileInteractionIdKey(), PersistentDataType.STRING,
                box.getUniqueId().toString());

        group.members.put(display.getUniqueId(), new Member(box.getUniqueId(), slot));
    }

    /**
     * The owner's head, spinning in the middle of the rings, with a hitbox of its own:
     * clicking it hands over a random stack, and sneak-clicking takes the lot.
     */
    private void spawnHead(World world, Location center, Player owner, UUID groupId, Group group) {
        byte[] orbit = encodeOrbit(center, DeathPile.HEAD_SLOT);
        if (orbit == null) {
            return;
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            // The online profile carries the skin, so the head renders without a lookup
            meta.setPlayerProfile(owner.getPlayerProfile());
            head.setItemMeta(meta);
        }

        Location location = center.clone().add(DeathPile.position(DeathPile.HEAD_SLOT, 0));
        ItemDisplay display = world.spawn(location, ItemDisplay.class, (ItemDisplay entity) -> {
            entity.setItemStack(head);
            entity.setTransformation(headTransformation(0));
            tag(entity, groupId, owner, orbit, ROLE_HEAD);
        });

        // The head spins in place, so its hitbox never has to move
        Location hitbox = location.clone().subtract(0, HEAD_HITBOX_SIZE / 2.0, 0);
        Interaction box = world.spawn(hitbox, Interaction.class, (Interaction entity) -> {
            entity.setInteractionWidth(HEAD_HITBOX_SIZE);
            entity.setInteractionHeight(HEAD_HITBOX_SIZE);
            entity.setResponsive(true);

            PersistentDataContainer container = entity.getPersistentDataContainer();
            container.set(plugin.getPileMemberKey(), PersistentDataType.BOOLEAN, true);
            container.set(plugin.getPileGroupKey(), PersistentDataType.STRING, groupId.toString());
            container.set(plugin.getPileRoleKey(), PersistentDataType.STRING, ROLE_HEAD);
        });
        display.getPersistentDataContainer().set(plugin.getPileInteractionIdKey(), PersistentDataType.STRING,
                box.getUniqueId().toString());

        group.headId = display.getUniqueId();
        group.headHitboxId = box.getUniqueId();
    }

    /** Whether a pile hitbox is the head's rather than a stack's. */
    public boolean isHeadHitbox(Interaction box) {
        return ROLE_HEAD.equals(box.getPersistentDataContainer().get(plugin.getPileRoleKey(), PersistentDataType.STRING));
    }

    /**
     * The owner's name above the pile, hidden until someone looks at the pile from within
     * reach. It's their display name, so it carries the colors, prefix and suffix the Chat
     * plugin gives them, or just the plain name without it.
     */
    private UUID spawnLabel(World world, Location center, Player owner, UUID groupId) {
        byte[] orbit = encodeOrbit(center, DeathPile.LABEL_SLOT);
        if (orbit == null) {
            return null;
        }

        Location location = center.clone().add(DeathPile.position(DeathPile.LABEL_SLOT, 0));
        TextDisplay display = world.spawn(location, TextDisplay.class, (TextDisplay entity) -> {
            entity.text(owner.displayName());
            entity.setBillboard(Billboard.CENTER);
            entity.setVisibleByDefault(false);
            tag(entity, groupId, owner, orbit, ROLE_LABEL);
        });
        return display.getUniqueId();
    }

    private void tag(Entity entity, UUID groupId, Player owner, byte[] orbit, String role) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(plugin.getPileGroupKey(), PersistentDataType.STRING, groupId.toString());
        container.set(plugin.getPileOwnerKey(), PersistentDataType.STRING, owner.getUniqueId().toString());
        container.set(plugin.getPileOrbitKey(), PersistentDataType.BYTE_ARRAY, orbit);
        container.set(plugin.getPileRoleKey(), PersistentDataType.STRING, role);
    }

    private byte[] encodeOrbit(Location center, Slot slot) {
        try {
            return DeathPile.writeOrbit(center.getX(), center.getY(), center.getZ(), slot);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to encode death pile orbit", ex);
            return null;
        }
    }

    /** Tracks a pile's stack, head or name tag if it isn't already tracked. Safe to call repeatedly. */
    public void adopt(Display display) {
        PersistentDataContainer container = display.getPersistentDataContainer();

        String groupValue = container.get(plugin.getPileGroupKey(), PersistentDataType.STRING);
        byte[] orbitBytes = container.get(plugin.getPileOrbitKey(), PersistentDataType.BYTE_ARRAY);
        if (groupValue == null || orbitBytes == null) {
            return;
        }

        String role = container.get(plugin.getPileRoleKey(), PersistentDataType.STRING);
        String interactionValue = container.get(plugin.getPileInteractionIdKey(), PersistentDataType.STRING);
        if (role == null && interactionValue == null) {
            return;
        }

        UUID groupId = UUID.fromString(groupValue);
        Group existing = groups.get(groupId);
        if (existing != null && (existing.members.containsKey(display.getUniqueId())
                || display.getUniqueId().equals(existing.headId) || display.getUniqueId().equals(existing.labelId))) {
            return;
        }

        Orbit orbit;
        try {
            orbit = DeathPile.readOrbit(orbitBytes);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to decode death pile orbit", ex);
            return;
        }

        Location center = new Location(display.getWorld(), orbit.centerX(), orbit.centerY(), orbit.centerZ());
        Group group = groups.computeIfAbsent(groupId, key -> new Group(center));
        if (group.ownerId == null) {
            group.ownerId = readOwner(display);
        }
        if (ROLE_HEAD.equals(role)) {
            group.headId = display.getUniqueId();
            // Piles from before the head had a hitbox have none to point at
            group.headHitboxId = interactionValue == null ? null : UUID.fromString(interactionValue);
        } else if (ROLE_LABEL.equals(role)) {
            // Whether this persists across a restart isn't guaranteed, so reapply it
            display.setVisibleByDefault(false);
            group.labelId = display.getUniqueId();
        } else {
            group.members.put(display.getUniqueId(), new Member(UUID.fromString(interactionValue), orbit.slot()));
        }
    }

    /** The owner a display was tagged with or, for piles from before that tag, the face on their head. */
    private UUID readOwner(Display display) {
        String owner = display.getPersistentDataContainer().get(plugin.getPileOwnerKey(), PersistentDataType.STRING);
        if (owner != null) {
            return UUID.fromString(owner);
        }

        if (display instanceof ItemDisplay item && item.getItemStack().getItemMeta() instanceof SkullMeta skull) {
            PlayerProfile profile = skull.getPlayerProfile();
            return profile == null ? null : profile.getId();
        }
        return null;
    }

    /** Who died to leave this pile, or null when that isn't known (piles older than 1.1.0). */
    public OfflinePlayer ownerOf(UUID groupId) {
        Group group = groupId == null ? null : groups.get(groupId);
        return group == null || group.ownerId == null ? null : Bukkit.getOfflinePlayer(group.ownerId);
    }

    /** Stops tracking a display (e.g. its chunk unloaded); the entities are left alone. */
    public void forget(UUID displayId) {
        for (Group group : groups.values()) {
            group.members.remove(displayId);
            if (displayId.equals(group.headId)) {
                group.headId = null;
                group.headHitboxId = null;
            }
            if (displayId.equals(group.labelId)) {
                group.labelId = null;
            }
        }
        groups.entrySet().removeIf(entry -> entry.getValue().members.isEmpty());
    }

    /** Left-click pickup: hand the clicked stack back and drop it from the pile. */
    public void grabOne(Player player, Interaction box) {
        PersistentDataContainer container = box.getPersistentDataContainer();
        String displayValue = container.get(plugin.getPileDisplayIdKey(), PersistentDataType.STRING);
        String groupValue = container.get(plugin.getPileGroupKey(), PersistentDataType.STRING);

        UUID displayId = displayValue == null ? null : UUID.fromString(displayValue);
        UUID groupId = groupValue == null ? null : UUID.fromString(groupValue);
        Location soundAt = box.getLocation();

        if (displayId != null && Bukkit.getEntity(displayId) instanceof ItemDisplay display) {
            soundAt = display.getLocation();
            ItemStack stack = display.getItemStack();
            // Handing it over can shrink the stack, so the record keeps a copy of it whole
            Audit audit = Audit.event(plugin, "pile-item-taken").with("owner", ownerOf(groupId)).with("by", player)
                    .at(soundAt).with("item", stack.clone());
            audit.with("dropped", giveStack(player, stack)).record();
            display.remove();
        }
        box.remove();
        player.getWorld().playSound(soundAt, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);

        if (groupId != null && displayId != null) {
            removeFromGroup(groupId, displayId, player);
        }
    }

    /** Left-click on the head: hand back one stack picked at random. */
    public void grabRandom(Player player, UUID groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            return;
        }

        List<Member> members = new ArrayList<>(group.members.values());
        Collections.shuffle(members, ThreadLocalRandom.current());
        for (Member member : members) {
            // Skip a stack whose hitbox isn't loaded rather than giving up
            if (Bukkit.getEntity(member.interactionId()) instanceof Interaction box) {
                grabOne(player, box);
                return;
            }
        }
    }

    /** Shift-left-click pickup: sweep the whole pile into the player's inventory. */
    public void grabAll(Player player, UUID groupId) {
        OfflinePlayer owner = ownerOf(groupId);
        Group group = groups.remove(groupId);
        if (group == null) {
            return;
        }

        List<ItemStack> taken = new ArrayList<>();
        List<ItemStack> dropped = new ArrayList<>();
        for (Map.Entry<UUID, Member> entry : group.members.entrySet()) {
            if (Bukkit.getEntity(entry.getKey()) instanceof ItemDisplay display) {
                ItemStack stack = display.getItemStack();
                taken.add(stack.clone());
                dropped.addAll(giveStack(player, stack));
                display.remove();
            }
            if (Bukkit.getEntity(entry.getValue().interactionId()) instanceof Interaction box) {
                box.remove();
            }
        }
        removeHeadAndLabel(group);

        Audit.event(plugin, "pile-taken").with("owner", owner).with("by", player).at(group.center)
                .with("items", taken).with("dropped", dropped).record();
        player.getWorld().playSound(group.center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void removeFromGroup(UUID groupId, UUID displayId, Player player) {
        Group group = groups.get(groupId);
        if (group == null) {
            return;
        }

        group.members.remove(displayId);
        if (group.members.isEmpty()) {
            groups.remove(groupId);
            removeHeadAndLabel(group);
            player.getWorld().playSound(group.center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    private static void removeHeadAndLabel(Group group) {
        for (UUID id : new UUID[] { group.headId, group.headHitboxId, group.labelId }) {
            Entity entity = id == null ? null : Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    /** Hands the stack over, dropping whatever doesn't fit at the player's feet. Returns what was dropped. */
    private List<ItemStack> giveStack(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return List.of();
        }

        List<ItemStack> dropped = new ArrayList<>();
        for (ItemStack overflow : player.getInventory().addItem(stack).values()) {
            dropped.add(overflow.clone());
            // Marked as theirs, so the audit log notices if it then despawns or burns
            player.getWorld().dropItemNaturally(player.getLocation(), overflow,
                    item -> item.setThrower(player.getUniqueId()));
        }
        return dropped;
    }

    private void tick() {
        phase += PHASE_STEP;
        if (phase > 2.0 * Math.PI) {
            phase -= 2.0 * Math.PI;
        }

        for (Group group : groups.values()) {
            for (Map.Entry<UUID, Member> entry : group.members.entrySet()) {
                if (!(Bukkit.getEntity(entry.getKey()) instanceof ItemDisplay display)) {
                    continue;
                }

                Vector offset = DeathPile.position(entry.getValue().slot(), phase);
                Location itemLocation = group.center.clone().add(offset);
                display.teleport(itemLocation);

                if (Bukkit.getEntity(entry.getValue().interactionId()) instanceof Interaction box) {
                    box.teleport(hitboxLocation(itemLocation));
                }
            }

            if (group.headId != null && Bukkit.getEntity(group.headId) instanceof ItemDisplay head) {
                // Spun by interpolated transformation rather than teleported yaw: rotations
                // interpolate the short way round, so the wrap at a full turn doesn't stutter
                head.setInterpolationDelay(0);
                head.setInterpolationDuration(TELEPORT_DURATION);
                head.setTransformation(headTransformation(phase));
            }

            if (group.labelId != null && Bukkit.getEntity(group.labelId) instanceof TextDisplay label) {
                updateLabel(group, label);
            }

            sparkle(group);
        }
    }

    /** Now and then, a white sparkle drifting down from somewhere inside the pile. */
    private static void sparkle(Group group) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= SPARKLE_CHANCE) {
            return;
        }

        Location at = group.center.clone()
                .add(DeathPile.sparkleOffset(random.nextDouble(), random.nextDouble(), random.nextDouble()));
        // A count of 0 turns the offset into the particle's direction, so it falls straight down
        group.center.getWorld().spawnParticle(Particle.END_ROD, at, 0, 0, -1, 0, SPARKLE_FALL_SPEED);
    }

    /** Turned with the ring (the orbit runs the opposite way to a positive Y rotation). */
    private static Transformation headTransformation(double phase) {
        return new Transformation(new Vector3f(0, HEAD_LIFT, 0), new Quaternionf().rotationY((float) -phase),
                new Vector3f(HEAD_SCALE), new Quaternionf());
    }

    /** Shows the tag to each player looking at the pile from within reach, and hides it from the rest. */
    private void updateLabel(Group group, TextDisplay label) {
        for (Player player : group.center.getWorld().getPlayers()) {
            boolean targeting = isTargeting(player, group);
            if (targeting && !player.canSee(label)) {
                player.showEntity(plugin, label);
            } else if (!targeting && player.canSee(label)) {
                player.hideEntity(plugin, label);
            }
        }
    }

    /**
     * Whether the player's line of sight hits one of the pile's stacks within their
     * entity reach: the same reach that decides whether a click picks a stack up.
     * Blocks in the way count, so a tag never shows through a wall.
     */
    private static boolean isTargeting(Player player, Group group) {
        if (player.getLocation().distanceSquared(group.center) > LABEL_CHECK_RADIUS_SQUARED) {
            return false;
        }

        AttributeInstance reach = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        double range = reach == null ? DEFAULT_REACH : reach.getValue();
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTrace(eye, eye.getDirection(), range, FluidCollisionMode.NEVER,
                true, 0.0, group::isHitbox);
        return hit != null && hit.getHitEntity() != null;
    }

    private static Location hitboxLocation(Location itemLocation) {
        return itemLocation.clone().subtract(0, HITBOX_HEIGHT / 2.0, 0);
    }
}
