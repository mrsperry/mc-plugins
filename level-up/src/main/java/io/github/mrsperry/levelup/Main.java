package io.github.mrsperry.levelup;

import com.google.common.collect.Lists;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {
    private final HashSet<Integer> levels = new HashSet<>();
    private final HashSet<Color> colors = new HashSet<>();
    private final Random random = new Random();

    private final ArrayList<FireworkEffect.Type> starTypes = Lists.newArrayList(
        FireworkEffect.Type.BALL,
        FireworkEffect.Type.BALL_LARGE,
        FireworkEffect.Type.BURST,
        FireworkEffect.Type.STAR
    );

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);

        if (this.getConfig().isSet("levels")) {
            for (String level : this.getConfig().getStringList("levels")) {
                try {
                    this.levels.add(Integer.parseInt(level));
                } catch (NumberFormatException ex) {
                    this.getLogger().severe("Invalid number format: " + level);
                }
            }
        }
        if (this.getConfig().isSet("colors")) {
            for (String color : this.getConfig().getStringList("colors")) {
                this.colors.add(getColor(color));
            }
        }
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        if (this.levels.contains(event.getNewLevel())) {
            if (event.getOldLevel() < event.getNewLevel()) {
                if (!this.colors.isEmpty()) {
                    Location location = event.getPlayer().getLocation();
                    World world = location.getWorld();
                    if (world == null) {
                        return;
                    }

                    Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.setPower(1);
                    meta.addEffects(FireworkEffect.builder()
                            .with(this.starTypes.get(this.random.nextInt(this.starTypes.size())))
                            .flicker(true)
                            .trail(true)
                            .withColor(getColorFromSet())
                            .withFade(getColorFromSet())
                            .build());
                    firework.setFireworkMeta(meta);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.FIREWORK) {
            event.setCancelled(true);
        }
    }

    private Color getColorFromSet() {
        return (Color) this.colors.toArray()[this.random.nextInt(this.colors.size())];
    }

    private Color getColor(String color) {
        try {
            if (color.equalsIgnoreCase("aqua")) {
                return Color.AQUA;
            } else if (color.equalsIgnoreCase("black")) {
                return Color.BLACK;
            } else if (color.equalsIgnoreCase("blue")) {
                return Color.BLUE;
            } else if (color.equalsIgnoreCase("fuchsia")) {
                return Color.FUCHSIA;
            } else if (color.equalsIgnoreCase("gray")) {
                return Color.GRAY;
            } else if (color.equalsIgnoreCase("green")) {
                return Color.GREEN;
            } else if (color.equalsIgnoreCase("lime")) {
                return Color.LIME;
            } else if (color.equalsIgnoreCase("maroon")) {
                return Color.MAROON;
            } else if (color.equalsIgnoreCase("navy")) {
                return Color.NAVY;
            } else if (color.equalsIgnoreCase("olive")) {
                return Color.OLIVE;
            } else if (color.equalsIgnoreCase("orange")) {
                return Color.ORANGE;
            } else if (color.equalsIgnoreCase("purple")) {
                return Color.PURPLE;
            } else if (color.equalsIgnoreCase("red")) {
                return Color.RED;
            } else if (color.equalsIgnoreCase("silver")) {
                return Color.SILVER;
            } else if (color.equalsIgnoreCase("teal")) {
                return Color.TEAL;
            } else if (color.equalsIgnoreCase("white")) {
                return Color.WHITE;
            } else if (color.equalsIgnoreCase("yellow")) {
                return Color.YELLOW;
            } else {
                throw new Exception();
            }
        } catch (Exception ex) {
            this.getLogger().severe("Invalid color name: " + color);
        }
        return Color.WHITE;
    }
}
