package com.mrjoshuasperry.pocketplugins.modules.shulkerpeek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.mcutils.audit.Audit;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import net.kyori.adventure.text.Component;

/**
 * Right-click the air holding a shulker box to open it where you stand, without
 * placing it.
 *
 * <p>
 * The box stays locked in its slot while it's open: it can't be clicked, swapped,
 * dropped, placed or handed to anything. Each change made in the view is written
 * into the box on the next tick, so the box matches the view however the peek
 * ends, whether by closing, dying, leaving or the server stopping. If the box
 * leaves its slot anyway (an operator's {@code /clear}, say), the view closes
 * rather than keep handing out items, and its last contents go to the audit log.
 */
public class ShulkerPeek extends Module {
  /** The off hand's slot in a player's inventory. */
  static final int OFF_HAND_SLOT = 40;
  static final int SIZE = InventoryType.SHULKER_BOX.getDefaultSize();

  protected boolean requireSneaking;
  protected boolean sounds;

  /** Each peeking player's open box. */
  protected Map<UUID, Peek> peeks;
  /** Stamped on a box while it's open, holding its peek's id. */
  protected NamespacedKey lockKey;

  /** An open box: the view onto it, the slot it's locked in and the id stamped on it. */
  static final class Peek {
    final String id = UUID.randomUUID().toString();
    final int slot;
    final Inventory view;
    boolean syncQueued;

    Peek(int slot, Inventory view) {
      this.slot = slot;
      this.view = view;
    }
  }

  public ShulkerPeek(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.requireSneaking = readableConfig.getBoolean("require-sneaking", false);
    this.sounds = readableConfig.getBoolean("sounds", true);

    this.peeks = new HashMap<>();
    this.lockKey = this.createKey("peek");
  }

