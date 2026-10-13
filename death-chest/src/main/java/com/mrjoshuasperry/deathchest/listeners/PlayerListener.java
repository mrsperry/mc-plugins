package com.mrjoshuasperry.deathchest.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.deathchest.Main;
import com.mrjoshuasperry.mcutils.audit.Audit;

public class PlayerListener implements Listener {
    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * The pile holds exactly what vanilla would have dropped, read after other plugins
     * have added to the drops (MobHeads adds a head there). Vanilla's drops already
     * leave out Curse of Vanishing items, so those vanish here as they would anywhere.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory()) {
            return;
        }

        Player player = event.getPlayer();
        if (!leavesAPile(player.getGameMode())) {
            return;
        }

        List<ItemStack> vanished = vanishing(player.getInventory().getContents());
        if (!vanished.isEmpty()) {
            Audit.event(this.plugin, "items-vanished").with("player", player).at(player.getLocation())
                    .with("items", vanished).record();
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        this.plugin.getPileManager().spawn(player, drops);
    }

    /** The items Curse of Vanishing destroys on death. */
    static List<ItemStack> vanishing(ItemStack[] contents) {
        List<ItemStack> vanishing = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                vanishing.add(item);
            }
        }
        return vanishing;
    }

    /** Survival and creative deaths leave a pile; adventure and spectator drop as vanilla. */
    static boolean leavesAPile(GameMode mode) {
        return mode == GameMode.SURVIVAL || mode == GameMode.CREATIVE;
    }
}
