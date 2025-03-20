package com.mrjoshuasperry.pocketplugins.modules.timeplayed;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.mrjoshuasperry.pocketplugins.utils.Module;

public class TimePlayed extends Module {
  protected Map<UUID, Long> timePlayed;
  protected YamlConfiguration config;

  public TimePlayed() {
    super("TimePlayed");

    this.startTimer();
  }

  @Override
  public void initialize(YamlConfiguration config) {
    super.initialize(config);
    this.config = config;
  }

  public void onTimePlayedCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sender.sendMessage("You have played for " + " minutes.");
    } else {
      sender.sendMessage("Usage: /timeplayed");
    }
  }

  protected void saveConfig() {
  }

  protected void startTimer() {
    Bukkit.getScheduler().runTaskTimer(this.getPlugin(), () -> {
      for (Player player : Bukkit.getOnlinePlayers()) {
        UUID uuid = player.getUniqueId();
        long secondsPlayed = this.timePlayed.getOrDefault(uuid, 0L) + 1;

        this.timePlayed.put(uuid, secondsPlayed);
        this.saveConfig();
      }
    }, 0, 20);
  }
}
