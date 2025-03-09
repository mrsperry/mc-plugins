package com.mrjoshuasperry.enhanceddungeons.commands.dungeons;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.dungeons.DungeonHandler;
import com.mrjoshuasperry.enhanceddungeons.dungeons.DungeonInstance;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class DungeonEnd {
    public static void onCommand(final CommandSender sender, final String[] args) {
        if (args.length == 1) {
            Commands.tooFewArguments(sender, "dungeon stop <id>");
        } else if (args.length == 2) {
            final DungeonInstance instance = DungeonHandler.getDungeonInstance(args[1]);
            if (instance == null) {
                sender.sendMessage(ChatColor.RED + "There is no dungeon instance with the ID: " + args[1]);
            } else {
                instance.end(true);
                sender.sendMessage(ChatColor.GREEN + "Dungeon instance successfully ended");
            }
        } else {
            Commands.tooManyArguments(sender, "dungeon stop <id>");
        }
    }

    public static List<String> onTabComplete() {
        return new ArrayList<>(DungeonHandler.getDungeonIDs());
    }
}
