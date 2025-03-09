package com.mrjoshuasperry.croptweaks;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.mcutils.types.CropTypes;
import com.mrjoshuasperry.mcutils.types.ToolTypes;

public class Main extends JavaPlugin implements Listener {
    private final Random random = new Random();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        PlayerInventory inventory = event.getPlayer().getInventory();
        EquipmentSlot hand = event.getHand();
        ItemStack tool;

        if (hand == EquipmentSlot.HAND) {
            tool = inventory.getItemInMainHand();
        } else {
            tool = inventory.getItemInOffHand();
        }

        if (!ToolTypes.getHoeTypes().contains(tool.getType())) {
            return;
        }

        Block block = event.getClickedBlock();
        Material type = block.getType();
        BlockData blockData = block.getBlockData();

        if (!CropTypes.getHarvestableTypes().contains(type)) {
            return;
        }

        Ageable data = (Ageable) blockData;
        if (data.getAge() != data.getMaximumAge()) {
            return;
        }

        data.setAge(0);
        block.setBlockData(data);

        Location location = block.getLocation();
        World world = location.getWorld();
        int amount = this.random.nextInt(2) + 1;

        Damageable meta = (Damageable) tool.getItemMeta();
        meta.setDamage(meta.getDamage() + 1);
        tool.setItemMeta(meta);

        switch (type) {
            case NETHER_WART:
                world.dropItemNaturally(location, new ItemStack(Material.NETHER_WART, amount));
                world.playSound(location, Sound.BLOCK_NETHER_WART_BREAK, 1, 1);
                break;
            case WHEAT:
                world.dropItemNaturally(location, new ItemStack(Material.WHEAT, amount));
                world.playSound(location, Sound.BLOCK_CROP_BREAK, 1, 1);
                break;
            case CARROTS:
                world.dropItemNaturally(location, new ItemStack(Material.CARROT, amount));
                world.playSound(location, Sound.BLOCK_CROP_BREAK, 1, 1);
                break;
            case POTATOES:
                world.dropItemNaturally(location, new ItemStack(Material.POTATO, amount));
                world.playSound(location, Sound.BLOCK_CROP_BREAK, 1, 1);
                break;
            case BEETROOTS:
                world.dropItemNaturally(location, new ItemStack(Material.BEETROOT, amount));
                world.playSound(location, Sound.BLOCK_CROP_BREAK, 1, 1);
                break;
            default:
                return;
        }
    }

}
