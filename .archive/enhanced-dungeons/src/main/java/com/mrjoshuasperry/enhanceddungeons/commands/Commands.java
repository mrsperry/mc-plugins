package com.mrjoshuasperry.enhanceddungeons.commands;

import com.google.common.collect.Lists;

import com.mrjoshuasperry.enhanceddungeons.Utils;
import com.mrjoshuasperry.enhanceddungeons.commands.dungeons.DungeonCreate;
import com.mrjoshuasperry.enhanceddungeons.commands.dungeons.DungeonEnd;
import com.mrjoshuasperry.enhanceddungeons.commands.dungeons.DungeonReload;
import com.mrjoshuasperry.enhanceddungeons.commands.parties.PartyCommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.logging.Level;

public class Commands implements TabExecutor {
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String line, final String[] args) {
        // Ensure the correct command is being handled
        final String name = command.getName();
        if (!name.equalsIgnoreCase("dungeon")) {
            Utils.log(Level.WARNING, "Invalid main command found: " + name);
            return false;
        }

        // Ensure there is a type argument to delegate
        if (args.length >= 1) {
            switch (args[0]) {
                case "party":
                    PartyCommand.onCommand(sender, args);
                    break;
                case "create":
                case "start":
                    DungeonCreate.onCommand(sender, args);
                    break;
                case "end":
                case "stop":
                    DungeonEnd.onCommand(sender, args);
                    break;
                case "reload":
                    DungeonReload.onCommand(sender, args);
                    break;
                default:
                    Commands.invalidArgument(sender, "dungeon <party | create | end | reload>", args[0]);
                    break;
            }
        } else {
            Commands.usage(sender, "dungeon <party | create | end | reload>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String line, final String[] args) {
        final String name = command.getName();
        if (!name.equalsIgnoreCase("dungeon")) {
            return null;
        }

        switch (args[0]) {
            case "party":
                return PartyCommand.onTabComplete();
            case "create":
            case "start":
                return DungeonCreate.onTabComplete();
            case "end":
            case "stop":
                return DungeonEnd.onTabComplete();
            case "reload":
                return DungeonReload.onTabComplete();
            default:
                return Lists.newArrayList("party", "create", "end", "reload");
        }
    }

    /**
     * Sent when there are too few arguments for a command
     * @param sender The command sender
     * @param usage The proper command usage
     */
    public static void tooFewArguments(final CommandSender sender, final String usage) {
        sender.sendMessage(ChatColor.RED + "Too few arguments!");
        Commands.usage(sender, usage);
    }

    /**
     * Sent when there are too many arguments for a command
     * @param sender The command sender
     * @param usage The proper command usage
     */
    public static void tooManyArguments(final CommandSender sender, final String usage) {
        sender.sendMessage(ChatColor.RED + "Too many arguments!");
        Commands.usage(sender, usage);
    }

    /**
     * Sent when there is an invalid argument for a command
     * @param sender The command sender
     * @param usage The proper command usage
     * @param arg The invalid argument
     */
    public static void invalidArgument(final CommandSender sender, final String usage, final String arg) {
        sender.sendMessage(ChatColor.RED + "Invalid argument: " + arg);
        Commands.usage(sender, usage);
    }

    /**
     * Sent when the correct usage of a command needs to be displayed
     * @param sender The command sender
     * @param usage The proper command usage
     */
    public static void usage(final CommandSender sender, final String usage) {
        sender.sendMessage(ChatColor.RED + "Usage: /" + usage);
    }

    /**
     * Sent when a console sender tries to use a player only command
     * @param sender The command sender
     */
    public static void mustBePlayer(final CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
    }
}
