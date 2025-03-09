package com.mrjoshuasperry.enhanceddungeons.commands.parties;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PartyLeave {
    public static void onCommand(final Player sender, final String[] args) {
        if (args.length > 2) {
            Commands.tooManyArguments(sender, "dungeon party leave");
            return;
        }

        final Party party = PartyHandler.getPartyByMember(sender);
        // Make sure the player is currently in a party
        if (party == null) {
            sender.sendMessage(ChatColor.RED + "You are not currently in a party!");
            return;
        }

        // Remove the player from their party
        party.removeMember(sender);
        sender.sendMessage(ChatColor.GREEN + "You successfully left the party");
    }
}
