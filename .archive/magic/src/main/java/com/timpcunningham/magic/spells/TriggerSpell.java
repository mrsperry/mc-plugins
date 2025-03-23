package com.timpcunningham.magic.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import com.mrjoshuasperry.mcutils.projectile.BaseProjectile;

public abstract class TriggerSpell extends Spell {
  private int spellCasts;
  private List<Spell> spells;

  public TriggerSpell(Player owner, int spellCasts) {
    super(owner);

    this.spellCasts = spellCasts;
    this.spells = new ArrayList<>();
  }

  public int getMaxSpellCasts() {
    return this.spellCasts;
  }

  public int getSpellCasts() {
    return this.spells.size();
  }

  public boolean addSpell(Spell spell) {
    if (this.spells.size() >= this.spellCasts) {
      return false;
    }

    this.spells.add(spell);
    return true;
  }

  public List<Spell> getSpells() {
    return this.spells;
  }

  @Override
  public void onBlockCollision(BaseProjectile projectile, Block block, RayTraceResult result, Location hitLocation) {
    for (Spell spell : this.spells) {
      spell.cast(hitLocation, projectile.getPosition().getDirection());
    }
  }
}
