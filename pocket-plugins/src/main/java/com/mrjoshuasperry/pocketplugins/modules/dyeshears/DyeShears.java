package com.mrjoshuasperry.pocketplugins.modules.dyeshears;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.mcutils.ItemMetaHandler;
import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.pocketplugins.utils.CraftingUtil;
import com.mrjoshuasperry.pocketplugins.utils.Module;
import com.mrjoshuasperry.pocketplugins.utils.StringHelper;

import net.md_5.bungee.api.ChatColor;

/** @author TimPCunningham */
public class DyeShears extends Module {
    private int chance;
    private final NamespacedKey shearKey;
    private static final PersistentDataType<Byte, Byte> BYTE = PersistentDataType.BYTE;

    public DyeShears() {
        super("ImprovedShears");
        this.shearKey = this.createKey("Improved_Shears");
    }

    @Override
    public void initialize(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
        super.initialize(readableConfig, writableConfig);
        this.chance = readableConfig.getInt("dye-chance", 25);
        this.initRecipes();
    }

    @EventHandler
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Sheep)) {
            return;
        }

        Player player = event.getPlayer();
        Sheep sheep = (Sheep) entity;
        ItemStack itemUsed = this.checkAndGet(player);
        Double chanceRoll = this.getPlugin().getRandom().nextDouble();
        
        if (itemUsed.getType().equals(Material.SHEARS) &&
                ItemMetaHandler.hasKey(itemUsed, this.shearKey, BYTE) &&
                chanceRoll <= (chance / 100.0)) {

            DyeColor color = sheep.getColor();
            if (color == null) {
                return;
            }

            event.setCancelled(true);
            ItemStack drop = new ItemStack(Material.valueOf(color.name() + "_DYE"));
            drop.setAmount(this.getPlugin().getRandom().nextInt(1, 4));
            sheep.setSheared(true);
            sheep.getWorld().dropItemNaturally(sheep.getLocation().clone().add(0, 0.5, 0), drop);
            player.getWorld().playSound(sheep.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);
        }
    }

    private ItemStack checkAndGet(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (main.getType().equals(Material.SHEARS)) {
            return main;
        } else if (off.getType().equals(Material.SHEARS)) {
            return off;
        }
        return main;
    }

    private void initRecipes() {
        Map<Material, Integer> ingredients = new EnumMap<>(Material.class);
        ingredients.put(Material.SHEARS, 2);
        ingredients.put(Material.DIAMOND, 1);

        ItemStack result = new ItemBuilder(Material.SHEARS)
                .setName(StringHelper.rainbowify("Improved Shears"))
                .addLore(ChatColor.GOLD + "Has a " + chance + "% chance to drop dye instead of wool!")
                .build();

        ItemMetaHandler.set(result, this.shearKey, PersistentDataType.BYTE, (byte) 1);
        CraftingUtil.addShapelessCrafting("Improved_Shears", ingredients, result);
    }
}
