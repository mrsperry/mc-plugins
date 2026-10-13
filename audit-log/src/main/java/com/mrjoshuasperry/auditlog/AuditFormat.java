package com.mrjoshuasperry.auditlog;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Turns a record into one JSON line and works out whose files it belongs in. Items
 * are written in /give syntax, e.g. {@code minecraft:diamond_sword[enchantments={sharpness:5}] 1},
 * so any of them can be recreated exactly by hand.
 */
final class AuditFormat {
    // Without disableHtmlEscaping every = in an item's components would read =
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    private AuditFormat() {
    }

    static String line(ZonedDateTime time, String plugin, String event, Map<String, ?> fields) {
        JsonObject object = new JsonObject();
        object.addProperty("time", time.truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        object.addProperty("plugin", plugin);
        object.addProperty("event", event);
        fields.forEach((key, value) -> object.add(key, toJson(value)));
        return GSON.toJson(object);
    }

    /**
     * The files a record goes in, relative to its day's folder: one per player among
     * the values (including inside lists), or the server's file when there are none.
     */
    static List<String> files(Map<String, ?> fields) {
        Map<UUID, OfflinePlayer> players = new LinkedHashMap<>();
        for (Object value : fields.values()) {
            collectPlayers(value, players);
        }

        if (players.isEmpty()) {
            return List.of("server.jsonl");
        }

        return players.values().stream()
                .map(player -> "players/" + (player.getName() == null ? player.getUniqueId() : player.getName())
                        + ".jsonl")
                .toList();
    }

    private static void collectPlayers(Object value, Map<UUID, OfflinePlayer> players) {
        if (value instanceof OfflinePlayer player) {
            players.putIfAbsent(player.getUniqueId(), player);
        } else if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                collectPlayers(element, players);
            }
        }
    }

    /**
     * Players and entities become objects with their name and UUID, locations an
     * object with the world and coordinates, items their /give string, and an item
     * array (an inventory's contents) an object of slot to item, leaving out empty
     * slots. Registry values such as entity types appear as their key, other enums in
     * lower case, and text components as plain text.
     */
    static JsonElement toJson(Object value) {
        return switch (value) {
            case null -> JsonNull.INSTANCE;
            case String text -> new JsonPrimitive(text);
            case Number number -> new JsonPrimitive(number);
            case Boolean flag -> new JsonPrimitive(flag);
            case OfflinePlayer player -> player(player);
            case Entity entity -> entity(entity);
            case Location location -> location(location);
            case ItemStack item -> isEmpty(item) ? JsonNull.INSTANCE : new JsonPrimitive(itemString(item));
            case ItemStack[] items -> slots(items, AuditFormat::itemString);
            case Collection<?> collection -> array(collection);
            case Map<?, ?> map -> object(map);
            case Component component -> new JsonPrimitive(PlainTextComponentSerializer.plainText().serialize(component));
            case Keyed keyed -> new JsonPrimitive(keyed.getKey().asString());
            case Enum<?> constant -> new JsonPrimitive(constant.name().toLowerCase(Locale.ROOT));
            default -> new JsonPrimitive(value.toString());
        };
    }

    static String itemString(ItemStack item) {
        String components = item.hasItemMeta() ? item.getItemMeta().getAsComponentString() : "[]";
        return giveString(item.getType().getKey().asString(), components, item.getAmount());
    }

    /** The item as /give takes it: its id, any changed components, then the count. */
    static String giveString(String id, String components, int amount) {
        return id + ("[]".equals(components) ? "" : components) + " " + amount;
    }

    private static boolean isEmpty(ItemStack item) {
        return item.getType().isAir() || item.getAmount() <= 0;
    }

    private static JsonObject player(OfflinePlayer player) {
        JsonObject object = new JsonObject();
        object.addProperty("name", player.getName());
        object.addProperty("uuid", player.getUniqueId().toString());
        return object;
    }

    private static JsonObject entity(Entity entity) {
        JsonObject object = new JsonObject();
        object.addProperty("type", entity.getType().getKey().asString());
        object.addProperty("uuid", entity.getUniqueId().toString());
        Component name = entity.customName();
        if (name != null) {
            object.addProperty("name", PlainTextComponentSerializer.plainText().serialize(name));
        }
        return object;
    }

    private static JsonObject location(Location location) {
        JsonObject object = new JsonObject();
        object.addProperty("world", location.getWorld() == null ? null : location.getWorld().getName());
        object.addProperty("x", round(location.getX()));
        object.addProperty("y", round(location.getY()));
        object.addProperty("z", round(location.getZ()));
        return object;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    static JsonObject slots(ItemStack[] items, Function<ItemStack, String> describe) {
        JsonObject object = new JsonObject();
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] != null && !isEmpty(items[slot])) {
                object.addProperty(Integer.toString(slot), describe.apply(items[slot]));
            }
        }
        return object;
    }

    private static JsonArray array(Collection<?> collection) {
        JsonArray array = new JsonArray();
        for (Object element : collection) {
            // An emptied stack in a list is noise, not a record of anything
            if (!(element instanceof ItemStack item && isEmpty(item))) {
                array.add(toJson(element));
            }
        }
        return array;
    }

    private static JsonObject object(Map<?, ?> map) {
        JsonObject object = new JsonObject();
        map.forEach((key, value) -> object.add(String.valueOf(key), toJson(value)));
        return object;
    }
}
