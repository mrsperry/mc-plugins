package com.mrjoshuasperry.customgeneration;

import com.mrjoshuasperry.customgeneration.generators.DesolationGenerator;

import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Main extends org.bukkit.plugin.java.JavaPlugin implements CommandExecutor {
    private static Random random;

    @Override
    public void onEnable() {
        random = new Random();

        getCommand("createworld").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String cmdLine, String[] args) {
        // placeholder
        if (command.getName().equalsIgnoreCase("createworld")) {
            WorldCreator creator = new WorldCreator("desolation/" + UUID.randomUUID());
            creator.generator(new DesolationGenerator());

            World world = Bukkit.createWorld(creator);
            world.setSpawnLocation(world.getHighestBlockAt(random.nextInt(400) - 200, random.nextInt(400) - 200).getLocation());

            ((Player) sender).teleport(world.getSpawnLocation());
        }
        return false;
    }

    public static Random getRandom() {
        return random;
    }
}
