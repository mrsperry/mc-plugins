package com.mrjoshuasperry.enhanceddungeons.commands.parties;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;
import com.mrjoshuasperry.enhanceddungeons.parties.Party;
import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PartyInvite {
    public static void onCommand(final Player sender, final String[] args) {
        if (args.length == 2) {
            Commands.tooFewArguments(sender, "dungeon party invite <player>");
        } else if (args.length == 3) {
            final Party party = PartyHandler.getOrCreateParty(sender);

            // Only allow the owner to invite players
            if (party.getOwner() != sender) {
                sender.sendMessage(ChatColor.RED + "Only the party owner may invite other players!");
                return;
            }

            final Player player = Bukkit.getPlayer(args[2]);
            // Ensure the invited player is online
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Could not find player: " + args[2]);
                return;
            }

            // Ensure the invited player has no other active invites
            if (PartyHandler.getPartyByInvite(player) != null) {
                sender.sendMessage(ChatColor.RED + player.getName() + " already has an active invite to another party!");
                return;
            }

            // Try to invite the player to the party
            if (party.invite(player)) {
                sender.sendMessage(ChatColor.GREEN + "Successfully invited " + player.getName() + " to the party!");
            } else {
                if (party.getMembers().contains(player)) {
                    sender.sendMessage(ChatColor.RED + player.getName() + " is already in your party!");
                } else {
                    sender.sendMessage(ChatColor.RED + "You already have an active invite to " + player.getName() + "!");
                }
            }
        } else {
            Commands.tooManyArguments(sender, "dungeon party invite <player>");
        }
    }
}
