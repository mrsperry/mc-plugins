package com.mrjoshuasperry.enhanceddungeons.commands.dungeons;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.dungeons.DungeonHandler;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DungeonCreate {
    public static void onCommand(final CommandSender sender, final String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;

            if (args.length == 1) {
                Commands.tooFewArguments(sender, "dungeon create <id>");
            } else if (args.length == 2) {
                DungeonHandler.createDungeonInstance(args[1], PartyHandler.getOrCreateParty(player));
            } else {
                Commands.tooManyArguments(sender, "dungeon create <id>");
            }
        } else {
            Commands.mustBePlayer(sender);
        }
    }

    public static List<String> onTabComplete() {
        return new ArrayList<>(DungeonHandler.getDungeonIDs());
    }
}
