package com.mrjoshuasperry.levelup;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;

public class Main extends JavaPlugin implements Listener {
    private final Set<Integer> levels = new HashSet<>();
    private final Set<Color> colors = new HashSet<>();
    private final Random random = new Random();
    private final NamespacedKey levelUpKey = new NamespacedKey(this, "level-up");

    private final List<FireworkEffect.Type> starTypes = Lists.newArrayList(
            FireworkEffect.Type.BALL,
            FireworkEffect.Type.BALL_LARGE,
            FireworkEffect.Type.BURST,
            FireworkEffect.Type.STAR);

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);

        FileConfiguration config = this.getConfig();
        for (String level : config.getStringList("levels")) {
            try {
                this.levels.add(Integer.parseInt(level));
            } catch (NumberFormatException ex) {
                this.getLogger().severe("Invalid number format for level: " + level);
            }
        }

        for (Object color : config.getList("colors")) {
            if (!(color instanceof Map)) {
                continue;
            }

            Map<?, ?> colorMap = (Map<?, ?>) color;
            try {
                int alpha = Integer.parseInt((String) colorMap.get("alpha"));
                int red = Integer.parseInt((String) colorMap.get("red"));
                int green = Integer.parseInt((String) colorMap.get("green"));
                int blue = Integer.parseInt((String) colorMap.get("blue"));

                this.colors.add(Color.fromARGB(alpha, red, green, blue));
            } catch (ClassCastException ex) {
                this.getLogger().severe("Invalid color format: " + color);
            }
        }
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        if (!this.levels.contains(event.getNewLevel())) {
            return;
        }

        // Don't send a firework when spending levels
        if (event.getOldLevel() > event.getNewLevel()) {
            return;
        }

        if (this.colors.isEmpty()) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        World world = location.getWorld();

        Firework firework = (Firework) world.spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        meta.addEffects(FireworkEffect.builder()
                .with(this.starTypes.get(this.random.nextInt(this.starTypes.size())))
                .flicker(true)
                .trail(true)
                .withColor(getRandomColor())
                .withFade(getRandomColor())
                .build());
        firework.setFireworkMeta(meta);

        firework.getPersistentDataContainer().set(this.levelUpKey, PersistentDataType.BYTE,
                (byte) 1);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();

        if (entity.getType() != EntityType.FIREWORK_ROCKET) {
            return;
        }

        if (!entity.getPersistentDataContainer().has(this.levelUpKey, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
    }

    private Color getRandomColor() {
        return (Color) this.colors.toArray()[this.random.nextInt(this.colors.size())];
    }
}
