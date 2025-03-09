package com.mrjoshuasperry.pocketplugins.additions.biomebombs;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.mcutils.LocationUtils;
import com.mrjoshuasperry.mcutils.projectile.BaseProjectile;
import com.mrjoshuasperry.mcutils.projectile.ProjectileUtils;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BiomeBombProjectile {
  private final String AS_META_KEY = "biome_bomb_as";
  private final String TYPE_META_KEY = "biome_bomb_type";
  private final String COLOR_META_KEY = "biome_bomb_color";
  private final String OWNER_META_KEY = "biome_bomb_owner";
  private final int explosionRange;
  private final ItemStack projectileItem;
  private BaseProjectile projectile;

  public BiomeBombProjectile(int explosionRange, String type, Color color, ItemStack projectileItem) {
    this.projectile = new BaseProjectile(PocketPlugins.getInstance(), 0.1f, -1, 1f);
    this.explosionRange = explosionRange;
    this.projectileItem = projectileItem;
    initProjectile(type, color);
  }

  private void initProjectile(String type, Color color) {
    this.projectile.addUpdateListener(proj -> proj.applyForce(new Vector(0, -0.025, 0)));
    this.projectile.addProjectileDrawListener((proj, prevLocation) -> {
      ArmorStand as = (ArmorStand) proj.getMetaData(AS_META_KEY);
      ProjectileUtils.teleportAndTranslateArmorStand(as, proj.getPosition());
    });
    this.projectile.addProjectileHitBlockListener(this::onBlockCollision);

    this.projectile.addMetaData(TYPE_META_KEY, type);
    this.projectile.addMetaData(COLOR_META_KEY, color);
  }

  public void launchProjectile(Player owner) {
    ArmorStand armorStand = ProjectileUtils.spawnArmorStand(owner.getEyeLocation(), projectileItem, false);

    this.projectile.addMetaData(AS_META_KEY, armorStand);
    this.projectile.addMetaData(OWNER_META_KEY, owner);
    this.projectile.applyForce(owner.getLocation().clone().getDirection().normalize());
    this.projectile.launch(owner.getEyeLocation());
  }

  private void onBlockCollision(BaseProjectile projectile, Block block, RayTraceResult rayTrace, Location hitLocation) {
    ArmorStand armorStand = (ArmorStand) projectile.getMetaData(AS_META_KEY);
    World world = projectile.getPosition().getWorld();
    Player player = (Player) this.projectile.getMetaData(OWNER_META_KEY);

    world.playSound(hitLocation, Sound.BLOCK_BELL_RESONATE, 2f, 1f);
    world.playSound(hitLocation, Sound.ITEM_TOTEM_USE, 0.5f, 2f);
    spawnCollisionParticles(projectile);
    transformBiome(projectile);

    player.sendMessage(
        Component.text("Biome updated! You may have to relog to see the changes").color(NamedTextColor.GRAY));

    armorStand.remove();
    projectile.queueCancel();
  }

  private void spawnCollisionParticles(BaseProjectile proj) {
    Color color = (Color) proj.getMetaData(COLOR_META_KEY);
    Location origin = proj.getPosition().clone();
    World world = proj.getPosition().getWorld();
    int range = this.explosionRange;

    new BukkitRunnable() {
      int age = 0;
      static final int LIFE = 20;

      @Override
      public void run() {
        if (age > LIFE) {
          this.cancel();
        }
        world.spawnParticle(Particle.DUST, origin, 200, range, range, range, 0,
            new Particle.DustOptions(color, 1));
        age += 5;
      }
    }.runTaskTimer(PocketPlugins.getInstance(), 0, 5);
  }

  private void transformBiome(BaseProjectile proj) {
    World world = proj.getPosition().getWorld();
    Location origin = proj.getPosition().clone();
    Location effectedLocation = origin.clone();
    String type = String.valueOf(proj.getMetaData(TYPE_META_KEY)).toUpperCase();
    Biome biome = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
        .get(new NamespacedKey("minecraft", type.toLowerCase()));

    origin.add(0.5, 0, 0.5);

    for (int x = -this.explosionRange - 1; x < this.explosionRange + 1; x++) {
      for (int z = -this.explosionRange - 1; z < this.explosionRange + 1; z++) {
        LocationUtils.setXYZ(effectedLocation, origin.getX() + x, origin.getY(),
            origin.getZ() + z);
        if (origin.distance(effectedLocation) <= this.explosionRange) {
          world.getBlockAt(effectedLocation).setBiome(biome);
        }
      }
    }
  }
}