  @Override
  public void onDisable() {
    for (UUID id : List.copyOf(this.peeks.keySet())) {
      Player player = Bukkit.getPlayer(id);
      if (player != null) {
        this.finish(player, "server-stopped");
        player.closeInventory();
      }
    }

    super.onDisable();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInteract(PlayerInteractEvent event) {
    ItemStack item = event.getItem();
    if (this.isLocked(item)) {
      // Covers placing it, and putting it into a decorated pot or the like
      event.setCancelled(true);
      return;
    }

    // Clicks on the air arrive already cancelled, so only a denied item use counts
    Player player = event.getPlayer();
    if (event.getAction() != Action.RIGHT_CLICK_AIR || event.useItemInHand() == Event.Result.DENY
        || (this.requireSneaking && !player.isSneaking()) || this.peeks.containsKey(player.getUniqueId())
        || !canPeek(item)) {
      return;
    }

    int slot = event.getHand() == EquipmentSlot.OFF_HAND ? OFF_HAND_SLOT : player.getInventory().getHeldItemSlot();
    this.open(player, slot, player.getInventory().getItem(slot));
  }

  private void open(Player player, int slot, ItemStack box) {
    Component name = box.getData(DataComponentTypes.CUSTOM_NAME);
    Inventory view = Bukkit.createInventory(null, InventoryType.SHULKER_BOX,
        name != null ? name : Component.translatable("container.shulkerBox"));
    ItemStack[] contents = toSlots(contentsOf(box));
    view.setContents(contents);

    // Another plugin can stop the view opening, and then there's nothing to lock
    if (player.openInventory(view) == null) {
      return;
    }

    Peek peek = new Peek(slot, view);
    this.peeks.put(player.getUniqueId(), peek);
    player.getInventory().setItem(slot, withLock(box, this.lockKey, peek.id));

    this.playSound(player, Sound.BLOCK_SHULKER_BOX_OPEN);
    Audit.event(this.getPlugin(), "shulker-opened").with("player", player).at(player.getLocation())
        .with("box", box.getType()).with("slot", slot).with("items", contents).record();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onClick(InventoryClickEvent event) {
    Peek peek = this.peekIn(event.getWhoClicked(), event.getView());
    if (peek != null && movesBox(event.getClickedInventory() == event.getView().getBottomInventory(),
        event.getSlot(), event.getClick(), event.getHotbarButton(), peek.slot)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClickApplied(InventoryClickEvent event) {
    this.queueSync(event.getWhoClicked(), event.getView());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDragApplied(InventoryDragEvent event) {
    this.queueSync(event.getWhoClicked(), event.getView());
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player player && this.peekIn(player, event.getView()) != null) {
      this.finish(player, event.getReason());
    }
  }

  /**
   * The drops were copied from the inventory before this event, so the box among
   * them can be a tick behind the view. This runs before DeathChest piles them up.
   */
  @EventHandler(priority = EventPriority.LOWEST)
  public void onDeath(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    Peek peek = this.peeks.get(player.getUniqueId());
    if (peek == null) {
      return;
    }

    ItemStack[] contents = peek.view.getContents();
    event.getDrops().replaceAll(drop -> peek.id.equals(lockOf(drop, this.lockKey))
        ? withoutLock(withContents(drop, contents), this.lockKey)
        : drop);

    this.finish(player, InventoryCloseEvent.Reason.DEATH);
    // Vanilla closes the view after this event, but not if another plugin cancels the death
    player.closeInventory(InventoryCloseEvent.Reason.DEATH);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    // Leaving closes the view first, so this only catches a peek that close missed
    this.finish(event.getPlayer(), "quit");
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    // A box only stays locked past its peek if the server went down without closing it
    PlayerInventory inventory = event.getPlayer().getInventory();
    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack item = inventory.getItem(slot);
      if (this.isLocked(item)) {
        inventory.setItem(slot, withoutLock(item, this.lockKey));
      }
    }
  }

  // A vanilla client can't do any of these with a view open, but a modified one can

  @EventHandler(ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (this.isLocked(event.getItemDrop().getItemStack())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onSwapHands(PlayerSwapHandItemsEvent event) {
    if (this.isLocked(event.getMainHandItem()) || this.isLocked(event.getOffHandItem())) {
      event.setCancelled(true);
    }
  }

  /** Covers item frames, allays and anything else that takes an item from the hand. */
  @EventHandler(ignoreCancelled = true)
  public void onInteractEntity(PlayerInteractEntityEvent event) {
    if (this.isLocked(event.getPlayer().getInventory().getItem(event.getHand()))) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onManipulateArmorStand(PlayerArmorStandManipulateEvent event) {
    if (this.isLocked(event.getPlayerItem())) {
      event.setCancelled(true);
    }
  }

  /** The player's peek, if this is its view. */
  private Peek peekIn(HumanEntity player, InventoryView view) {
    Peek peek = this.peeks.get(player.getUniqueId());
    return peek != null && view.getTopInventory() == peek.view ? peek : null;
  }

  /** Writes the view into the box at the start of next tick, once the click has applied. */
  private void queueSync(HumanEntity who, InventoryView view) {
    Peek peek = this.peekIn(who, view);
    if (peek == null || peek.syncQueued || !(who instanceof Player player)) {
      return;
    }

    peek.syncQueued = true;
    Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
      peek.syncQueued = false;
      if (this.peeks.get(player.getUniqueId()) != peek || this.write(player, peek, true) != null) {
        return;
      }

      this.peeks.remove(player.getUniqueId());
      this.lost(player, peek, "box-left-slot");
      player.closeInventory();
    });
  }

  /** Ends the player's peek, writing the view into the box one last time and unlocking it. */
  private void finish(Player player, Object reason) {
    Peek peek = this.peeks.remove(player.getUniqueId());
    if (peek == null) {
      return;
    }

    ItemStack box = this.write(player, peek, false);
    if (box == null) {
      this.lost(player, peek, reason);
      return;
    }

    this.playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE);
    Audit.event(this.getPlugin(), "shulker-closed").with("player", player).at(player.getLocation())
        .with("box", box.getType()).with("slot", peek.slot).with("reason", reason)
        .with("items", peek.view.getContents()).record();
  }

  /** Writes the view into the box in its slot, returning the box, or null if it isn't there. */
  private ItemStack write(Player player, Peek peek, boolean keepLocked) {
    PlayerInventory inventory = player.getInventory();
    ItemStack box = inventory.getItem(peek.slot);
    if (!peek.id.equals(lockOf(box, this.lockKey))) {
      return null;
    }

    ItemStack updated = withContents(box, peek.view.getContents());
    if (!keepLocked) {
      updated = withoutLock(updated, this.lockKey);
    }
    inventory.setItem(peek.slot, updated);
    return updated;
  }

  /** The box left its slot while open, so the view's contents are recorded rather than written. */
  private void lost(Player player, Peek peek, Object reason) {
    this.getPlugin().getLogger().warning("The shulker box " + player.getName() + " was peeking into left slot "
        + peek.slot + " while open; its last contents are in the audit log as shulker-lost");
    Audit.event(this.getPlugin(), "shulker-lost").with("player", player).at(player.getLocation())
        .with("slot", peek.slot).with("reason", reason).with("items", peek.view.getContents()).record();
  }

  private void playSound(Player player, Sound sound) {
    if (this.sounds) {
      player.getWorld().playSound(player, sound, SoundCategory.BLOCKS, 0.5f, 1);
    }
  }

  private boolean isLocked(ItemStack item) {
    return lockOf(item, this.lockKey) != null;
  }

  /** One box, not locked against the player and with no loot still to roll, that a view can hold. */
  static boolean canPeek(ItemStack item) {
    if (item == null || item.getAmount() != 1 || !Tag.ITEMS_SHULKER_BOXES.isTagged(item.getType())
        || item.hasData(DataComponentTypes.CONTAINER_LOOT) || contentsOf(item).size() > SIZE) {
      return false;
    }

    return !(item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox box
        && box.isLocked());
  }

  /**
   * Whether a click would move the box being peeked: clicking it, or a number key
   * or the swap-hands key trading its slot for the one under the cursor.
   */
  static boolean movesBox(boolean inPlayerInventory, int slot, ClickType click, int hotbarButton, int boxSlot) {
    return (inPlayerInventory && slot == boxSlot)
        || (click == ClickType.NUMBER_KEY && hotbarButton == boxSlot)
        || (click == ClickType.SWAP_OFFHAND && boxSlot == OFF_HAND_SLOT);
  }

  static List<ItemStack> contentsOf(ItemStack box) {
    ItemContainerContents contents = box.getData(DataComponentTypes.CONTAINER);
    return contents == null ? List.of() : contents.contents();
  }

  /** A box's stored stacks laid out as its slots, empty ones null. */
  static ItemStack[] toSlots(List<ItemStack> contents) {
    ItemStack[] slots = new ItemStack[SIZE];
    for (int slot = 0; slot < Math.min(contents.size(), SIZE); slot++) {
      ItemStack item = contents.get(slot);
      slots[slot] = item == null || item.isEmpty() ? null : item;
    }
    return slots;
  }

  /** The reverse of {@link #toSlots}: the component wants an empty stack, not null, for a gap. */
  static List<ItemStack> fromSlots(ItemStack[] slots) {
    List<ItemStack> contents = new ArrayList<>(slots.length);
    for (ItemStack item : slots) {
      contents.add(item == null ? ItemStack.empty() : item);
    }
    return contents;
  }

  static ItemStack withContents(ItemStack box, ItemStack[] slots) {
    ItemStack updated = box.clone();
    updated.setData(DataComponentTypes.CONTAINER, ItemContainerContents.containerContents(fromSlots(slots)));
    return updated;
  }

  static String lockOf(ItemStack item, NamespacedKey key) {
    return item == null || item.isEmpty() ? null
        : item.getPersistentDataContainer().get(key, PersistentDataType.STRING);
  }

  static ItemStack withLock(ItemStack box, NamespacedKey key, String id) {
    ItemStack locked = box.clone();
    locked.editPersistentDataContainer(pdc -> pdc.set(key, PersistentDataType.STRING, id));
    return locked;
  }

  static ItemStack withoutLock(ItemStack box, NamespacedKey key) {
    ItemStack unlocked = box.clone();
    unlocked.editPersistentDataContainer(pdc -> pdc.remove(key));
    return unlocked;
  }
}
