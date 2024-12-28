package com.timpcunningham.magic.spells;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.mrjoshuasperry.mcutils.ColoredParticle;
import com.mrjoshuasperry.mcutils.projectile.BaseProjectile;
import com.timpcunningham.magic.Magic;

public class TestSpell extends Spell {

  public TestSpell(Player owner) {
    super(owner);

    this.addModifier(new TestModifier());
  }

  @Override
  public void onEntityCollision(BaseProjectile projectile, Entity collidedWith, RayTraceResult result,
      Location hitLocation) {
    super.onEntityCollision(projectile, collidedWith, result, hitLocation);
    Magic.getInstance().getLogger().info("Collided with " + collidedWith.getType().name());
    projectile.queueCancel();
  }

  @Override
  public void onBlockCollision(BaseProjectile projectile, Block block, RayTraceResult result, Location hitLocation) {
    super.onBlockCollision(projectile, block, result, hitLocation);
    Magic.getInstance().getLogger().info("Collided with " + block.getType().name());
    projectile.queueCancel();
  }

  @Override
  public void onDisplay(BaseProjectile projectile, Location previousLocation) {
    super.onDisplay(projectile, previousLocation);
    ColoredParticle.displaySphere(projectile.getPosition(), Particle.DUST, projectile.getRadius(), 200,
        new DustOptions(Color.fromRGB(255, 255, 255), 0.25f));
  }
}
