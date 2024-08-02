package com.mrjoshuasperry.mcutils;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.builders.PotionBuilder;
import com.mrjoshuasperry.mcutils.types.ColorTypes;

public class RandomItems {
    private static final Random random = new Random();

    /**
     * A list of materials both unsafe and creative that are prohibited from being
     * obtained.
     * <br>
     * <br>
     * Unsafe materials are materials that should not be spawned as an item stack.
     * Doing so will throw an error and under the correct circumstances can crash
     * the server.
     * <br>
     * <br>
     * Creative materials are materials that are generally used for creative
     * purposes and have little viability in a survival or adventure world.
     */
    private static final List<Material> prohibitedMaterials = Lists.newArrayList(
            // Unsafe materials
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR, Material.BUBBLE_COLUMN, Material.MOVING_PISTON,
            Material.PISTON_HEAD, Material.END_PORTAL, Material.NETHER_PORTAL, Material.END_GATEWAY,
            Material.REDSTONE_WIRE,
            Material.TRIPWIRE, Material.FIRE, Material.WATER, Material.LAVA, Material.FROSTED_ICE,
            Material.TALL_SEAGRASS, Material.KELP_PLANT, Material.BAMBOO_SAPLING, Material.SWEET_BERRY_BUSH,
            Material.POTATOES,
            Material.BEETROOTS, Material.CARROTS, Material.COCOA,

            // Creative materials
            Material.COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART, Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK, Material.BARRIER,
            Material.JIGSAW, Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.SPAWNER,
            Material.WRITTEN_BOOK);

    /**
     * A list of strings that are used to prohibit a swath of material types that
     * share a common string easily.
     * <br>
     * <br>
     * For example, using the string "LEGACY_" will prohibit and material that is
     * legacy and shouldn't be used.
     */
    private static final List<String> prohibitedStrings = Lists.newArrayList(
            "LEGACY_", "POTTED_", "WALL_", "_STEM");

    /**
     * Materials that generally have caveats when converting them to item stacks.
     * <br>
     * <br>
     * These special case materials are handled separately and their item stacks can
     * be retrieved by using their appropriate method.
     * If these items are not desired you can use
     * <code>getRandomSafeMaterial()</code>.
     */
    private static final List<Material> unsafeMaterials = Lists.newArrayList(
            Material.POTION, Material.LINGERING_POTION, Material.SPLASH_POTION, Material.ENCHANTED_BOOK,
            Material.TIPPED_ARROW,
            Material.FIREWORK_ROCKET, Material.FIREWORK_STAR);

    /**
     * @return A random item stack from all available materials
     */
    public static ItemStack getRandomItem() {
        final Random random = RandomItems.random;
        final Material type = RandomItems.getMaterial();

        // Special case with banners as their base material can be any color
        if (type.name().endsWith("_BANNER")) {
            return RandomItems.getRandomBanner(random.nextInt(6));
        }

        // Amount used whenever extra data is required (ex: number of colors)
        final int amount = random.nextInt(3) + 1;
        switch (type) {
            case POTION:
            case LINGERING_POTION:
            case SPLASH_POTION:
                return RandomItems.getRandomPotion();
            case ENCHANTED_BOOK:
                return RandomItems.getRandomEnchantedBook(amount);
            case TIPPED_ARROW:
                return RandomItems.getRandomTippedArrow();
            case FIREWORK_ROCKET:
                return RandomItems.getRandomFirework(amount);
            case FIREWORK_STAR:
                return RandomItems.getRandomFireworkStar(amount);
            default:
                return new ItemStack(type);
        }
    }

    /**
     * Gets a random item not from unsafe materials. Unsafe materials are defined as
     * materials that require extra data (meta manipulation) to have a viable item
     * stack.
     * 
     * @return A random item stack from safe materials
     */
    public static ItemStack getRandomSafeItem() {
        Material type;
        do {
            type = RandomItems.getMaterial();
        } while (RandomItems.unsafeMaterials.contains(type));

        // Special case for banners as their base material can be any color
        if (type.name().endsWith("_BANNER")) {
            return RandomItems.getRandomBanner(RandomItems.random.nextInt(6) + 1);
        }

        return new ItemStack(type);
    }

    /**
     * @return A potion that can be drinkable, splash, or lingering with a random
     *         effect that may be extended or upgraded
     */
    public static ItemStack getRandomPotion() {
        final Random random = RandomItems.random;
        final ItemStack potion = new PotionBuilder().setBase(RandomItems.getRandomPotionData()).build();

        // Randomly set the potion to a splash or lingering potion
        if (random.nextBoolean()) {
            potion.setType(random.nextBoolean() ? Material.SPLASH_POTION : Material.LINGERING_POTION);
        }

        return potion;
    }

    /**
     * @param enchantments The number of enchantments to add to the book
     * @return An enchanted book with random enchantments
     */
    public static ItemStack getRandomEnchantedBook(final int enchantments) {
        final Random random = RandomItems.random;
        // Array of all possible enchantments
        final Enchantment[] allEnchants = Enchantment.values();

        // Map of all current enchantments and their level
        final HashMap<Enchantment, Integer> enchants = new HashMap<>();

        for (int index = 0; index < enchantments; index++) {
            // The current enchantment to be added
            Enchantment enchant;
            // If there was a conflict with the new enchantment and existing enchantments
            boolean conflict;

            do {
                // Get a new random enchantment
                enchant = allEnchants[random.nextInt(allEnchants.length)];
                conflict = false;

                // Check for conflicts
                for (final Enchantment currentEnchant : enchants.keySet()) {
                    if (enchant.conflictsWith(currentEnchant)) {
                        conflict = true;
                        break;
                    }
                }
            } while (conflict);

            // Add the enchantment with a random level
            enchants.put(enchant, random.nextInt(enchant.getMaxLevel()) + enchant.getStartLevel());
        }

        return new ItemBuilder(Material.ENCHANTED_BOOK)
                .setEnchantments(enchants)
                .build();
    }

