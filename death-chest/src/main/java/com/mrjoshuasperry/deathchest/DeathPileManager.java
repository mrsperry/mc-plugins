package com.mrjoshuasperry.deathchest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;

import com.mrjoshuasperry.deathchest.DeathPile.Orbit;
import com.mrjoshuasperry.deathchest.DeathPile.Slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Owns the live death piles: spawns them, keeps an in-memory registry keyed by group
 * UUID, orbits their items on a repeating task, and hands stacks back on pickup. The
 * registry is rebuilt from entity PDC on enable and as chunks load, so piles survive
 * restarts.
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

    private final Main plugin;
    private final Map<UUID, Group> groups = new HashMap<>();
    private double phase;

    /** A tracked stack: its hitbox and its fixed slot in the ring. */
    private record Member(UUID interactionId, Slot slot) {
    }

    /** All stacks of one death, sharing a center. */
    private static final class Group {
        private final Location center;
        private final Map<UUID, Member> members = new HashMap<>();

        private Group(Location center) {
            this.center = center;
        }
    }

    public DeathPileManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Adopts already-loaded piles and starts the orbit task. Call once from onEnable. */
    public void start() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
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
        List<Slot> slots = DeathPile.layout(stacks.size());
        for (int index = 0; index < stacks.size(); index++) {
            spawnMember(world, center, stacks.get(index), slots.get(index), groupId, group);
        }

        groups.put(groupId, group);
        player.sendMessage(Component.text(
                "Your items are floating at (" + center.getBlockX() + ", " + center.getBlockY() + ", "
                        + center.getBlockZ() + ")",
                NamedTextColor.RED));
    }

    private void spawnMember(World world, Location center, ItemStack stack, Slot slot, UUID groupId, Group group) {
        Location itemLocation = center.clone().add(DeathPile.position(slot, 0));

        byte[] orbit;
        try {
            orbit = DeathPile.writeOrbit(center.getX(), center.getY(), center.getZ(), slot);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to encode death pile orbit", ex);
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

    /** Tracks a display's pile if it isn't already tracked. Safe to call repeatedly. */
    public void adopt(ItemDisplay display) {
        PersistentDataContainer container = display.getPersistentDataContainer();

        String groupValue = container.get(plugin.getPileGroupKey(), PersistentDataType.STRING);
        byte[] orbitBytes = container.get(plugin.getPileOrbitKey(), PersistentDataType.BYTE_ARRAY);
        String interactionValue = container.get(plugin.getPileInteractionIdKey(), PersistentDataType.STRING);
        if (groupValue == null || orbitBytes == null || interactionValue == null) {
            return;
        }

        UUID groupId = UUID.fromString(groupValue);
        Group existing = groups.get(groupId);
        if (existing != null && existing.members.containsKey(display.getUniqueId())) {
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
        group.members.put(display.getUniqueId(), new Member(UUID.fromString(interactionValue), orbit.slot()));
    }

    /** Stops tracking a display (e.g. its chunk unloaded); the entities are left alone. */
    public void forget(UUID displayId) {
        groups.values().forEach(group -> group.members.remove(displayId));
        groups.entrySet().removeIf(entry -> entry.getValue().members.isEmpty());
    }

    /** Left-click pickup: hand the clicked stack back and drop it from the pile. */
    public void grabOne(Player player, Interaction box) {
        PersistentDataContainer container = box.getPersistentDataContainer();
        String displayValue = container.get(plugin.getPileDisplayIdKey(), PersistentDataType.STRING);
        String groupValue = container.get(plugin.getPileGroupKey(), PersistentDataType.STRING);

        UUID displayId = displayValue == null ? null : UUID.fromString(displayValue);
        Location soundAt = box.getLocation();

        if (displayId != null && Bukkit.getEntity(displayId) instanceof ItemDisplay display) {
            soundAt = display.getLocation();
            giveStack(player, display.getItemStack());
            display.remove();
        }
        box.remove();
        player.getWorld().playSound(soundAt, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);

        if (groupValue != null && displayId != null) {
            removeFromGroup(UUID.fromString(groupValue), displayId, player);
        }
    }

    /** Shift-left-click pickup: sweep the whole pile into the player's inventory. */
    public void grabAll(Player player, UUID groupId) {
        Group group = groups.remove(groupId);
        if (group == null) {
            return;
        }

        for (Map.Entry<UUID, Member> entry : group.members.entrySet()) {
            if (Bukkit.getEntity(entry.getKey()) instanceof ItemDisplay display) {
                giveStack(player, display.getItemStack());
                display.remove();
            }
            if (Bukkit.getEntity(entry.getValue().interactionId()) instanceof Interaction box) {
                box.remove();
            }
        }

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
            player.getWorld().playSound(group.center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    private void giveStack(Player player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }

        for (ItemStack overflow : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }
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
        }
    }

    private static Location hitboxLocation(Location itemLocation) {
        return itemLocation.clone().subtract(0, HITBOX_HEIGHT / 2.0, 0);
    }
}
