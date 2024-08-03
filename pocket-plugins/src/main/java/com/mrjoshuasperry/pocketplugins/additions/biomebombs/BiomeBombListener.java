package com.mrjoshuasperry.pocketplugins.additions.biomebombs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.mrjoshuasperry.pocketplugins.MiniAdditions;
import com.mrjoshuasperry.pocketplugins.utils.CraftingUtil;
import com.mrjoshuasperry.pocketplugins.utils.CustomProjectile;

import com.mrjoshuasperry.mcutils.ItemMetaHandler;
import com.mrjoshuasperry.mcutils.LocationUtils;
import com.mrjoshuasperry.mcutils.builders.ItemBuilder;

public class BiomeBombListener extends Module {
    private final NamespacedKey biomeBombTypeKey;
    private final NamespacedKey biomeBombColorKey;
    private final PersistentDataType<String, String> STRING = PersistentDataType.STRING;
    private final PersistentDataType<Integer, Integer> INT = PersistentDataType.INTEGER;
    private int blastRange;

    public BiomeBombListener() {
        super("BiomeBombs");
        biomeBombTypeKey = new NamespacedKey(MiniAdditions.getInstance(), "biomb_bomb_type");
        biomeBombColorKey = new NamespacedKey(MiniAdditions.getInstance(), "biomb_bomb_color");
        this.initRecipes();
    }

    @Override
    public void init(YamlConfiguration config) {
        super.init(config);
        this.blastRange = config.getInt("bomb-range", 5);
    }

    @EventHandler
    public void onBiomeBombUse(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        Player player = event.getPlayer();
        if (ItemMetaHandler.hasKey(item, biomeBombTypeKey, STRING)) {
            CustomProjectile biomeBomb = getBiomeBomb(item, player.getEyeLocation(),
                    player.getLocation().getDirection());
            item.setAmount(item.getAmount() - 1);
            biomeBomb.launch();
        }
    }

    private CustomProjectile getBiomeBomb(ItemStack item, Location location, Vector direction) {
        CustomProjectile biomeBomb = new CustomProjectile(location, direction, 0.1, 500)
                .addAcceleration(0.1, 1.3)
                .addGravity(0.05)
                .onDisplay(proj -> {
                    if (proj.hasMetadata("biomebomb_as")) {
                        ArmorStand armorStand = (ArmorStand) proj.getMetadata("biomebomb_as");
                        armorStand.teleport(proj.getLocation().clone().subtract(0, 2, 0));
                    }
                })
                .onBlockCollision((proj, block) -> {
                    if (proj.hasMetadata("biomebomb_as") && proj.hasMetadata("type")) {
                        ArmorStand armorStand = (ArmorStand) proj.getMetadata("biomebomb_as");
                        String type = String.valueOf(proj.getMetadata("type")).toUpperCase();
                        Biome biome = Biome.valueOf(type);

                        int color = (int) proj.getMetadata("color");
                        World world = proj.getLocation().getWorld();
                        if (world == null) {
                            return;
                        }
                        Location origin = proj.getLocation().clone();

                        new BukkitRunnable() {
                            int age = 0;
                            final int life = 20;

                            @Override
                            public void run() {
                                if (age > life) {
                                    this.cancel();
                                }
                                world.spawnParticle(Particle.REDSTONE, origin, 200, 2, 2, 2, 0,
                                        new Particle.DustOptions(Color.fromRGB(color), 1));
                                age += 5;
                            }
                        }.runTaskTimer(MiniAdditions.getInstance(), 0, 5);
                        world.playSound(origin, Sound.BLOCK_BELL_RESONATE, 2f, 1f);
                        world.playSound(origin, Sound.ITEM_TOTEM_USE, 0.5f, 2f);

                        origin.add(0.5, 0, 0.5);
                        Location effectedLocation = origin.clone();

                        for (int x = -this.blastRange - 1; x < this.blastRange + 1; x++) {
                            for (int z = -this.blastRange - 1; z < this.blastRange + 1; z++) {
                                LocationUtils.setXYZ(effectedLocation, origin.getX() + x, origin.getY(),
                                        origin.getZ() + z);
                                if (origin.distance(effectedLocation) <= this.blastRange) {
                                    world.getBlockAt(effectedLocation).setBiome(biome);
                                }
                            }
                        }

                        armorStand.remove();
                        proj.destroy();
                    }
                });
        World world = location.getWorld();
        if (world == null) {
            return biomeBomb;
        }
        ArmorStand armorStand = (ArmorStand) world.spawnEntity(location.clone().subtract(0, 2, 0),
                EntityType.ARMOR_STAND);
        armorStand.setInvulnerable(true);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(item.clone());
        }

