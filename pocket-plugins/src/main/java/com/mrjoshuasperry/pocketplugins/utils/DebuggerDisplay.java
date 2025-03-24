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
    private static final Map<Entity, TextDisplay> displays = new HashMap<>();

    public DebuggerDisplay() {
        Bukkit.getScheduler().runTaskTimer(PocketPlugins.getInstance(), () -> {
            cleanup();
            for (Entity entity : DebuggerDisplay.displays.keySet()) {
                TextDisplay display = DebuggerDisplay.displays.get(entity);
                display.teleport(entity.getLocation().add(0, 1.5, 0));
            }
        }, 0, 1);
    }

    public void updateDisplay(Entity entity, boolean concat, String... lines) {
        String text = String.join("\n", lines);
        TextDisplay display = DebuggerDisplay.displays.get(entity);

        if (display == null || !display.isValid()) {
            display = entity.getWorld().spawn(entity.getLocation().add(0, 2.5, 0), TextDisplay.class, td -> {
                td.text(Component.text(text));
                td.setAlignment(TextAlignment.CENTER);
                td.setBillboard(Billboard.CENTER);
                td.setBackgroundColor(Color.BLACK);
            });
            DebuggerDisplay.displays.put(entity, display);
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
        DebuggerDisplay.displays.entrySet().removeIf(entry -> {
            if (!entry.getKey().isValid() || !entry.getValue().isValid()) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
    }

    public void removeEntity(Entity entity) {
        DebuggerDisplay.displays.remove(entity);
    }

    public static void removeAll() {
        DebuggerDisplay.displays.values().forEach(TextDisplay::remove);
        DebuggerDisplay.displays.clear();
    }
}