package com.mrjoshuasperry.pocketplugins.modules.slimyboots;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** @author TimPCunningham */
public class SlimyBoots extends Module {
    private final NamespacedKey bootsKey;
    private static final PersistentDataType<Byte, Byte> BYTE = PersistentDataType.BYTE;

    public SlimyBoots(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super(readableConfig, writableConfig);
        this.bootsKey = this.createKey("slimy-boots");
        this.initRecipes();
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Vector dir = player.getLocation().getDirection();

        if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            ItemStack boots = player.getInventory().getBoots();
            // An empty feet slot comes back as an AIR stack rather than null, so the
            // null check alone lets it through; AIR has no meta to read a PDC from
            if (player.isSneaking() || boots == null || boots.isEmpty()) {
                return;
            }

            ItemMeta itemMeta = boots.getItemMeta();
            if (itemMeta == null || !itemMeta.getPersistentDataContainer().has(this.bootsKey, BYTE)) {
                return;
            }

            double velY = launchVelocity(player.getFallDistance());

            player.setVelocity(new Vector(dir.getX(), velY, dir.getZ()));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_FALL, 2, 1);
            event.setCancelled(true);
        }
    }

    // The launch parabola peaks at this fall distance (its vertex, b / 2a for the
    // coefficients below); beyond it the curve falls back toward zero and then goes
    // negative. Capping the fall distance here holds the bounce at its maximum for big
    // falls instead of shrinking it — and past ~396 blocks would otherwise yield NaN.
    private static final float PEAK_FALL_DISTANCE = 0.43529f / (2f * 0.0011f);

    /**
     * Upward launch velocity for a slime-boots fall, from the tuned parabola. The fall
     * distance is capped at the parabola's peak, so falls beyond it hold the maximum
     * bounce rather than curving back down. Package-private and static so it is unit-
     * testable without a live player.
     */
    static double launchVelocity(float fallDistance) {
        float effective = Math.min(fallDistance, PEAK_FALL_DISTANCE);
        float modified = (-.0011f * effective * effective) + (0.43529f * effective);
        return Math.sqrt(0.32 * modified);
    }

    private void initRecipes() {
        ItemStack result = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta itemMeta = (LeatherArmorMeta) result.getItemMeta();

        itemMeta.setColor(Color.fromRGB(100, 255, 100));
        itemMeta.displayName(Component.text("Slimy Boots", NamedTextColor.GREEN));
        itemMeta.lore(List.of(
                Component.text("A bit squishy but it should protect from falls", NamedTextColor.GRAY)));

        itemMeta.getPersistentDataContainer().set(bootsKey, BYTE, (byte) 1);
        result.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(this.createKey("slimy-boots"), result);
        recipe.shape("SSS", "SBS", "SSS");
        recipe.setIngredient('B', Material.LEATHER_BOOTS);
        recipe.setIngredient('S', Material.SLIME_BLOCK);
        this.registerCraftingRecipe(recipe);
    }
}
