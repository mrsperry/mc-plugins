package com.mrjoshuasperry.pocketplugins.additions.biomebombs;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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

import com.mrjoshuasperry.mcutils.ItemMetaHandler;
import com.mrjoshuasperry.mcutils.LocationUtils;
import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.pocketplugins.PocketPlugins;
import com.mrjoshuasperry.pocketplugins.utils.CraftingUtil;
import com.mrjoshuasperry.pocketplugins.utils.CustomProjectile;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import net.md_5.bungee.api.ChatColor;

public class BiomeBombListener extends Module {
    private final NamespacedKey biomeBombTypeKey;
    private final NamespacedKey biomeBombColorKey;
    private static final String META_DATA_KEY = "biomebomb_as";
    private static final PersistentDataType<String, String> STRING = PersistentDataType.STRING;
    private static final PersistentDataType<Integer, Integer> INT = PersistentDataType.INTEGER;
    private int blastRange;

    public BiomeBombListener() {
        super("BiomeBombs");
        biomeBombTypeKey = new NamespacedKey(PocketPlugins.getInstance(), "biomb_bomb_type");
        biomeBombColorKey = new NamespacedKey(PocketPlugins.getInstance(), "biomb_bomb_color");
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

    private void spawnCollisionParticles(CustomProjectile proj) {
        int color = (int) proj.getMetadata("color");
        Location origin = proj.getLocation().clone();
        World world = proj.getLocation().getWorld();

        new BukkitRunnable() {
            int age = 0;
            static final int LIFE = 20;

            @Override
            public void run() {
                if (age > LIFE) {
                    this.cancel();
                }
                world.spawnParticle(Particle.DUST, origin, 200, 2, 2, 2, 0,
                        new Particle.DustOptions(Color.fromRGB(color), 1));
                age += 5;
            }
        }.runTaskTimer(PocketPlugins.getInstance(), 0, 5);
    }

    private void transformBiome(CustomProjectile proj, World world) {
        Location origin = proj.getLocation().clone();
        Location effectedLocation = origin.clone();
        String type = String.valueOf(proj.getMetadata("type")).toUpperCase();
        Biome biome = Biome.valueOf(type);

        origin.add(0.5, 0, 0.5);

        for (int x = -this.blastRange - 1; x < this.blastRange + 1; x++) {
            for (int z = -this.blastRange - 1; z < this.blastRange + 1; z++) {
                LocationUtils.setXYZ(effectedLocation, origin.getX() + x, origin.getY(),
                        origin.getZ() + z);
                if (origin.distance(effectedLocation) <= this.blastRange) {
                    world.getBlockAt(effectedLocation).setBiome(biome);
                }
            }
        }
    }

    private void onBlockCollision(CustomProjectile proj) {
        if (proj.hasMetadata(META_DATA_KEY) && proj.hasMetadata("type")) {
            ArmorStand armorStand = (ArmorStand) proj.getMetadata(META_DATA_KEY);

            World world = proj.getLocation().getWorld();
            if (world == null) {
                return;
            }
            Location origin = proj.getLocation().clone();

            spawnCollisionParticles(proj);
            world.playSound(origin, Sound.BLOCK_BELL_RESONATE, 2f, 1f);
            world.playSound(origin, Sound.ITEM_TOTEM_USE, 0.5f, 2f);
            transformBiome(proj, world);

            armorStand.remove();
            proj.destroy();
        }
    }

    private CustomProjectile getBiomeBomb(ItemStack item, Location location, Vector direction) {
        CustomProjectile biomeBomb = new CustomProjectile(location, direction, 0.1, 500)
                .addAcceleration(0.1, 1.3)
                .addGravity(0.05)
                .onDisplay(proj -> {
                    if (proj.hasMetadata(META_DATA_KEY)) {
                        ArmorStand armorStand = (ArmorStand) proj.getMetadata(META_DATA_KEY);
                        armorStand.teleport(proj.getLocation().clone().subtract(0, 2, 0));
                    }
                })
                .onBlockCollision((proj, block) -> this.onBlockCollision(proj));

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

        biomeBomb.setMetaData(META_DATA_KEY, armorStand);
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
        Map<Material, Integer> plainsIngredients = new EnumMap<>(Material.class);
        plainsIngredients.put(Material.EGG, 1);
        plainsIngredients.put(Material.TALL_GRASS, 8);
        this.addRecipe(
                plainsIngredients,
                "Plains",
                ChatColor.GREEN,
                Color.fromRGB(130, 255, 185));

        // Ocean
        Map<Material, Integer> oceanIngredients = new EnumMap<>(Material.class);
        oceanIngredients.put(Material.EGG, 1);
        oceanIngredients.put(Material.KELP, 8);
        this.addRecipe(
                oceanIngredients,
                "Ocean",
                ChatColor.BLUE,
                Color.fromRGB(95, 205, 225));

        // Forest
        Map<Material, Integer> forestIngredients = new EnumMap<>(Material.class);
        forestIngredients.put(Material.EGG, 1);
        forestIngredients.put(Material.OAK_LEAVES, 8);
        this.addRecipe(
                forestIngredients,
                "Forest",
                ChatColor.DARK_GREEN,
                Color.fromRGB(75, 130, 35));

        // Desert
        Map<Material, Integer> desertIngredients = new EnumMap<>(Material.class);
        desertIngredients.put(Material.EGG, 1);
        desertIngredients.put(Material.SAND, 8);
        this.addRecipe(
                desertIngredients,
                "Desert",
                ChatColor.YELLOW,
                Color.fromRGB(255, 240, 150));

        // Tiaga
        Map<Material, Integer> tiagaIngredients = new EnumMap<>(Material.class);
        tiagaIngredients.put(Material.EGG, 1);
        tiagaIngredients.put(Material.SPRUCE_LEAVES, 8);
        this.addRecipe(
                tiagaIngredients,
                "Tiaga",
                ChatColor.DARK_BLUE,
                Color.fromRGB(90, 115, 60));

        // Jungle
        Map<Material, Integer> jungleIngredients = new EnumMap<>(Material.class);
        jungleIngredients.put(Material.EGG, 1);
        jungleIngredients.put(Material.JUNGLE_LEAVES, 8);
        this.addRecipe(
                jungleIngredients,
                "Jungle",
                ChatColor.GREEN,
                Color.fromRGB(55, 255, 45));

        // Mesa
        Map<Material, Integer> mesaIngredients = new EnumMap<>(Material.class);
        mesaIngredients.put(Material.EGG, 1);
        mesaIngredients.put(Material.TERRACOTTA, 8);
        this.addRecipe(
                mesaIngredients,
                "Badlands",
                ChatColor.GOLD,
                Color.fromRGB(165, 120, 90));

        // Roofed Forest
        Map<Material, Integer> darkForestIngredients = new EnumMap<>(Material.class);
        darkForestIngredients.put(Material.EGG, 1);
        darkForestIngredients.put(Material.DARK_OAK_LEAVES, 8);
        this.addRecipe(
                darkForestIngredients,
                "Dark_Forest",
                ChatColor.DARK_GREEN,
                Color.fromRGB(25, 90, 25));

        // Mushroom Island
        Map<Material, Integer> mushroomFieldsIngredients = new EnumMap<>(Material.class);
        mushroomFieldsIngredients.put(Material.EGG, 1);
        mushroomFieldsIngredients.put(Material.MYCELIUM, 8);
        this.addRecipe(
                mushroomFieldsIngredients,
                "Mushroom_Fields",
                ChatColor.LIGHT_PURPLE,
                Color.fromRGB(200, 125, 200));
    }
}
