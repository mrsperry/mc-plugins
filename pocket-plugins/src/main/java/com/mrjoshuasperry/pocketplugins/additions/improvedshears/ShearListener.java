package com.mrjoshuasperry.pocketplugins.additions.improvedshears;

import java.util.EnumMap;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.mrjoshuasperry.mcutils.ItemMetaHandler;
import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.CraftingUtil;
import com.mrjoshuasperry.pocketplugins.utils.Module;
import com.mrjoshuasperry.pocketplugins.utils.StringHelper;

import net.md_5.bungee.api.ChatColor;

public class ShearListener extends Module {
    private int chance;
    private final NamespacedKey shearKey;
    private static final PersistentDataType<Byte, Byte> BYTE = PersistentDataType.BYTE;

    public ShearListener() {
        super("ImprovedShears");
        this.shearKey = new NamespacedKey(PocketPlugins.getInstance(), "Improved_Shears");
    }

    @Override
    public void init(YamlConfiguration config) {
        super.init(config);
        this.chance = config.getInt("dye-chance", 25);
        this.initRecipes();
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {
        if (event.getEntity() instanceof Sheep) {
            Player player = event.getPlayer();
            Sheep sheep = (Sheep) event.getEntity();
            ItemStack itemUsed = checkAndGet(player);
            Double chanceRoll = PocketPlugins.getRandom().nextDouble();

            if (itemUsed.getType().equals(Material.SHEARS) &&
                    ItemMetaHandler.hasKey(itemUsed, this.shearKey, BYTE) &&
                    chanceRoll <= (chance / 100.0)) {
                event.setCancelled(true);
                DyeColor color = sheep.getColor();
                if (color != null) {
                    ItemStack drop = new ItemStack(Material.valueOf(color.name() + "_DYE"));
                    int amount = PocketPlugins.getRandom().nextInt(1, 4);
                    drop.setAmount(amount);
                    sheep.setSheared(true);
                    sheep.getWorld().dropItemNaturally(sheep.getLocation().clone().add(0, 0.5, 0), drop);
                    player.getWorld().playSound(sheep.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1, 1);
                }
            }
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
