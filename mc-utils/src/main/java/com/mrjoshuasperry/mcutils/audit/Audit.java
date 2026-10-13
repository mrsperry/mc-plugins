package com.mrjoshuasperry.mcutils.audit;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * Records something a plugin did that could cost a player their items, or anything
 * else worth being able to roll back by hand, in the AuditLog plugin's daily logs:
 *
 * <pre>
 * Audit.event(plugin, "pile-created").with("owner", player).at(center).with("items", stacks).record();
 * </pre>
 *
 * <p>
 * Each player among the values gets the record in their own file. Without AuditLog
 * installed this does nothing, so a plugin behaves the same either way; call
 * {@link #warnIfMissing} from onEnable to say so once. The calling plugin needs
 * {@code softdepend: [AuditLog]} in its plugin.yml, so AuditLog loads first and its
 * classes are visible.
 */
public final class Audit {
  private static final String PLUGIN_NAME = "AuditLog";

  private final Plugin source;
  private final String event;
  private final Map<String, Object> fields = new LinkedHashMap<>();

  private Audit(Plugin source, String event) {
    this.source = source;
    this.event = event;
  }

  /** Starts a record of {@code event}, a short kebab-case name such as "pile-created". */
  public static Audit event(Plugin source, String event) {
    return new Audit(source, event);
  }

  /**
   * Adds a value: text, a number or boolean, a player, entity, location or item, an
   * item array such as an inventory's contents (which keeps its slots), or a list or
   * map of those. Items are written in /give syntax, so they can be recreated exactly.
   */
  public Audit with(String key, Object value) {
    this.fields.put(key, value);
    return this;
  }

  public Audit at(Location location) {
    return this.with("location", location);
  }

  /**
   * Writes the record. Call it on the main thread, before anything else changes the
   * items: they're read immediately, so the record shows them as they were.
   */
  public void record() {
    if (isAvailable()) {
      AuditLogBridge.record(this.source, this.event, this.fields);
    }
  }

  public static boolean isAvailable() {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
    return plugin != null && plugin.isEnabled();
  }

  public static void warnIfMissing(Plugin source) {
    if (!isAvailable()) {
      source.getLogger().warning(PLUGIN_NAME + " is not installed, so nothing this plugin does with players' items is"
          + " being recorded");
    }
  }
}
