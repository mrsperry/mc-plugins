package com.mrjoshuasperry.pocketplugins.additions.experimental;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.kyori.adventure.text.Component;

public class BlockMD extends Module {
    private static final String META_DATA_ID = "PocketPlugins_id";

    public BlockMD() {
        super("BlockMD");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        String id = UUID.randomUUID().toString();
        block.setMetadata(META_DATA_ID, new FixedMetadataValue(PocketPlugins.getInstance(), id));
        Bukkit.broadcast(Component.text("Placed " + block.getType().name() + " with id " + id), "");
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() == null)
            return;

        EquipmentSlot hand = event.getHand();
        Action action = event.getAction();

        if (hand.equals(EquipmentSlot.HAND)
                && action.equals(Action.LEFT_CLICK_BLOCK)) {
            Material itemInHand = event.getPlayer().getInventory().getItemInMainHand().getType();
            if (itemInHand.equals(Material.STICK)) {
                Block block = event.getClickedBlock();
                if (block != null && block.hasMetadata(META_DATA_ID)) {
                    Bukkit.broadcast(Component.text(block.getMetadata(META_DATA_ID).get(0).asString()), "");
                }
            }
        }
    }
}
