package com.timpcunningham.magic;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import com.timpcunningham.magic.spells.TestSpell;

public class MagicTest implements Listener {

  @EventHandler
  public void onItemUse(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getItem().getType().equals(Material.STICK)) {
      TestSpell spell = new TestSpell(event.getPlayer());

      Vector eyeDir = event.getPlayer().getEyeLocation().getDirection().normalize();
      Location source = event.getPlayer().getEyeLocation().clone().add(eyeDir);

      spell.cast(source, eyeDir);
    }
  }

}
