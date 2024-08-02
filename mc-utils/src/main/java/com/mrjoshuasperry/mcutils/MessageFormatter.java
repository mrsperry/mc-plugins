package com.mrjoshuasperry.mcutils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.md_5.bungee.api.ChatColor;

public class MessageFormatter {
    public static String addTimestamp() {
        return ChatColor.DARK_GRAY + "[" + ChatColor.GRAY
                + new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()) + ChatColor.DARK_GRAY + "]";
    }

    public static String addWorldName(String name) {
        return ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + name + ChatColor.DARK_GRAY + "]";
    }

    public static String addTimeAndWorld(String name) {
        return addTimestamp() + addWorldName(name);
    }
}
