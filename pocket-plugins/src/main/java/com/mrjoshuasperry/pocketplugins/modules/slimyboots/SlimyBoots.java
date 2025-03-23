package com.mrjoshuasperry.pocketplugins.modules.slimyboots;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

/** @author TimPCunningham */
public class SlimyBoots extends Module {
    private final NamespacedKey bootsKey;
    private static final PersistentDataType<Byte, Byte> BYTE = PersistentDataType.BYTE;

    public SlimyBoots() {
        super("SlimyBoots");
        bootsKey = this.createKey("SlimyBoots");
        initRecipes();
    }

    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Vector dir = player.getLocation().getDirection();

        if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            ItemStack boots = player.getInventory().getBoots();
            if (player.isSneaking() || boots == null
                    || !boots.getItemMeta().getPersistentDataContainer().has(this.bootsKey, BYTE)) {
                return;
            }

            float fallDist = player.getFallDistance();
            float fallDistModified = (-.0011f * fallDist * fallDist) + (0.43529f * fallDist);
            double velY = Math.sqrt(0.32 * fallDistModified);

            player.setVelocity(new Vector(dir.getX(), velY, dir.getZ()));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_FALL, 2, 1);
            event.setCancelled(true);
        }
    }

    private void initRecipes() {
        ItemStack result = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta itemMeta = (LeatherArmorMeta) result.getItemMeta();

        itemMeta.setColor(Color.fromRGB(100, 255, 100));
        itemMeta.displayName(Component.text(ChatColor.GREEN + "Slimy Boots"));
        itemMeta.lore(List.of(Component.text(ChatColor.GRAY + "A bit squishy but it should protect from falls")));

        itemMeta.getPersistentDataContainer().set(bootsKey, BYTE, (byte) 1);
        result.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(this.createKey("slimy-boots"), result);
        recipe.setIngredient('B', Material.LEATHER_BOOTS);
        recipe.setIngredient('S', Material.SLIME_BLOCK);
        recipe.shape("SSS", "SBS", "SSS");
        this.registerCraftingRecipe(recipe);
    }
}
