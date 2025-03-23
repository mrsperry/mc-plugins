package com.mrjoshuasperry.pocketplugins.modules.timeplayed;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/** @author mrsperry */
public class TimePlayed extends Module implements BasicCommand {
  protected Map<UUID, Long> timePlayed;

  public TimePlayed(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);
    this.timePlayed = new HashMap<>();

    ConfigurationSection savedPlayers = writableConfig.getConfigurationSection("players");
    if (savedPlayers == null) {
      return;
    }

    for (String key : savedPlayers.getKeys(false)) {
      UUID uuid = UUID.fromString(key);
      long time = writableConfig.getLong("players." + key + ".time", 0);

      this.timePlayed.put(uuid, time);
    }

    this.registerBasicCommand("timeplayed", this);
    this.startTimer();
  }

  @Override
  public void execute(CommandSourceStack commandSourceStack, String[] args) {
    CommandSender sender = commandSourceStack.getSender();

    if (!(sender instanceof Player)) {
      sender.sendMessage(Component.text("You must be a player to use this command", NamedTextColor.RED));
      return;
    }

    if (args.length != 0) {
      sender.sendMessage(Component.text("Usage: /timeplayed", NamedTextColor.RED));
      return;
    }

    Player player = (Player) sender;
    long secondsPlayed = this.timePlayed.getOrDefault(player.getUniqueId(), 0L);
    long minutesPlayed = (long) Math.floor(secondsPlayed / 60);
    long hoursPlayed = (long) Math.floor(minutesPlayed / 60);
    long daysPlayed = (long) Math.floor(hoursPlayed / 24);
    double percentOfLife = (secondsPlayed / (20d * 365d * 24d * 60d * 60d)) * 100d;

    TextComponent.Builder message = Component.text()
        .content("═════")
        .color(NamedTextColor.DARK_GRAY)
        .append(Component.text(" Time Report ", NamedTextColor.GREEN))
        .append(Component.text("═════", NamedTextColor.DARK_GRAY))
        .append(Component.newline())
        .append(Component.text("You have played for ", NamedTextColor.GRAY))
        .append(Component.text(this.timeToString(secondsPlayed), NamedTextColor.GREEN))
        .append(Component.text(" " + this.pluralize(secondsPlayed, "second"), NamedTextColor.GRAY));

    if (minutesPlayed > 0) {
      message
          .append(Component.newline())
          .append(Component.newline())
          .append(Component.text("That translates to ", NamedTextColor.GRAY))
          .append(Component.text(this.timeToString(minutesPlayed), NamedTextColor.GREEN))
          .append(Component.text(" " + this.pluralize(minutesPlayed, "minute"), NamedTextColor.GRAY));
    }

    if (hoursPlayed > 0) {
      message
          .append(Component.newline())
          .append(Component.text("    ... or ", NamedTextColor.GRAY))
          .append(Component.text(this.timeToString(hoursPlayed), NamedTextColor.GREEN))
          .append(Component.text(" " + this.pluralize(hoursPlayed, "hour"), NamedTextColor.GRAY));
    }

    if (daysPlayed > 0) {
      message
          .append(Component.newline())
          .append(Component.text("    ... or ", NamedTextColor.GRAY))
          .append(Component.text(this.timeToString(daysPlayed), NamedTextColor.GREEN))
          .append(Component.text(" " + this.pluralize(daysPlayed, "day"), NamedTextColor.GRAY));
    }

    message
        .append(Component.newline())
        .append(Component.newline())
        .append(Component.text("If you were 20 years old, you would have played for ", NamedTextColor.GRAY))
        .append(Component.text(new DecimalFormat("#.###").format(percentOfLife) + "%", NamedTextColor.GREEN))
        .append(Component.text(" of your life!", NamedTextColor.GRAY));

    sender.sendMessage(message.build());
  }

  protected String timeToString(long time) {
    return new DecimalFormat("#,###").format(time);
  }

  protected String pluralize(double time, String unit) {
    boolean plural = time != 1;
    return unit + (plural ? "s" : "");
  }

  protected void startTimer() {
    Bukkit.getScheduler().runTaskTimer(this.getPlugin(), () -> {
      for (Player player : Bukkit.getOnlinePlayers()) {
        UUID uuid = player.getUniqueId();
        long secondsPlayed = this.timePlayed.getOrDefault(uuid, 0L) + 1;

        this.timePlayed.put(uuid, secondsPlayed);

        String basePath = "players." + uuid.toString();
        ConfigurationSection config = this.getWritableConfig();
        config.set(basePath + ".name", player.getName());
        config.set(basePath + ".time", secondsPlayed);
      }

      this.saveConfig();
    }, 0, 20);
  }
}
