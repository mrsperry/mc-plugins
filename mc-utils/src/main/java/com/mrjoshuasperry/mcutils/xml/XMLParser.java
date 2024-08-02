package com.mrjoshuasperry.mcutils.xml;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mrjoshuasperry.mcutils.builders.ItemBuilder;
import com.mrjoshuasperry.mcutils.builders.PotionBuilder;
import com.mrjoshuasperry.mcutils.classes.Pair;

public class XMLParser {
    /**
     * Parses an XML document
     * 
     * @param file The XML document to parse
     * @return The root element of the document
     */
    public static Element parse(final File file) {
        final Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        } catch (Exception ex) {
            Bukkit.getLogger().severe("Could not parse XML file: " + file.getName());
            return null;
        }

        final Element root = document.getDocumentElement();
        root.normalize();

        return root;
    }

    /**
     * Gets a node's child elements
     * 
     * @param node The parent node
     * @return A set of child elements
     */
    public static Set<Element> getChildElements(final Node node) {
        final HashSet<Element> elements = new HashSet<>();

        final NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            final Node child = children.item(index);

            // Only add element nodes
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) child);
            }
        }

        return elements;
    }

    /**
     * Gets a specific node child element
     * 
     * @param node The parent node
     * @param name The case insensitive name of the element
     * @return The child element or null if none could be found
     */
    public static Element getChildElement(final Node node, final String name) {
        Element result = null;
        for (final Element element : XMLParser.getChildElements(node)) {
            if (element.getNodeName().equalsIgnoreCase(name)) {
                result = element;
            }
        }

        return result;
    }

    /**
     * Parses a constant value
     * <br>
     * <br>
     * This capitalizes the string and replaces all spaces with underscores ex: "the
     * example" -> "THE_EXAMPLE"
     * 
     * @param constant The constant to parse
     * @return The formatted string
     */
    public static String parseConstant(final String constant) {
        return constant.toUpperCase().replaceAll(" ", "_");
    }

    /**
     * Parses a range value
     * <br>
     * <br>
     * Ranges are formatted as follows: {@code 1-3}
     * 
     * @param range The range to parse
     * @return A pair containing the min and max value
     */
    public static Pair<Double, Double> parseRange(final String range) {
        // Check if the range is a formatted range or a single number
        if (range.contains("-")) {
            final String[] split = range.split("-");

            // Parse each number
            try {
                return new Pair<>(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse range: " + range);
                return null;
            }
        }

        // Parse the number
        final double number;
        try {
            number = Double.parseDouble(range);
        } catch (final Exception ex) {
            Bukkit.getLogger().severe("Could not pare range: " + range);
            return null;
        }

        return new Pair<>(number, number);
    }

    /**
     * Parses a duration value
     * <br>
     * <br>
     * Duration are formatted as follows: 15s | 15m | 15h | 15d | *
     * <br>
     * {@code s} = seconds
     * <br>
     * {@code m} = minutes
     * <br>
     * {@code h} = hours
     * <br>
     * {@code d} = days
     * <br>
     * {@code *} = infinite
     * 
     * @param duration The duration to parse
     * @return The number of ticks the duration represents
     */
    public static int parseDuration(final String duration) {
        // Check if the duration is infinite
        if (duration.equals("*")) {
            return Integer.MAX_VALUE;
        }

        // Parse the number before modifiers
        final int number;
        try {
            number = Integer.parseInt(duration.substring(0, duration.length() - 1));
        } catch (final Exception ex) {
            Bukkit.getLogger().severe("Could not parse duration number: " + duration);
            return -1;
        }

        // Apply the tick modifier
        int multiplier = 20;
        if (duration.endsWith("m")) {
            multiplier *= 60;
        } else if (duration.endsWith("h")) {
            multiplier *= 3600;
        } else if (duration.endsWith("d")) {
            multiplier *= 86400;
        }

        return number * multiplier;
    }

    /**
     * Parses a location value
     * <br>
     * <br>
     * Location elements are formatted as follows:
     * 
     * <pre>{@code
     * <location pitch="80.5" yaw="0">45.5,23,1.34</location>
     * }</pre>
     * 
     * The pitch and yaw attributes may be excluded independently
     * 
     * @param element The element to parse
     * @param world   The world the location resides in
     * @return The corresponding location or null if it could not be parsed
     */
    public static Location parseLocation(final Element element, final World world) {
        final Location location;

        // Parse the coordinates
        final String content = element.getTextContent();
        try {
            final String[] coords = content.split(",");

            location = new Location(world,
                    Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1]),
                    Double.parseDouble(coords[2]));
        } catch (final Exception ex) {
            Bukkit.getLogger().severe("Could not parse location coordinates: " + content);
            return null;
        }

        // Check for a pitch attribute
        if (element.hasAttribute("pitch")) {
            final String pitch = element.getAttribute("pitch");

            try {
                location.setPitch(Float.parseFloat(pitch));
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse location pitch: " + pitch);
                return null;
            }
        }

        // Check for a yaw attribute
        if (element.hasAttribute("yaw")) {
            final String yaw = element.getAttribute("yaw");

            try {
                location.setYaw(Float.parseFloat(yaw));
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse location yaw: " + yaw);
                return null;
            }
        }

        return location;
    }

    /**
     * Parses a material value
     * <br>
     * <br>
     * Material elements are formatted as follows:
     * 
     * <pre>{@code
     *     <type>stone bricks</type>
     * }</pre>
     * 
     * @param element The element to parse
     * @return The corresponding material or null if it was invalid
     */
    public static Material parseMaterial(final Element element) {
        final String content = element.getTextContent();

        final Material material;
        try {
            material = Material.valueOf(XMLParser.parseConstant(content));
        } catch (final Exception ex) {
            Bukkit.getLogger().severe("Could not parse material: " + content);
            return null;
        }

        return material;
    }

    /**
     * Parses an item stack value
     * <br>
     * <br>
     * Item stack elements are formatted as follows:
     * 
     * <pre>{@code
     * <parent>diamond sword</parent>
     * }</pre>
     * 
     * or
     * 
     * <pre>{@code
     * <parent>
     *     <item name="Flamebringer">
     *         <material>diamond sword</material>
     *         <enchants>
     *             <enchant level="2">fire aspect</enchant>
     *         </enchants>
     *         <lore>
     *             <line>It is said to have culled the Icewalkers...</line>
     *             <line>Perhaps it will do so again?</line>
     *         </lore>
     *     </item>
     * </parent>
     * }</pre>
     * 
     * The name and amount attributes are optional, as are the enchants and lore
     * tags
     * 
     * @param element The element to parse
     * @param random  The random to use
     * @return The corresponding item stack or null if it was invalid
     */
    public static ItemStack parseItem(final Element element, Random random) {
        final ItemBuilder stack = new ItemBuilder();

        if (element.hasAttribute("name")) {
            stack.setName(element.getAttribute("name"));
        }

        if (element.hasAttribute("name-color")) {
            final String content = element.getAttribute("name-color");

            try {
                stack.setNameColor(ChatColor.valueOf(XMLParser.parseConstant(content)));
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse item name color: " + content);
                return null;
            }
        }

        if (element.hasAttribute("amount")) {
            final String content = element.getAttribute("amount");

            final Pair<Double, Double> range = XMLParser.parseRange(content);
            if (range == null) {
                Bukkit.getLogger().severe("Could not parse item amount: " + content);
                return null;
            }

            final int min = (int) Math.round(range.getKey());
            final int max = (int) Math.round(range.getValue());
            stack.setAmount(min + random.nextInt(max - min + 1));
        }

        // Check if the item is strictly a material
        if (XMLParser.getChildElements(element).size() == 0) {
            final Material material = XMLParser.parseMaterial(element);
            if (material == null) {
                return null;
            }

            return stack.setMaterial(material).build();
        }

        for (final Element child : XMLParser.getChildElements(element)) {
            switch (child.getTagName()) {
                case "material":
                    final Material material = XMLParser.parseMaterial(child);
                    if (material == null) {
                        return null;
                    }

                    stack.setMaterial(material);
                    break;
                case "enchants":
                    for (final Element enchantChild : XMLParser.getChildElements(child)) {
                        final Pair<Enchantment, Integer> enchant = XMLParser.parseEnchant(enchantChild);
                        if (enchant == null) {
                            return null;
                        }

                        stack.addEnchantment(enchant.getKey(), enchant.getValue());
                    }
                    break;
                case "lore":
                    for (final Element line : XMLParser.getChildElements(child)) {
                        stack.addLore(line.getTextContent());
                    }
                    break;
            }
        }

        return stack.build();
    }

    /**
     * Parses an enchantment value
     * <br>
     * <br>
     * Enchantment elements are formatted as follows:
     * 
     * <pre>{@code
     * <enchants>
     *     <enchant level="2">fire aspect</enchant>
     * </enchants>
     * }</pre>
     * 
     * The level attribute may be excluded (default level is 1)
     * <br>
     * <br>
     * Note that this method can only parse enchantments under the default Minecraft
     * namespace
     * 
     * @param element The element to parse
     * @return A pair containing the enchantment and its level or null if either
     *         were invalid
     */
    public static Pair<Enchantment, Integer> parseEnchant(final Element element) {
        final String content = element.getTextContent();
        // Get the namespaced key for this enchantment
        final NamespacedKey key = NamespacedKey.minecraft(XMLParser.parseConstant(content).toLowerCase());

        // Get the actual enchantment value
        final Enchantment enchant = EnchantmentWrapper.getByKey(key);
        if (enchant == null) {
            Bukkit.getLogger().severe("Could not parse enchantment: " + content);
            return null;
        }

        int level = 1;
        if (element.hasAttribute("level")) {
            final String levelString = element.getAttribute("level");

            try {
                level = Integer.parseInt(levelString);
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse enchantment level: " + levelString);
                return null;
            }
        }

        return new Pair<>(enchant, level);
    }

    /**
     * Parses a potion value
     * <br>
     * <br>
     * Potion elements are formatted as follows:
     * 
     * <pre>{@code
     * <potion color="255,255,128">
     *     <base extended="true" upgraded="true">slowness</base>
     *     <effect amplifier="3">healing</effect>
     *     <effect duration="3m">resistance</effect>
     * </potion>
     * }</pre>
     * 
     * The potion base defines the base effect with the other effects being added on
     * 
     * @param element The element to parse
     * @return A potion item stack or null if it was invalid
     */
    public static ItemStack parsePotion(final Element element) {
        final PotionBuilder potion = new PotionBuilder();

        if (element.hasAttribute("color")) {
            final String content = element.getAttribute("color");
            final String[] colors = content.split(",");

            try {
                potion.setColor(Integer.parseInt(colors[0]),
                        Integer.parseInt(colors[1]),
                        Integer.parseInt(colors[2]));
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse potion color: " + content);
                return null;
            }
        }

        // Parse each potion effect
        for (final Element child : XMLParser.getChildElements(element)) {
            // Check if the current effect is the base
            if (child.getNodeName().equalsIgnoreCase("base")) {
                final String content = child.getTextContent();

                boolean extended = false;
                if (child.hasAttribute("extended")) {
                    final String extendedContent = child.getAttribute("extended");

                    try {
                        extended = Boolean.parseBoolean(extendedContent);
                    } catch (final Exception ex) {
                        Bukkit.getLogger().severe("Could not parse potion extended value: " + extendedContent);
                        return null;
                    }
                }

                boolean upgraded = false;
                if (child.hasAttribute("upgraded")) {
                    final String upgradedContent = child.getAttribute("upgraded");

                    try {
                        upgraded = Boolean.parseBoolean(upgradedContent);
                    } catch (final Exception ex) {
                        Bukkit.getLogger().severe("Could not parse potion upgraded value: " + upgradedContent);
                        return null;
                    }
                }

                if (extended && upgraded) {
                    Bukkit.getLogger().severe("A potion base cannot be both extended and upgraded: " + content);
                    return null;
                }

                try {
                    potion.setBase(PotionType.valueOf(XMLParser.parseConstant(content)), extended, upgraded);
                } catch (final Exception ex) {
                    Bukkit.getLogger().severe("Could not parse potion base type: " + content);
                    return null;
                }

                continue;
            }

            final PotionEffect effect = XMLParser.parsePotionEffect(child);
            if (effect == null) {
                return null;
            }

            potion.addEffect(effect);
        }

        return potion.build();
    }

    /**
     * Parses a potion effect value
     * <br>
     * <br>
     * Potion effect elements are formatted as follows:
     * 
     * <pre>{@code
     * <potion-effect duration="3m" amplifier="2">resistance</potion-effect>
     * }</pre>
     * 
     * You may exclude the duration and amplifier attributes
     * 
     * @param element The element to parse
     * @return The corresponding potion effect or null if it was invalid
     */
    public static PotionEffect parsePotionEffect(final Element element) {
        final String content = element.getTextContent();

        final PotionEffectType type = PotionEffectType.getByName(XMLParser.parseConstant(content));
        if (type == null) {
            Bukkit.getLogger().severe("Could not parse potion effect type: " + content);
            return null;
        }

        int duration = 0;
        if (element.hasAttribute("duration")) {
            duration = XMLParser.parseDuration(element.getAttribute("duration"));

            if (duration == -1) {
                return null;
            }
        }

        int amplifier = 1;
        if (element.hasAttribute("amplifier")) {
            final String amplifierString = element.getAttribute("amplifier");

            try {
                amplifier = Integer.parseInt(amplifierString);
            } catch (final Exception ex) {
                Bukkit.getLogger().severe("Could not parse potion effect amplifier: " + amplifierString);
                return null;
            }
        }

        return new PotionEffect(type, duration, amplifier);
    }
}
