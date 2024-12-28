package com.timpcunningham.magic.spells;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.mcutils.projectile.BaseProjectile;

public abstract class Modifier {

  public void modifyBeforeCast(Spell spell) {
  }

  public void modifyAtCast(Spell spell, BaseProjectile projectileToBeFired, Location location, Vector direction) {

  }

  public void modifyOnUpdate(Spell spell) {
  }

  public void modifyDraw(Spell spell, BaseProjectile projectile, Location previousLocation) {
  }

  public void modifyOnEntityCollision(Spell spell, Entity collidedWith, Location hitLocation) {

  }

  public void modifyOnBlockCollision(Spell spell, Block block, Location hitLocation) {
  }

  public void modifyOnDecay(Spell spell) {
  }
}