        biomeBomb.setMetaData("biomebomb_as", armorStand);
        biomeBomb.setMetaData("type", ItemMetaHandler.get(item, biomeBombTypeKey, STRING));
        biomeBomb.setMetaData("color", ItemMetaHandler.get(item, biomeBombColorKey, INT));
        return biomeBomb;
    }

    private void addRecipe(Map<Material, Integer> ingredients, String type, ChatColor textColor, Color fireworkColor) {
        String displayName = type.replace('_', ' ');
        ItemStack item = new ItemBuilder(Material.FIREWORK_STAR)
                .setName(textColor + displayName + " " + ChatColor.GRAY + "Biome Bomb")
                .setLore(Collections.singletonList(ChatColor.GRAY + "Type: " + ChatColor.GOLD + displayName))
                .build();

        ItemMetaHandler.set(item, biomeBombTypeKey, STRING, type.toUpperCase());
        ItemMetaHandler.set(item, biomeBombColorKey, INT, fireworkColor.asRGB());

        FireworkEffectMeta meta = (FireworkEffectMeta) item.getItemMeta();
        if (meta != null) {
            FireworkEffect effect = FireworkEffect.builder().withColor(fireworkColor).build();
            meta.setEffect(effect);
            item.setItemMeta(meta);
        }

        CraftingUtil.addShapelessCrafting("Biomb_Bomb_" + type, ingredients, item);
    }

    private void initRecipes() {
        // Plains
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.GRASS, 8);
                    }
                },
                "Plains",
                ChatColor.GREEN,
                Color.fromRGB(130, 255, 185));

        // Ocean
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.KELP, 8);
                    }
                },
                "Ocean",
                ChatColor.BLUE,
                Color.fromRGB(95, 205, 225));

        // Forest
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.OAK_LEAVES, 8);
                    }
                },
                "Forest",
                ChatColor.DARK_GREEN,
                Color.fromRGB(75, 130, 35));

        // Desert
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.SAND, 8);
                    }
                },
                "Desert",
                ChatColor.YELLOW,
                Color.fromRGB(255, 240, 150));

        // Tiaga
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.SPRUCE_LEAVES, 8);
                    }
                },
                "Tiaga",
                ChatColor.DARK_BLUE,
                Color.fromRGB(90, 115, 60));

        // Jungle
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.JUNGLE_LEAVES, 8);
                    }
                },
                "Jungle",
                ChatColor.GREEN,
                Color.fromRGB(55, 255, 45));

        // Mesa
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.TERRACOTTA, 8);
                    }
                },
                "Badlands",
                ChatColor.GOLD,
                Color.fromRGB(165, 120, 90));

        // Roofed Forest
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.DARK_OAK_LEAVES, 8);
                    }
                },
                "Dark_Forest",
                ChatColor.DARK_GREEN,
                Color.fromRGB(25, 90, 25));

        // Mushroom Island
        this.addRecipe(
                new HashMap<Material, Integer>() {
                    {
                        put(Material.EGG, 1);
                        put(Material.MYCELIUM, 8);
                    }
                },
                "Mushroom_Fields",
                ChatColor.LIGHT_PURPLE,
                Color.fromRGB(200, 125, 200));
    }
}
