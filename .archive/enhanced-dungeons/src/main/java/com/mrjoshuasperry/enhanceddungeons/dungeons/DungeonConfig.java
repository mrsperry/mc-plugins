package com.mrjoshuasperry.enhanceddungeons.dungeons;

import com.mrjoshuasperry.enhanceddungeons.Main;
import com.mrjoshuasperry.enhanceddungeons.Utils;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonGate;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonGroup;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonLoot;
import com.mrjoshuasperry.enhanceddungeons.dungeons.content.DungeonMob;

import com.mrjoshuasperry.mcutils.classes.Pair;
import com.mrjoshuasperry.mcutils.xml.XMLParser;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class DungeonConfig {
    /** The world this config handles */
    private final World world;

    /** If the world should be time locked */
    private boolean timeLock;
    /** If the world should allow weather changes */
    private boolean allowWeather;
    /** The max number of party members allowed at a time */
    private int maxPlayers;
    /** A list of spawn locations for players */
    private final Set<Location> spawnLocations;
    /** A list of ending locations for the dungeon */
    private final Set<Location> endingLocations;
    /** A lit of dungeon gates */
    private final Set<DungeonGate> gates;
    /** A list of all dungeon mobs */
    private final Set<DungeonMob> mobs;
    /** A list of all dungeon mob templates */
    private final Map<String, DungeonMob> mobTemplates;
    /** A list of all dungeon mob groups */
    private final Set<DungeonGroup> mobGroups;
    /** A list of all dungeon loot */
    private final Set<DungeonLoot> loot;

    /**
     * Creates a new dungeon config
     * @param world The world this config handles
     * @param config The XML file to parse
     */
    public DungeonConfig(final World world, final File config) {
        this.world = world;

        this.timeLock = false;
        this.allowWeather = true;
        this.maxPlayers = Integer.MAX_VALUE;
        this.spawnLocations = new HashSet<>();
        this.endingLocations = new HashSet<>();
        this.gates = new HashSet<>();
        this.mobs = new HashSet<>();
        this.mobTemplates = new HashMap<>();
        this.mobGroups = new HashSet<>();
        this.loot = new HashSet<>();

        // Parse the XML
        final Element root = XMLParser.parse(config);

        // Parse each top level element's children
        for (final Element child : XMLParser.getChildElements(root)) {
            final String name = child.getNodeName();

            // Parse any mob templates before parsing mobs
            if (name.equalsIgnoreCase("mobs")) {
                for (final Element templates : XMLParser.getChildElements(child)) {
                    if (templates.getNodeName().equalsIgnoreCase("templates")) {
                        this.parseMobTemplates(templates);
                        break;
                    }
                }
            }

            for (final Element element : XMLParser.getChildElements(child)) {
                switch (name) {
                    case "settings":
                        this.parseSetting(element);
                        break;
                    case "spawns":
                        this.parseSpawnLocation(element);
                        break;
                    case "gates":
                        this.parseGate(element);
                        break;
                    case "endings":
                        this.parseEndingLocation(element);
                        break;
                    case "mobs":
                        // Ignore templates that have already been parsed
                        if (element.getNodeName().equalsIgnoreCase("templates")) {
                            continue;
                        }

                        // Parse mob groups separately from regular mobs
                        if (element.getNodeName().equalsIgnoreCase("mob-group")) {
                            final DungeonGroup group = this.parseMobGroup(element);
                            if (group != null) {
                                this.mobGroups.add(group);
                            }

                            continue;
                        }

                        final DungeonMob mob = this.parseMob(element);
                        if (mob != null) {
                            this.mobs.add(mob);
                        }
                        break;
                    case "lootables":
                        this.parseLootables(element);
                        break;
                }
            }
        }
    }

    /**
     * Parses a world setting
     * @param element The element to parse
     */
    private void parseSetting(final Element element) {
        final String content = element.getTextContent();

        switch (element.getNodeName()) {
            case "time-lock":
                try {
                    this.timeLock = Boolean.parseBoolean(content);
                } catch (final Exception ex) {
                    Utils.log(Level.SEVERE, "Could not parse time lock setting: " + content);
                }
                break;
            case "allow-weather":
                try {
                    this.allowWeather = Boolean.parseBoolean(content);
                } catch (final Exception ex) {
                    Utils.log(Level.SEVERE, "Could not parse weather setting: " + content);
                }
                break;
            case "max-players":
                try {
                    this.maxPlayers = Integer.parseInt(content);
                } catch (final Exception ex) {
                    Utils.log(Level.SEVERE, "Could not parse max players setting: " + content);
                }
                break;
        }
    }

    /**
     * Parses and adds a spawn location
     * @param element The element to parse
     */
    private void parseSpawnLocation(final Element element) {
        final Location location = XMLParser.parseLocation(element, this.world);

        if (location != null) {
            this.spawnLocations.add(location);
        }
    }

    /**
     * Parses and adds an ending location
     * @param element The element to parse
     */
    private void parseEndingLocation(final Element element) {
        final Location location = XMLParser.parseLocation(element, this.world);

        if (location != null) {
            this.endingLocations.add(location);
        }
    }

    /**
     * Parses and adds a gate
     * @param element The element to parse
     */
    private void parseGate(final Element element) {
        // Ensure gates come with an ID
        if (!element.hasAttribute("id")) {
            Utils.log(Level.SEVERE, "Could not find gate ID!");
            return;
        }

        final String id = element.getAttribute("id");
        final Location start, end;

        // Parse the start location
        final Element startElement = XMLParser.getChildElement(element, "start");
        if (startElement == null) {
            Utils.log(Level.SEVERE, "Could not find gate start location!");
            return;
        }
        start = XMLParser.parseLocation(startElement, this.world);
        if (start == null) {
            return;
        }

        // Parse the end location
        final Element endElement = XMLParser.getChildElement(element, "end");
        if (endElement == null) {
            Utils.log(Level.SEVERE, "Could not find gate end location!");
            return;
        }

        end = XMLParser.parseLocation(endElement, this.world);
        if (end == null) {
            return;
        }

        // Parse the material used to regenerate the gate
        final Material material = XMLParser.parseMaterial(XMLParser.getChildElement(element, "material"));
        if (material == null) {
            return;
        }

        if (!material.isBlock()) {
            Utils.log(Level.SEVERE, "Gate material is not a valid block type: " + material.toString());
            return;
        }

        this.gates.add(new DungeonGate(id, start, end, material));
    }

    /**
     * Parses a mob element
     * @param element The element to parse
     * @return The corresponding dungeon mob or null if it was invalid
     */
    private DungeonMob parseMob(final Element element) {
        // Check if this mob is a template mob
        if (element.hasAttribute("template")) {
            final Set<DungeonMob> mobs = this.parseTemplateMob(element);
            if (mobs != null) {
                this.mobs.addAll(mobs);
            }

            return null;
        }

        final DungeonMob mob = new DungeonMob();
        this.parseMobAttributes(element, mob);

        // Parse the mob nodes
        for (final Element child : XMLParser.getChildElements(element)) {
            switch (child.getNodeName()) {
                case "type":
                    final String content = child.getTextContent();

                    try {
                        mob.setType(EntityType.valueOf(XMLParser.parseConstant(content)));
                    } catch (final Exception ex) {
                        Utils.log(Level.SEVERE, "Could not parse mob type: " + content);
                        return null;
                    }
                    break;
                case "data":
                    final Map<String, Object> data = this.parseMobData(child);
                    if (data == null) {
                        return null;
                    }

                    mob.setData(data);
                    break;
                case "location":
                    final Location location = XMLParser.parseLocation(child, this.world);
                    if (location == null) {
                        Utils.log(Level.SEVERE, "Could not parse mob location: " + child.getTextContent());
                        return null;
                    }

                    mob.setLocation(location);
                    break;
                case "equipment":
                    final ItemStack[] equipment = this.parseEquipment(child);
                    if (equipment == null) {
                        return null;
                    }

                    // Set the armor items
                    mob.setEquipment(new ItemStack[] {
                        equipment[0],
                        equipment[1],
                        equipment[2],
                        equipment[3]
                    });

                    // Set the hand items
                    mob.setMainHand(equipment[4]);
                    mob.setOffHand(equipment[5]);
                    break;
                case "drops":
                    // Parse if default drops should be included
                    boolean defaults = true;
                    if (child.hasAttribute("defaults")) {
                        final String defaultsContent = child.getAttribute("defaults");

                        try {
                            defaults = Boolean.parseBoolean(defaultsContent);
                        } catch (final Exception ex) {
                            Utils.log(Level.SEVERE, "Could not parse include default drops: " + defaultsContent);
                            return null;
                        }
                    }
                    mob.setIncludeDefaultDrops(defaults);

                    // Parse the list of custom drops
                    final Map<Double, Set<ItemStack>> drops = this.parseDrops(child);
                    if (drops == null) {
                        return null;
                    }
                    mob.setDrops(drops);

                    break;
                case "effects":
                    final Set<PotionEffect> effects = new HashSet<>();

                    // Parse each potion effect
                    for (final Element effectElement : XMLParser.getChildElements(child)) {
                        final PotionEffect effect = XMLParser.parsePotionEffect(effectElement);
                        if (effect == null) {
                            return null;
                        }

                        effects.add(effect);
                    }

                    mob.setEffects(effects);
                    break;
            }
        }

        return mob;
    }

    /**
     * Parses mob element attributes
     * @param element The element to parse
     * @param mob The mob who's attributes should be modified
     */
    private void parseMobAttributes(final Element element, final DungeonMob mob) {
        if (element.hasAttribute("name")) {
            mob.setName(element.getAttribute("name"));
        }

        if (element.hasAttribute("name-display")) {
            final String content = element.getAttribute("name-display");

            try {
                mob.setNameDisplay(Boolean.parseBoolean(content));
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob name display: " + content);
                return;
            }
        }

        if (element.hasAttribute("proximity")) {
            final String content = element.getAttribute("proximity");

            try {
                mob.setProximity(Integer.parseInt(content));
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob proximity: " + content);
                return;
            }
        }

        if (element.hasAttribute("ai-proximity")) {
            final String content = element.getAttribute("ai-proximity");

            try {
                mob.setAIProximity(Integer.parseInt(content));
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob AI proximity: " + content);
                return;
            }
        }

        if (element.hasAttribute("count")) {
            final String content = element.getAttribute("count");

            try {
                mob.setCount(Integer.parseInt(content));
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob count: " + content);
                return;
            }
        }

        if (element.hasAttribute("interval")) {
            mob.setInterval(XMLParser.parseDuration(element.getAttribute("interval")));
        }

        final Set<String> tags = new HashSet<>();
        if (element.hasAttribute("required")) {
            final String content = element.getAttribute("required");

            try {
                if (Boolean.parseBoolean(content)) {
                    tags.add("required-kill");
                }
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse required tag: " + content);
                return;
            }
        }

        if (element.hasAttribute("gate")) {
            tags.add(element.getAttribute("gate"));
        }

        mob.setTags(tags);
    }

    /**
     * Parses a mob data list
     * @param element The element to parse
     * @return A map of all data pairs
     */
    private Map<String, Object> parseMobData(final Element element) {
        final Map<String, Object> data = new HashMap<>();

        for (final Element child : XMLParser.getChildElements(element)) {
            final String name = child.getNodeName();
            final String content = child.getTextContent();

            try {
                switch (name) {
                    case "age":
                        if (content.equalsIgnoreCase("adult")) {
                           data.put("age", true);
                        } else if (content.equalsIgnoreCase("baby")) {
                            data.put("age", false);
                        } else {
                            throw new Exception();
                        }
                        break;
                    case "villager-profession":
                        data.put(name, Villager.Profession.valueOf(XMLParser.parseConstant(content)));
                        break;
                    case "villager-type":
                        data.put(name, Villager.Type.valueOf(XMLParser.parseConstant(content)));
                        break;
                    case "slime-size":
                        data.put(name, Integer.parseInt(content));
                        break;
                }
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob data '" + name + "': " + content);
                return null;
            }
        }

        return data;
    }

    /**
     * Parses mob templates
     * @param element The element to parse
     */
    private void parseMobTemplates(final Element element) {
        for (final Element template : XMLParser.getChildElements(element)) {
            if (!template.hasAttribute("id")) {
                Utils.log(Level.SEVERE, "Mob template does not have an ID attribute!");
                return;
            }

            // Get the mob element of this template
            Element mobElement = null;
            for (final Element children : XMLParser.getChildElements(template)) {
                mobElement = children;
                break;
            }

            if (mobElement == null) {
                Utils.log(Level.SEVERE, "Mob template element could not be found!");
                return;
            }

            this.mobTemplates.put(template.getAttribute("id"), this.parseMob(mobElement));
        }
    }

    /**
     * Parses a dungeon mob group element
     * @param element The element to parse
     * @return The dungeon group or null if it could not be created
     */
    private DungeonGroup parseMobGroup(final Element element) {
        final DungeonGroup group = new DungeonGroup();

        // Get the spawn proximity
        int proximity = -1;
        if (element.hasAttribute("proximity")) {
            final String content = element.getAttribute("proximity");

            try {
                proximity = Integer.parseInt(content);
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob group proximity: " + content);
                return null;
            }
        }
        group.setProximity(proximity);

        // Get the AI proximity
        int aiProximity = -1;
        if (element.hasAttribute("ai-proximity")) {
            final String content = element.getAttribute("ai-proximity");

            try {
                aiProximity = Integer.parseInt(content);
            } catch (final Exception ex) {
                Utils.log(Level.SEVERE, "Could not parse mob group AI proximity: " + content);
                return null;
            }
        }
        group.setAIProximity(aiProximity);

        Location trigger = null;
        final Set<Set<DungeonMob>> mobs = new HashSet<>();

        // Search the child elements
        for (final Element child : XMLParser.getChildElements(element)) {
            switch (child.getNodeName()) {
                case "trigger":
                    // Set the trigger location for the proximities
                    trigger = XMLParser.parseLocation(child, this.world);
                    break;
                case "mob":
                    // Parse each dungeon mob in the group
                    final Set<DungeonMob> template = this.parseTemplateMob(child);
                    if (template == null) {
                        Utils.log(Level.SEVERE, "Could not find or parse a dungeon mob template!");
                        return null;
                    }

                    mobs.add(template);
                    break;
            }
        }

        if (trigger == null) {
            Utils.log(Level.SEVERE, "Could not find or parse a trigger location!");
        }

        group.setTrigger(trigger);
        group.setMobs(mobs);

        return group;
    }

    /**
     * Parses a template mob
     * @param element The element to parse
     * @return A list of dungeon mobs the template applies to
     */
    private Set<DungeonMob> parseTemplateMob(final Element element) {
        final String content = element.getAttribute("template");

        // Get the template for this mob
        final DungeonMob template = this.mobTemplates.getOrDefault(content, null);
        if (template == null) {
            Utils.log(Level.SEVERE, "Could not find mob template: " + content);
            return null;
        }

        final Set<DungeonMob> mobs = new HashSet<>();
        for (final Element child : XMLParser.getChildElements(element)) {
            final Location location = XMLParser.parseLocation(child, this.world);
            if (location == null) {
                continue;
            }

            // Override the mob template defaults
            final DungeonMob mob = new DungeonMob(template);
            this.parseMobAttributes(element, mob);
            mob.setLocation(location);

            mobs.add(mob);
        }

        return mobs;
    }

    /**
     * Parses mob equipment
     * @param element The element to parse
     * @return An array of item stacks containing the equipment (0-3 for armor, 4 for main hand, 5 for off hand)
     */
    private ItemStack[] parseEquipment(final Element element) {
        final ItemStack[] equipment = new ItemStack[6];

        for (final Element child : XMLParser.getChildElements(element)) {
            ItemStack item = null;

            // Check if the item is only a material
            if (XMLParser.getChildElements(child).size() == 0) {
                item = XMLParser.parseItem(child, Main.getRandom());
            } else {
                for (final Element realChild : XMLParser.getChildElements(child)) {
                    item = XMLParser.parseItem(realChild, Main.getRandom());
                    break;
                }
            }

            if (item == null) {
                return null;
            }

            switch (child.getNodeName()) {
                case "boots":
                    equipment[0] = item;
                    break;
                case "leggings":
                    equipment[1] = item;
                    break;
                case "chestplate":
                    equipment[2] = item;
                    break;
                case "helmet":
                    equipment[3] = item;
                    break;
                case "main-hand":
                    equipment[4] = item;
                    break;
                case "off-hand":
                    equipment[5] = item;
                    break;
            }
        }

        return equipment;
    }

    /**
     * Parses a custom drop element
     * @param element The element to parse
     * @return The list of items to drop with its corresponding chance
     */
    private Map<Double, Set<ItemStack>> parseDrops(final Element element) {
        final Map<Double, Set<ItemStack>> drops = new HashMap<>();

        for (final Element child : XMLParser.getChildElements(element)) {
            // Parse the chance for this drop
            final double chance;
            if (child.hasAttribute("chance")) {
                final String content = child.getAttribute("chance");

                try {
                    chance = Double.parseDouble(content);
                } catch (final Exception ex) {
                    Utils.log(Level.SEVERE, "Could not parse mob drop chance: " + content);
                    return null;
                }
            } else {
                Utils.log(Level.SEVERE, "Could not find mob drop chance!");
                return null;
            }

            // Parse the list of items to drop
            final Set<ItemStack> items = new HashSet<>();
            for (final Element itemElement : XMLParser.getChildElements(child)) {
                final ItemStack item = XMLParser.parseItem(itemElement, Main.getRandom());
                if (item == null) {
                    return null;
                }

                items.add(item);
            }

            drops.put(chance, items);
        }

        return drops;
    }

    /**
     * Parses all lootable elements
     * @param element The element to parse
     */
    private void parseLootables(final Element element) {
        // Get the list of blocks this lootable fills
        final Element blocks = XMLParser.getChildElement(element, "blocks");
        if (blocks == null) {
            Utils.log(Level.SEVERE, "Lootable did not have any block locations set!");
            return;
        }

        for (final Element block : XMLParser.getChildElements(blocks)) {
            final Element contents = XMLParser.getChildElement(element, "contents");
            if (contents == null) {
                Utils.log(Level.SEVERE, "Could not find dungeon loot contents element!");
                return;
            }

            // Get the global roll number (each lootable may override this)
            final int globalRolls = this.parseLootRolls(contents);
            if (globalRolls == -1) {
                return;
            }

            for (final Element content : XMLParser.getChildElements(contents)) {
                final DungeonLoot loot = new DungeonLoot();

                final Location location = XMLParser.parseLocation(block, this.world);
                if (location == null) {
                    return;
                }
                loot.setInventory(location);

                // Check if the global rolls should be overridden
                if (content.hasAttribute("rolls")) {
                    final int rolls = this.parseLootRolls(content);
                    if (rolls == -1) {
                        return;
                    }

                    loot.setRolls(rolls);
                } else {
                    loot.setRolls(globalRolls);
                }

                if (!content.hasAttribute("chance")) {
                    Utils.log(Level.SEVERE, "Could not find content chance!");
                    return;
                }

                if (content.hasAttribute("spread")) {
                    final String spreadContent = content.getAttribute("spread");

                    try {
                        loot.setSpread(Boolean.parseBoolean(spreadContent));
                    } catch (final Exception ex) {
                        Utils.log(Level.SEVERE, "Could not parse content spread: " + spreadContent);
                        return;
                    }
                }

                final String chanceContent = content.getAttribute("chance");
                try {
                    loot.setChance(Double.parseDouble(chanceContent));
                } catch (final Exception ex) {
                    Utils.log(Level.SEVERE, "Could not parse content chance: " + chanceContent);
                    return;
                }

                // Parse each item this lootable handles
                final Set<ItemStack> items = new HashSet<>();
                for (final Element item : XMLParser.getChildElements(content)) {
                    final ItemStack result;
                    if (item.getTagName().equalsIgnoreCase("potion")) {
                        result = XMLParser.parsePotion(item);
                    } else {
                        result = XMLParser.parseItem(item, Main.getRandom());
                    }

                    if (result == null) {
                        return;
                    }

                    items.add(result);
                }
                loot.setItems(items);

                this.loot.add(loot);
            }
        }
    }

    /**
     * Parses loot rolls on an element
     * @param element The element to parse
     * @return The number of times to roll the lootable
     */
    private int parseLootRolls(final Element element) {
        int rolls = 1;
        if (element.hasAttribute("rolls")) {
            final String content = element.getAttribute("rolls");

            final Pair<Double, Double> range = XMLParser.parseRange(content);
            if (range == null) {
                Utils.log(Level.SEVERE, "Could not parse content rolls: " + content);
                return -1;
            }

            // Get a random integer value between the min (key) and max (value)
            rolls = (int) (range.getKey() + Main.getRandom().nextInt((int) (range.getValue() - range.getKey()) + 1));
        }

        return rolls;
    }

    /** @return If time locking is enabled */
    public boolean getTimeLock() {
        return this.timeLock;
    }

    /** @return If weather is enabled */
    public boolean getAllowWeather() {
        return this.allowWeather;
    }

    /** @return The number of players allowed in this dungeon */
    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    /** @return A list of all spawn locations */
    public Set<Location> getSpawnLocations() {
        return this.spawnLocations;
    }

    /** @return A list of all ending locations */
    public Set<Location> getEndingLocations() {
        return this.endingLocations;
    }

    /** @return A list of all dungeon gates */
    public Set<DungeonGate> getGates() {
        return this.gates;
    }

    /** @return A list of all dungeon mobs */
    public Set<DungeonMob> getMobs() {
        return this.mobs;
    }

    /** @return A list of all dungeon mob groups */
    public Set<DungeonGroup> getMobGroups() {
        return this.mobGroups;
    }

    /** @return A list of all dungeon mobs including groups */
    public Set<DungeonMob> getAllMobs() {
        final Set<DungeonMob> mobs = this.mobs;

        for (final DungeonGroup group : this.mobGroups) {
            mobs.addAll(group.getMobs());
        }

        return mobs;
    }

    /** @return A list of all lootables */
    public Set<DungeonLoot> getLoot() {
        return this.loot;
    }
}
