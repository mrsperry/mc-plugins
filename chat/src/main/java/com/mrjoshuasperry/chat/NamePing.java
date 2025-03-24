package com.mrjoshuasperry.chat;

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
import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;

public class NamePing implements Listener {
    private final Map<String, Date> cooldowns;
    private final Pattern pattern;
    private int cooldown;

    public NamePing(YamlConfiguration config) {
        this.cooldowns = new HashMap<>();
        this.pattern = Pattern.compile("@(\\w+)\\s*");
        this.cooldown = config.getInt("ping-cooldown", 5);
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        String message = ((TextComponent) event.message()).content();

        Matcher matcher = this.pattern.matcher(message);
        while (matcher.find()) {
            Player player = Bukkit.getPlayer(matcher.group(1));

            if (player == null) {
                continue;
            }

            if (!player.isOnline()) {
                continue;
            }

            if (this.cooldowns.containsKey(player.getName())) {
                Date expire = this.cooldowns.get(player.getName());

                if (new Date().before(expire)) {
                    return;
                }
            }

            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);

            Date expire = new Date();
            expire.setTime(expire.getTime() + (cooldown * 1000));
            this.cooldowns.put(player.getName(), expire);
        }
    }
}
