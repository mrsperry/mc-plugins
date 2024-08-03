package com.mrjoshuasperry.croptweaks;

import com.mrjoshuasperry.mcutils.types.CropTypes;

import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class Main extends JavaPlugin implements Listener {
    private final Random random = new Random();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    private void dropItem(Location location, Material material, int amount) {
        World world = location.getWorld();
        if (world != null) {
            world.dropItemNaturally(location, new ItemStack(material, amount));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getHand() == EquipmentSlot.HAND) {
                Block block = event.getClickedBlock();
                if (block == null) {
                    return;
                }

                Material type = block.getType();
                BlockData blockData = block.getBlockData();
                if (CropTypes.getHarvestableTypes().contains(type) && block.getBlockData() instanceof Ageable) {
                    Ageable data = (Ageable) blockData;
                    if (data.getAge() != data.getMaximumAge()) {
                        return;
                    }
                    data.setAge(0);
                    block.setBlockData(data);
                    event.setCancelled(true);

                    Location location = block.getLocation();
                    int amount = this.random.nextInt(2) + 1;
                    switch (type) {
                        case NETHER_WART:
                            this.dropItem(location, Material.NETHER_WART, amount);
                            break;
                        case WHEAT:
                            this.dropItem(location, Material.WHEAT, 1);
                            break;
                        case CARROTS:
                            this.dropItem(location, Material.CARROT, amount);
                            break;
                        case POTATOES:
                            this.dropItem(location, Material.POTATO, amount);
                            break;
                        case BEETROOTS:
                            this.dropItem(location, Material.BEETROOT, amount);
                            break;
                    }
                }
            }
        }
    }
}
