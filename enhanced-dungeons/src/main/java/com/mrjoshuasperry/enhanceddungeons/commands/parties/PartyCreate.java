package com.mrjoshuasperry.enhanceddungeons.commands.parties;

import com.mrjoshuasperry.enhanceddungeons.commands.Commands;

import com.mrjoshuasperry.enhanceddungeons.parties.PartyHandler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PartyCreate {
    public static void onCommand(final Player sender, final String[] args) {
        if (args.length > 2) {
            Commands.tooManyArguments(sender, "dungeon party create");
            return;
        }

        if (PartyHandler.createParty(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Party successfully created!");
        } else {
            sender.sendMessage(ChatColor.RED + "You are already in a party!");
        }
    }
}
