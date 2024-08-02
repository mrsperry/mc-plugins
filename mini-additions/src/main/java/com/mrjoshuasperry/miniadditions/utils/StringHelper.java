package com.mrjoshuasperry.miniadditions.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;

public class StringHelper {
    private static final List<ChatColor> rainbowColors = Arrays.asList(ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW,
            ChatColor.GREEN, ChatColor.BLUE, ChatColor.LIGHT_PURPLE);

    public static String rainbowfy(String str) {
        int index = 0;
        StringBuilder result = new StringBuilder();

        for (String s : str.split("")) {
            if (s.equals(" ")) {
                result.append(s);
            } else {

                result.append(rainbowColors.get(index)).append(s);
                index = (index + 1) % rainbowColors.size();
            }
        }

        return result.toString();
    }
}
