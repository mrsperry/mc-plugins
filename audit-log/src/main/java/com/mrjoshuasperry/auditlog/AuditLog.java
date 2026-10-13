package com.mrjoshuasperry.auditlog;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Keeps a daily JSON Lines log per player of everything that could cost them their
 * items: joins, leaves, deaths and kills recorded here, plus whatever other plugins
 * record through {@link #record}. It never acts on what it logs; it only makes sure
 * there is a record to roll back from by hand.
 */
public class AuditLog extends JavaPlugin {
    private static AuditLog instance;

    private AuditWriter writer;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        this.writer = new AuditWriter(this.getDataFolder().toPath().resolve("logs"), this.getLogger());
        this.writer.deleteExpired(LocalDate.now(), Math.max(1, config.getInt("retention-days", 7)));

        this.getServer().getPluginManager().registerEvents(new BuiltInEvents(this, config), this);
        instance = this;
    }

    @Override
    public void onDisable() {
        instance = null;
        if (this.writer != null) {
            this.writer.close();
        }
    }

    /**
     * Records one event. Each player among the values gets the line in their own
     * file, and an event with no players goes to the server's file. Call it on the
     * main thread: the values are read immediately, so changing an item afterwards
     * doesn't change what was recorded.
     *
     * <p>
     * Other plugins should call mc-utils' {@code Audit} instead, which does nothing
     * when this plugin isn't installed rather than failing to load this class.
     *
     * @param fields written in order; {@link AuditFormat#toJson} lists how each type appears
     */
    public static void record(Plugin source, String event, Map<String, ?> fields) {
        AuditLog plugin = instance;
        if (plugin == null) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        String line = AuditFormat.line(now, source.getName(), event, fields);
        plugin.writer.write(now.toLocalDate(), AuditFormat.files(fields), line);
    }
}
