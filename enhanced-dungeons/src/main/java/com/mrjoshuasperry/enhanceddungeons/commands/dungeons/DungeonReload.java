package com.mrjoshuasperry.enhanceddungeons.commands.dungeons;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.dungeons.DungeonHandler;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class DungeonReload {
    public static void onCommand(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            Commands.tooFewArguments(sender, "dungeon reload <id | *>");
        } else if (args.length == 2) {
            if (args[1].equals("*")) {
                DungeonHandler.reloadAllConfigs();
                sender.sendMessage(ChatColor.GREEN + "All dungeons configs reloaded");
            } else {
                if (DungeonHandler.reloadConfig(args[1])) {
                    sender.sendMessage(ChatColor.GREEN + "Dungeon config reloaded");
                } else {
                    sender.sendMessage(ChatColor.RED + "Dungeon could not be found: " + args[1]);
                }
            }
        } else {
            Commands.tooManyArguments(sender, "dungeon reload <id | *>");
        }
    }

    public static List<String> onTabComplete() {
        final List<String> ids = new ArrayList<>(DungeonHandler.getDungeonIDs());
        ids.add("*");

        return ids;
    }
}
