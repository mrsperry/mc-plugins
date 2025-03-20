package com.mrjoshuasperry.pocketplugins.modules.nameping;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;

public class NamePing extends Module {
    private final Map<String, Date> cooldowns;
    private final Pattern pattern;
    private int cooldown;

    public NamePing() {
        super("NamePing");
        cooldowns = new HashMap<>();
        pattern = Pattern.compile("@(\\w+)\\s*");
    }

    @Override
    public void initialize(YamlConfiguration config) {
        super.initialize(config);
        this.cooldown = config.getInt("ping-cooldown", 5);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        String message = ((TextComponent) event.message()).content();

        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            Player player = Bukkit.getPlayer(matcher.group(1));

            if (player != null && player.isOnline()) {
                if (cooldowns.containsKey(player.getName())) {
                    Date expire = cooldowns.get(player.getName());

                    if (new Date().before(expire)) {
                        return;
                    }
                }

                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);

                Date expire = new Date();
                expire.setTime(expire.getTime() + (cooldown * 1000));
                cooldowns.put(player.getName(), expire);
            }
        }
    }
}
