package com.mrjoshuasperry.enhanceddungeons.commands.parties;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PartyList {
    public static void onCommand(final Player sender, final String[] args) {
        if (args.length > 2) {
            Commands.tooManyArguments(sender, "dungeon party list");
            return;
        }

        final Party party = PartyHandler.getPartyByMember(sender);
        if (party != null) {
            sender.sendMessage(ChatColor.DARK_GRAY + "======= "
                    + ChatColor.AQUA + party.getOwner().getName() + ChatColor.GRAY + "'s Party"
                    + ChatColor.DARK_GRAY + " =======");

            for (final Player member : party.getMembers()) {
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + member.getDisplayName());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You are not currently in a party!");
        }
    }
}
