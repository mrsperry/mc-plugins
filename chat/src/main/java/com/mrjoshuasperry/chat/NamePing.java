package com.mrjoshuasperry.chat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)\\s*");

    private final Map<String, Date> cooldowns;
    private int cooldown;

    public NamePing(YamlConfiguration config) {
        this.cooldowns = new HashMap<>();
        this.cooldown = config.getInt("ping-cooldown", 5);
    }

    /**
     * Extracts the mentioned names (the {@code @name} tokens) from a chat message, in
     * order of appearance. Package-private and static so the scan is unit-testable.
     */
    static List<String> parseMentions(String message) {
        List<String> names = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(message);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        String message = ((TextComponent) event.message()).content();

        for (String name : parseMentions(message)) {
            Player player = Bukkit.getPlayer(name);
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (this.cooldowns.containsKey(player.getName())) {
                Date expire = this.cooldowns.get(player.getName());
                // Skip only this mention while it is on cooldown; a `return` here used
                // to abandon every remaining mention in the same message.
                if (new Date().before(expire)) {
                    continue;
                }
            }

            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);

            Date expire = new Date();
            expire.setTime(expire.getTime() + (cooldown * 1000));
            this.cooldowns.put(player.getName(), expire);
        }
    }
}
