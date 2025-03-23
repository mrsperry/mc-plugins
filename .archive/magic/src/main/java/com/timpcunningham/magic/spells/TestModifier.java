package com.timpcunningham.magic.spells;

import java.util.Collection;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.mrjoshuasperry.mcutils.ColoredParticle;
import com.mrjoshuasperry.mcutils.projectile.BaseProjectile;

public class TestModifier extends Modifier {

  @Override
  public void modifyOnUpdate(Spell spell) {
    for (BaseProjectile projectile : spell.getActiveProjectiles()) {
      Collection<Entity> entities = projectile.getPosition().getWorld().getNearbyEntities(projectile.getPosition(), 3,
          3, 3);

      for (Entity entity : entities) {
        if (entity instanceof Player && ((Player) entity) == spell.getOwner())
          continue;
        entity.setFireTicks(40);
      }

    }
  }

  @Override
  public void modifyDraw(Spell spell, BaseProjectile projectile, Location previousLocation) {
    ColoredParticle.displaySphere(projectile.getPosition(), Particle.DUST, 1.5, 200,
        new Particle.DustOptions(Color.fromRGB(242, 116, 53), .4f));
  }

}
