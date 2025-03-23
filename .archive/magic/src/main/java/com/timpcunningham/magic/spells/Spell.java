package com.timpcunningham.magic.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.mcutils.projectile.BaseProjectile;
import com.timpcunningham.magic.Magic;

public abstract class Spell {
  List<BaseProjectile> projectiles;
  List<Modifier> modifiers;
  Player owner;

  public Spell(Player owner) {
    projectiles = new ArrayList<>();
    modifiers = new ArrayList<>();
    this.owner = owner;
  }

  public Player getOwner() {
    return this.owner;
  }

  public void addModifier(Modifier modifier) {
    this.modifiers.add(modifier);
    modifier.modifyBeforeCast(this);
  }

  public void onEntityCollision(BaseProjectile projectile, Entity collidedWith, RayTraceResult result,
      Location hitLocation) {

    for (Modifier modifier : this.modifiers) {
      modifier.modifyOnEntityCollision(this, collidedWith, hitLocation);
    }
  }

  public void onBlockCollision(BaseProjectile projectile, Block block, RayTraceResult result, Location hitLocation) {
    for (Modifier modifier : this.modifiers) {
      modifier.modifyOnBlockCollision(this, block, hitLocation);
    }
  }

  public void onDisplay(BaseProjectile projectile, Location previousLocation) {
    for (Modifier modifier : this.modifiers) {
      modifier.modifyDraw(this, projectile, previousLocation);
    }
  }

  public void onDeath(BaseProjectile projectile) {
    this.projectiles.remove(projectile);
    for (Modifier modifier : this.modifiers) {
      modifier.modifyOnDecay(this);
    }
  }

  public void onUpdate(BaseProjectile projectile) {
    for (Modifier modifier : this.modifiers) {
      modifier.modifyOnUpdate(this);
    }
  }

  public List<BaseProjectile> getActiveProjectiles() {
    return this.projectiles;
  }

  public BaseProjectile getBaseProjectile() {
    return new BaseProjectile(Magic.getInstance(), 0.2f, 100, .75f);
  }

  public BaseProjectile getSpellProjectile() {
    BaseProjectile projectile = this.getBaseProjectile();

    projectile.addProjectileHitEntityListener(this::onEntityCollision);
    projectile.addProjectileHitBlockListener(this::onBlockCollision);
    projectile.addProjectileDrawListener(this::onDisplay);
    projectile.addProjectileDecayListener(this::onDeath);
    projectile.addUpdateListener(this::onUpdate);

    return projectile;
  }

  public void beforeCast() {

  }

  public void cast(Location location, Vector direction) {
    BaseProjectile projectile = this.getSpellProjectile();
    this.beforeCast();

    for (Modifier modifier : this.modifiers) {
      modifier.modifyAtCast(this, projectile, location, direction);
    }

    projectile.applyForce(direction.normalize().multiply(1.5));
    projectile.launch(location);
    this.projectiles.add(projectile);
  }
}
