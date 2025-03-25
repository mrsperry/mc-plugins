package com.mrjoshuasperry.chat;

import java.time.Duration;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.mrjoshuasperry.mcutils.TextColors;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickCallback.Options;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class MessageHandler implements Listener {
  protected String messageDelimiter;
  protected TextColor delimiterColor;
  protected TextColor defaultNameColor;
  protected TextColor defaultMessageColor;
  protected TextColor consoleColor;
  protected TextColor highlightColor;

  public MessageHandler(YamlConfiguration config) {
    this.messageDelimiter = config.getString("message-delimiter", " »") + " ";
    this.delimiterColor = TextColors.parseTextColor(config.getString("delimiter-color"),
        TextColor.fromHexString("#cccccc"));
    this.defaultNameColor = TextColors.parseTextColor(config.getString("default-name-color"),
        TextColor.fromHexString("#6b6bff"));
    this.defaultMessageColor = TextColors.parseTextColor(config.getString("default-mesasge-color"),
        TextColor.fromHexString("#ededed"));
    this.consoleColor = TextColors.parseTextColor(config.getString("console-color"), NamedTextColor.RED);
    this.highlightColor = TextColors.parseTextColor(config.getString("highlight-color"), NamedTextColor.AQUA);
  }

  protected void sendFormattedMessage(Player sender, Component name, Component message) {
    this.sendFormattedMessage(sender, name, message, null, null, true);
  }

  protected void sendFormattedMessage(Player sender, Component name, Component message, Component prefix,
      Component suffix) {
    this.sendFormattedMessage(sender, name, message, prefix, suffix, true);
  }

  protected void sendFormattedMessage(Player sender, Component name, Component message, Component prefix,
      Component suffix, boolean includeDelimiter) {
    Component delimiter = Component.text(this.messageDelimiter, this.delimiterColor);

    if (sender != null) {
      message = message.replaceText(
          TextReplacementConfig.builder()
              .match(Pattern.compile("\\[\\[.+?\\]\\]"))
              .replacement((MatchResult result, TextComponent.Builder builder) -> {
                Component replacement = this.replaceText(sender, result, builder);
                if (replacement != null) {
                  return replacement;
                }

                return Component.text(result.group());
              })
              .build());

      name = name.clickEvent(ClickEvent.suggestCommand("/tell " + sender.getName() + " "));
    }

    TextComponent.Builder formattedMessage = Component.text();
    if (prefix != null) {
      formattedMessage
          .append(Component.space())
          .append(prefix);
    }

    formattedMessage.append(name.colorIfAbsent(this.defaultNameColor));

    if (suffix != null) {
      formattedMessage
          .append(Component.space())
          .append(suffix);
    }

    if (includeDelimiter) {
      formattedMessage.append(delimiter.colorIfAbsent(this.delimiterColor));
    }

    formattedMessage.append(message.colorIfAbsent(this.defaultMessageColor));

    Bukkit.getServer().sendMessage(formattedMessage.build());
  }

  protected Component replaceText(Player sender, MatchResult result, TextComponent.Builder builder) {
    String command = result
        .group()
        .replace("[[", "")
        .replace("]]", "");

    if (command.equalsIgnoreCase("coords")) {
      return this.sendPlayerCoordinates(sender);
    }

    try {
      EquipmentSlot slot = EquipmentSlot.valueOf(command.toUpperCase().replace(" ", "_"));
      return this.sendEquipmentSlot(sender, slot);
    } catch (Exception ex) {
      return null;
    }
  }

  protected Component sendEquipmentSlot(Player sender, EquipmentSlot slot) {
    ItemStack item = sender.getInventory().getItem(slot);
    if (item == null || item.getType().isAir()) {
      return null;
    }

    ItemStack itemCopy = item.clone();
    Component itemName = item.displayName().color(this.highlightColor);

    HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.text()
        .color(this.highlightColor)
        .append(Component.text("Get x" + itemCopy.getAmount() + " "))
        .append(itemName)
        .build());

    Options options = Options.builder()
        .uses(ClickCallback.UNLIMITED_USES)
        .lifetime(Duration.ofDays(5))
        .build();

    ClickEvent clickEvent = ClickEvent.callback((Audience clickers) -> {
      clickers
          .filterAudience((Audience audience) -> (audience instanceof Player))
          .forEachAudience((Audience audience) -> {
            Player player = (Player) audience;

            GameMode mode = player.getGameMode();
            if (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) {
              player.sendMessage(Component.text("You must be in creative mode to receive items.", NamedTextColor.RED));
              return;
            }

            player.getInventory().addItem(itemCopy);
            player.sendMessage(Component.text("You received x" + itemCopy.getAmount(), this.highlightColor)
                .append(Component.space())
                .append(itemName));
          });
    }, options);

    return itemName
        .hoverEvent(hoverEvent)
        .clickEvent(clickEvent);
  }

  protected Component sendPlayerCoordinates(Player sender) {
    Location location = sender.getLocation();
    Component coordinates = Component.text()
        .color(this.highlightColor)
        .append(Component.text("("))
        .append(Component.text(location.getBlockX() + ", "))
        .append(Component.text(location.getBlockY() + ", "))
        .append(Component.text(location.getBlockZ()))
        .append(Component.text(")"))
        .build();

    HoverEvent<Component> hoverEvent = HoverEvent
        .showText(Component.text("Teleport here", this.highlightColor));

    Options options = Options.builder()
        .uses(ClickCallback.UNLIMITED_USES)
        .lifetime(Duration.ofDays(5))
        .build();

    ClickEvent clickEvent = ClickEvent.callback((Audience clickers) -> {
      clickers
          .filterAudience((Audience audience) -> (audience instanceof Player))
          .forEachAudience((Audience audience) -> {
            Player player = (Player) audience;

            GameMode mode = player.getGameMode();
            if (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE) {
              player.sendMessage(Component.text("You must be in creative mode to teleport.", NamedTextColor.RED));
              return;
            }

            Location from = player.getLocation();
            Location to = location.clone();
            to.setYaw(from.getYaw());
            to.setPitch(from.getPitch());

            player.teleport(to);
            player.sendMessage(Component.text("You teleported to ", this.highlightColor).append(coordinates));
          });
    }, options);

    return coordinates
        .hoverEvent(hoverEvent)
        .clickEvent(clickEvent);
  }

  @EventHandler
  public void onAsyncChat(AsyncChatEvent event) {
    event.setCancelled(true);

    Player player = event.getPlayer();
    Component message = event.message();

    this.sendFormattedMessage(player, player.displayName(), message);
  }

  @EventHandler
  public void onServerCommand(ServerCommandEvent event) {
    String command = event.getCommand();
    if (!command.startsWith("say")) {
      return;
    }

    event.setCancelled(true);

    Component consoleName = Component.text("Console", Style.style(TextDecoration.BOLD).color(this.consoleColor));
    Component rawMessage = Component.text(command.substring(4).trim());

    this.sendFormattedMessage(null, consoleName, rawMessage);
  }

  @EventHandler
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    String message = event.getMessage();
    if (!message.startsWith("/say")) {
      return;
    }

    event.setCancelled(true);

    Player player = event.getPlayer();
    Component rawMessage = Component.text(message.substring(4).trim());

    this.sendFormattedMessage(player, player.displayName(), rawMessage);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    event.joinMessage(Component.text()
        .decorate(TextDecoration.ITALIC)
        .append(Component.text("⮩").decorate(TextDecoration.BOLD))
        .append(Component.space())
        .append(event.getPlayer().displayName())
        .append(Component.space())
        .append(Component.text("joined the game", this.defaultMessageColor))
        .build());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    event.quitMessage(Component.text()
        .decorate(TextDecoration.ITALIC)
        .append(Component.text("⮨").decorate(TextDecoration.BOLD))
        .append(Component.space())
        .append(event.getPlayer().displayName())
        .append(Component.space())
        .append(Component.text("left the game", this.defaultMessageColor))
        .build());
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    event.deathMessage(Component.text()
        .color(NamedTextColor.RED)
        .append(Component.text("💀"))
        .append(Component.space())
        .append(event.deathMessage().decorate(TextDecoration.ITALIC))
        .append(Component.space())
        .append(Component.text("💀"))
        .build()
        .replaceText(TextReplacementConfig.builder()
            .matchLiteral(event.getPlayer().getName())
            .replacement(event.getPlayer().displayName())
            .build()));
  }
}
