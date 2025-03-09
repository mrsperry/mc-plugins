package com.mrjoshuasperry.enhanceddungeons.commands.parties;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PartyAccept {
    public static void onCommand(final Player sender, final String[] args) {
        if (args.length > 2) {
            Commands.tooManyArguments(sender, "dungeon party accept");
            return;
        }

        final Party party = PartyHandler.getPartyByInvite(sender);
        if (party != null) {
            if (party.acceptInvite(sender)) {
                sender.sendMessage(ChatColor.GREEN + "Party successfully joined!");
            } else {
                sender.sendMessage(ChatColor.RED + "Your party invite has expired!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have an active party invite!");
        }
    }
}