    /**
     * @return A tipped arrow with a random potion effect that may be extended or
     *         upgraded
     */
    public static ItemStack getRandomTippedArrow() {
        final ItemStack arrow = new ItemStack(Material.TIPPED_ARROW);
        final PotionMeta meta = (PotionMeta) arrow.getItemMeta();

        if (meta != null) {
            meta.setBasePotionData(RandomItems.getRandomPotionData());
            arrow.setItemMeta(meta);
        }

        return arrow;
    }

    /**
     * @param colors The number of colors to add to the firework
     * @return A firework with randomized effects and colors
     */
    public static ItemStack getRandomFirework(final int colors) {
        final ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
        final FireworkMeta meta = (FireworkMeta) firework.getItemMeta();

        if (meta != null) {
            // Survival fireworks only go up to 3 power
            meta.setPower(RandomItems.random.nextInt(3) + 1);
            // Only add one effect per firework
            meta.addEffect(RandomItems.getRandomFireworkEffect(colors));
            firework.setItemMeta(meta);
        }

        return firework;
    }

    /**
     * @param colors The number of colors to add to the firework star
     * @return A firework star with randomized effects and colors
     */
    public static ItemStack getRandomFireworkStar(final int colors) {
        final ItemStack star = new ItemStack(Material.FIREWORK_STAR);
        final FireworkEffectMeta meta = (FireworkEffectMeta) star.getItemMeta();

        if (meta != null) {
            meta.setEffect(RandomItems.getRandomFireworkEffect(colors));
            star.setItemMeta(meta);
        }

        return star;
    }

    /**
     * @param patterns The number of patterns to add to the banner
     * @return A banner of a random base color with random patterns added
     */
    public static ItemStack getRandomBanner(final int patterns) {
        final Random random = RandomItems.random;
        // Colors that a base banner can be set to
        final DyeColor[] colors = DyeColor.values();
        // Pattern types that can be applied to a banner
        final PatternType[] types = PatternType.values();

        // Get a banner of a random base color
        final ItemStack banner = new ItemStack(
                Material.valueOf(colors[random.nextInt(colors.length)].name() + "_BANNER"));
        final BannerMeta meta = (BannerMeta) banner.getItemMeta();

        if (meta != null) {
            for (int index = 0; index < patterns; index++) {
                // Add a new pattern with a random color and type
                meta.addPattern(
                        new Pattern(colors[random.nextInt(colors.length)], types[random.nextInt(types.length)]));
            }

            banner.setItemMeta(meta);
        }

        return banner;
    }

    /**
     * @return Gets a random unsafe material
     */
    private static Material getMaterial() {
        // An array of all possible material types
        final Material[] materials = Material.values();

        // Check if a material is prohibited
        final Predicate<Material> isProhibited = (material) -> {
            // Check if this material contains a prohibited string
            for (final String string : RandomItems.prohibitedStrings) {
                if (material.name().contains(string)) {
                    return true;
                }
            }

            // Check if this material is prohibited
            return RandomItems.prohibitedMaterials.contains(material);
        };

        // Get a random non-prohibited material
        Material material;
        do {
            material = materials[RandomItems.random.nextInt(materials.length)];
        } while (isProhibited.test(material));

        return material;
    }

    /**
     * @return Potion data with a random effect that may be extended or upgraded
     */
    private static PotionData getRandomPotionData() {
        final Random random = RandomItems.random;
        // An array of all possible potion effects
        final PotionType[] types = PotionType.values();

        // Get a random potion type
        PotionType type = types[random.nextInt(types.length)];

        boolean extend = false;
        boolean upgrade = false;

        // Randomly extend and upgrade the potion
        if (random.nextBoolean()) {
            if (random.nextBoolean() && type.isExtendable()) {
                extend = true;
            } else if (type.isUpgradeable()) {
                upgrade = true;
            }
        }

        return new PotionData(type, extend, upgrade);
    }

    /**
     * @param colors The number of colors to add to the effect
     * @return A firework effect with randomly assigned trail, flicker, type, colors
     *         and fades
     */
    private static FireworkEffect getRandomFireworkEffect(final int colors) {
        final Random random = RandomItems.random;
        // An array of all possible firework explosion types
        final FireworkEffect.Type[] types = FireworkEffect.Type.values();
        // A list of all possible firework explosion and fade colors
        final List<Color> effectColors = Lists.newArrayList(ColorTypes.getColors().values());

        final FireworkEffect.Builder effect = FireworkEffect.builder()
                .flicker(random.nextBoolean())
                .trail(random.nextBoolean())
                .with(types[random.nextInt(types.length)]);

        // Add random colors and fades
        for (int index = 0; index < colors; index++) {
            effect.withColor(effectColors.get(random.nextInt(effectColors.size())));

            if (random.nextBoolean()) {
                effect.withFade(effectColors.get(random.nextInt(effectColors.size())));
            }
        }

        return effect.build();
    }
}
