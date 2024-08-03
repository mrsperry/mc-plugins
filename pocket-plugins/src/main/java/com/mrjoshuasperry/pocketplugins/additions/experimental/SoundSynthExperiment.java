package com.mrjoshuasperry.pocketplugins.additions.experimental;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mrjoshuasperry.pocketplugins.MiniAdditions;

import io.github.mrsperry.mcutils.sound.SoundByte;
import io.github.mrsperry.mcutils.sound.SoundSynth;

public class SoundSynthExperiment implements CommandExecutor {
    private final Map<Player, SoundSynth> soundSynths;

    public SoundSynthExperiment() {
        soundSynths = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Only players can use this command");
            return true;
        }

        Player player = (Player) commandSender;

        if (!soundSynths.containsKey(player)) {
            this.soundSynths.put(player, new SoundSynth());
        }

        if (command.getName().equalsIgnoreCase("synth")) {
            if (args.length > 0) {
                String subCommand = args[0];
                args = Arrays.copyOfRange(args, 1, args.length);

                Bukkit.getLogger().info("[DEBUG] + sub? " + subCommand + " args? " + args.length);

                switch (subCommand.toLowerCase()) {
                    case "add":
                        add(player, args);
                        break;
                    case "remove":
                        remove(player, args);
                        break;
                    case "edit":
                        edit(player, args);
                        break;
                    case "list":
                        list(player);
                        break;
                    case "play":
                        play(player);
                        break;
                    default:
                        player.sendMessage(ChatColor.RED + "Unknown sub command " + subCommand);
                        player.sendMessage(ChatColor.RED + "/synth [add|remove|edit|list|play]");
                        return true;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void add(Player player, String[] args) {
        SoundSynth pSynth = soundSynths.get(player);
        if (args.length == 4) {
            try {
                pSynth.add(new SoundByte(Sound.valueOf(args[0].toUpperCase()), Float.parseFloat(args[1]),
                        Float.parseFloat(args[2]), Integer.parseInt(args[3])));
                player.sendMessage(ChatColor.GREEN + "Sound added!");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "/synth add [sound] [volume] [pitch] [delay]");
            }
        } else {
            player.sendMessage(ChatColor.RED + "/synth add [sound] [volume] [pitch] [delay]");
        }
    }

    private void remove(Player player, String[] args) {
        SoundSynth pSynth = soundSynths.get(player);
        if (args.length == 1) {
            try {
                pSynth.remove(Integer.parseInt(args[0]));
                player.sendMessage(ChatColor.GREEN + "Sound removed!");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "/synth remove [index]");
            }
        } else {
            player.sendMessage(ChatColor.RED + "/synth remove [index]");
        }
    }

    private void edit(Player player, String[] args) {
        SoundSynth pSynth = soundSynths.get(player);
        if (args.length == 5) {
            try {
                pSynth.edit(new SoundByte(Sound.valueOf(args[1].toUpperCase()), Float.parseFloat(args[2]),
                        Float.parseFloat(args[3]), Integer.parseInt(args[4])), Integer.parseInt(args[0]));
                player.sendMessage(ChatColor.GREEN + "Sound added!");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "/synth edit [index] [sound] [volume] [pitch] [delay]");
            }
        } else {
            player.sendMessage(ChatColor.RED + "/synth edit [index] [sound] [volume] [pitch] [delay]");
        }
    }

    private void list(Player player) {
        SoundSynth pSynth = soundSynths.get(player);
        pSynth.list(player);
    }

    private void play(Player player) {
        SoundSynth pSynth = soundSynths.get(player);
        pSynth.play(MiniAdditions.getInstance(), player.getLocation());
    }

}
