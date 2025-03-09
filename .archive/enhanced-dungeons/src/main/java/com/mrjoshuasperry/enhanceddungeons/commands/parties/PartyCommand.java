package com.mrjoshuasperry.enhanceddungeons.commands.parties;

import com.google.common.collect.Lists;
import com.mrjoshuasperry.enhanceddungeons.commands.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PartyCommand {
    public static void onCommand(final CommandSender sender, final String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;

            if (args.length == 1) {
                Commands.tooFewArguments(sender, "dungeon party [list | create | invite | leave]");
            } else if (args.length >= 2) {
                // Delegate the command
                switch (args[1]) {
                    case "members":
                    case "list":
                        PartyList.onCommand(player, args);
                        break;
                    case "create":
                        PartyCreate.onCommand(player, args);
                        break;
                    case "invite":
                        PartyInvite.onCommand(player, args);
                        break;
                    case "leave":
                        PartyLeave.onCommand(player, args);
                        break;
                    case "accept":
                        PartyAccept.onCommand(player, args);
                        break;
                    default:
                        Commands.invalidArgument(sender, "dungeon party [list | create | invite | leave]", args[1]);
                        break;
                }
            }
        } else {
            Commands.mustBePlayer(sender);
        }
    }

    public static List<String> onTabComplete() {
        return Lists.newArrayList("list", "create", "invite", "leave", "accept");
    }
}
