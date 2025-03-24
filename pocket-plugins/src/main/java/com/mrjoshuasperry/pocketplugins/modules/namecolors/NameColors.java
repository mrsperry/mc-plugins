package com.mrjoshuasperry.pocketplugins.modules.namecolors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mrjoshuasperry.mcutils.TextColors;
import com.mrjoshuasperry.pocketplugins.utils.Module;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class NameColors extends Module {
  protected Map<UUID, List<TextColor>> playerNameColors;

  protected TextColor defaultNameColor;

  public NameColors(ConfigurationSection readableConfig, ConfigurationSection writableConfig) {
    super(readableConfig, writableConfig);

    this.playerNameColors = new HashMap<>();

    this.defaultNameColor = TextColors.parseTextColor(readableConfig.getString("default-name-color"),
        TextColor.fromHexString("#6b6bff"));

    ConfigurationSection nameColors = writableConfig.getConfigurationSection("name-colors");
    if (nameColors != null) {
      for (String key : nameColors.getKeys(false)) {
        this.playerNameColors.put(
            UUID.fromString(key),
            nameColors.getStringList(key)
                .stream()
                .map(TextColors::parseTextColor)
                .toList());
      }
    }

    this.registerCommand(this::createCommand);
  }

  protected LiteralArgumentBuilder<CommandSourceStack> createCommand() {
    return Commands.literal("namecolor")
        .requires((CommandSourceStack sender) -> sender.getExecutor() instanceof Player)
        .then(Commands
            .literal("list")
            .executes(this::listNameColors))
        .then(Commands
            .literal("add")
            .then(Commands.argument("color", StringArgumentType.string())
                .suggests(this::suggestColors)
                .then(Commands.argument("position", IntegerArgumentType.integer())
                    .suggests(this::suggestPosition)
                    .executes(this::setNameColor))))
        .then(Commands
            .literal("add")
            .then(Commands.argument("color", StringArgumentType.string())
                .suggests(this::suggestColors)
                .executes(this::addNameColor)))
        .then(Commands
            .literal("remove")
            .then(Commands.argument("position", IntegerArgumentType.integer())
                .suggests(this::suggestPosition)
                .executes(this::removeNameColor)))
        .then(Commands
            .literal("clear")
            .executes(this::removeAllNameColors));
  }

  protected CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    NamedTextColor.NAMES
        .keys()
        .stream()
        .map((String name) -> name.toLowerCase())
        .filter((String name) -> name.startsWith(builder.getRemainingLowerCase()))
        .forEach(builder::suggest);

    return builder.buildFuture();
  }

  protected CompletableFuture<Suggestions> suggestPosition(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = this.playerNameColors.getOrDefault(id, new ArrayList<>());

    for (int index = 0; index < colors.size(); index++) {
      builder.suggest(String.valueOf(index + 1));
    }

    return builder.buildFuture();
  }

  protected void updateNameColor(Player player) {
    List<TextColor> colors = this.playerNameColors.getOrDefault(player.getUniqueId(), new ArrayList<>());
    String originalName = player.getName();
    Component playerName;
    if (colors.size() == 0) {
      playerName = Component.text(originalName).color(this.defaultNameColor);
    } else {
      playerName = TextColors.gradient(originalName, colors);
    }

    player.displayName(playerName);
    player.playerListName(playerName);

    ConfigurationSection config = this.getWritableConfig();
    config.set("name-colors." + player.getUniqueId().toString(), colors.stream().map(TextColor::toString).toList());
    this.saveConfig();
  }

  protected int listNameColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = this.playerNameColors.getOrDefault(id, new ArrayList<>());

    if (colors.size() == 0) {
      player.sendMessage(Component.text("You have no name colors set", NamedTextColor.GRAY));
      return Command.SINGLE_SUCCESS;
    }

    TextComponent.Builder colorList = Component.text()
        .append(Component.text("Your name colors are: ", NamedTextColor.GRAY))
        .append(Component.text("[", NamedTextColor.GRAY));

    for (int index = 0; index < colors.size(); index++) {
      TextColor color = colors.get(index);

      HoverEvent<Component> hoverEvent = HoverEvent
          .showText(Component.text("Copy to clipboard", NamedTextColor.GREEN));
      ClickEvent clickEvent = ClickEvent.copyToClipboard(color.toString());

      colorList.append(Component.text(color.toString(), color).hoverEvent(hoverEvent).clickEvent(clickEvent));

      if (index != colors.size() - 1) {
        colorList.append(Component.text(", ", NamedTextColor.GRAY));
      }
    }

    colorList.append(Component.text("]", NamedTextColor.GRAY));

    player.sendMessage(colorList.build());
    return Command.SINGLE_SUCCESS;
  }

  protected int setNameColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = new ArrayList<>(this.playerNameColors.getOrDefault(id, new ArrayList<>()));
    String color = StringArgumentType.getString(context, "color");
    int index = IntegerArgumentType.getInteger(context, "position") - 1;

    if (index < 0) {
      index = 0;
    }

    if (index >= colors.size()) {
      index = colors.size();
    }

    TextColor textColor = TextColors.parseTextColor(color);
    if (textColor == null) {
      player.sendMessage(Component.text("Invalid color: " + color, NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    colors.add(index, textColor);
    this.playerNameColors.put(id, colors);
    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added color ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int addNameColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    String color = StringArgumentType.getString(context, "color");
    UUID id = player.getUniqueId();
    List<TextColor> colors = new ArrayList<>(this.playerNameColors.getOrDefault(id, new ArrayList<>()));

    TextColor textColor = TextColors.parseTextColor(color);
    if (textColor == null) {
      player.sendMessage(Component.text("Invalid color: " + color, NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    colors.add(textColor);
    this.playerNameColors.put(id, colors);
    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added color: ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int removeNameColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = new ArrayList<>(this.playerNameColors.getOrDefault(id, new ArrayList<>()));
    int index = IntegerArgumentType.getInteger(context, "position") - 1;

    if (colors.size() == 0) {
      player.sendMessage(Component.text("You have no name colors set", NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    if (index < 0) {
      index = 0;
    }

    if (index >= colors.size()) {
      index = colors.size() - 1;
    }

    TextColor oldColor = colors.get(index);
    colors.remove(index);
    this.playerNameColors.put(id, colors);
    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Removed color ", NamedTextColor.GRAY))
        .append(Component.text(oldColor.toString(), oldColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int removeAllNameColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    this.playerNameColors.remove(id);
    this.updateNameColor(player);
    player.sendMessage(Component.text("Removed all name colors", NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    this.updateNameColor(player);
  }
}
