package com.mrjoshuasperry.pocketplugins.utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.TextDisplay.TextAlignment;

import com.mrjoshuasperry.pocketplugins.PocketPlugins;

import net.kyori.adventure.text.Component;

public class DebuggerDisplay {
    private static DebuggerDisplay self;
    private final Map<Entity, TextDisplay> displays = new HashMap<>();

    private DebuggerDisplay() {
        Bukkit.getScheduler().runTaskTimer(PocketPlugins.getInstance(), () -> {
            cleanup();
            for (Entity entity : this.displays.keySet()) {
                TextDisplay display = this.displays.get(entity);
                display.teleport(entity.getLocation().add(0, 1.5, 0));
            }
        }, 0, 1);
    }

    public static DebuggerDisplay getInstance() {
        if (self == null) {
            self = new DebuggerDisplay();
        }
        return self;
    }

    public void updateDisplay(Entity entity, boolean concat, String... lines) {
        String text = String.join("\n", lines);
        TextDisplay display = displays.get(entity);

        if (display == null || !display.isValid()) {
            display = entity.getWorld().spawn(entity.getLocation().add(0, 2.5, 0), TextDisplay.class, td -> {
                td.text(Component.text(text));
                td.setAlignment(TextAlignment.CENTER);
                td.setBillboard(Billboard.CENTER);
                td.setBackgroundColor(Color.BLACK);
            });
            displays.put(entity, display);
        } else {
            if (concat) {
                Component component = display.text();
                component.append(Component.text(text));

                display.text(component);
            } else {
                display.text(Component.text(text));
            }
        }
    }

    public void cleanup() {
        displays.entrySet().removeIf(entry -> {
            if (!entry.getKey().isValid() || !entry.getValue().isValid()) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    public void removeEntity(Entity entity) {
        this.displays.remove(entity);
    }

    public void removeAll() {
        displays.values().forEach(TextDisplay::remove);
        displays.clear();
    }
}