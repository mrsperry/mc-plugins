package com.mrjoshuasperry.chat.names;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mrjoshuasperry.mcutils.TextColors;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class DisplayNameCommand implements Listener {
  protected JavaPlugin plugin;
  protected YamlConfiguration config;
  protected Map<UUID, DisplayNameConfig> displayNameConfigs;

  protected TextColor defaultNameColor;

  public DisplayNameCommand(JavaPlugin plugin) {
    this.plugin = plugin;
    this.config = (YamlConfiguration) this.plugin.getConfig();
    this.displayNameConfigs = new HashMap<>();

    this.defaultNameColor = TextColors.parseTextColor(this.config.getString("default-name-color"),
        TextColor.fromHexString("#6b6bff"));

    ConfigurationSection nameColors = this.config.getConfigurationSection("player-name-configs");
    if (nameColors != null) {
      for (String key : nameColors.getKeys(false)) {
        this.displayNameConfigs.put(UUID.fromString(key),
            DisplayNameConfig.loadFromConfig(nameColors.getConfigurationSection(key)));
      }
    }

    plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
        (ReloadableRegistrarEvent<Commands> event) -> event.registrar().register(this.createCommand().build()));
  }

  protected LiteralArgumentBuilder<CommandSourceStack> createColorCommand(
      SuggestionProvider<CommandSourceStack> suggestColors,
      SuggestionProvider<CommandSourceStack> suggestPosition,
      Command<CommandSourceStack> listColors,
      Command<CommandSourceStack> setColor,
      Command<CommandSourceStack> addColor,
      Command<CommandSourceStack> removeColor,
      Command<CommandSourceStack> clearColors) {
    return Commands
        .literal("color")
        .then(Commands
            .literal("list")
            .executes(listColors))
        .then(Commands
            .literal("add")
            .then(Commands
                .argument("color", StringArgumentType.string())
                .suggests(suggestColors)
                .then(Commands
                    .argument("position", IntegerArgumentType.integer())
                    .suggests(suggestPosition)
                    .executes(setColor))))
        .then(Commands
            .literal("add")
            .then(Commands
                .argument("color", StringArgumentType.string())
                .suggests(suggestColors)
                .executes(addColor)))
        .then(Commands
            .literal("remove")
            .then(Commands
                .argument("position", IntegerArgumentType.integer())
                .suggests(suggestPosition)
                .executes(removeColor)))
        .then(Commands
            .literal("clear")
            .executes(clearColors));
  }

  protected LiteralArgumentBuilder<CommandSourceStack> createCommand() {
    return Commands
        .literal("displayname")
        .requires((CommandSourceStack sender) -> sender.getExecutor() instanceof Player)
        .executes(this::listDisplayName)
        .then(this.createColorCommand(
            this::suggestColors,
            this::suggestNameColorPosition,
            this::listNameColors,
            this::setNameColor,
            this::addNameColor,
            this::removeNameColor,
            this::clearNameColors))
        .then(Commands
            .literal("prefix")
            .executes(this::listPrefix)
            .then(Commands
                .literal("set")
                .then(Commands
                    .argument("value", StringArgumentType.string())
                    .executes(this::setPrefix)))
            .then(Commands
                .literal("clear").executes(this::clearPrefix))
            .then(this.createColorCommand(
                this::suggestColors,
                this::suggestPrefixColorPosition,
                this::listPrefixColors,
                this::setPrefixColor,
                this::addPrefixColor,
                this::removePrefixColor,
                this::clearPrefixColors)))
        .then(Commands
            .literal("suffix")
            .executes(this::listSuffix)
            .then(Commands
                .literal("set")
                .then(Commands
                    .argument("value", StringArgumentType.string())
                    .executes(this::setSuffix)))
            .then(Commands
                .literal("clear")
                .executes(this::clearSuffix))
            .then(this.createColorCommand(
                this::suggestColors,
                this::suggestSuffixColorPosition,
                this::listSuffixColors,
                this::setSuffixColor,
                this::addSuffixColor,
                this::removeSuffixColor,
                this::clearSuffixColors)));
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

  protected List<String> colorsToStrings(List<TextColor> colors) {
    return colors.stream()
        .map(TextColor::toString)
        .toList();
  }

  protected SuggestionsBuilder suggestPosition(SuggestionsBuilder builder, List<TextColor> colors) {
    for (int index = 0; index < colors.size(); index++) {
      builder.suggest(String.valueOf(index + 1));
    }

    return builder;
  }

  protected CompletableFuture<Suggestions> suggestNameColorPosition(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    Player player = (Player) context.getSource().getSender();
    return this
        .suggestPosition(builder,
            this.displayNameConfigs.getOrDefault(player.getUniqueId(), new DisplayNameConfig()).getNameColors())
        .buildFuture();
  }

  protected CompletableFuture<Suggestions> suggestPrefixColorPosition(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    Player player = (Player) context.getSource().getSender();
    return this
        .suggestPosition(builder,
            this.displayNameConfigs.getOrDefault(player.getUniqueId(), new DisplayNameConfig()).getPrefixColors())
        .buildFuture();
  }

  protected CompletableFuture<Suggestions> suggestSuffixColorPosition(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    Player player = (Player) context.getSource().getSender();
    return this
        .suggestPosition(builder,
            this.displayNameConfigs.getOrDefault(player.getUniqueId(), new DisplayNameConfig()).getSuffixColors())
        .buildFuture();
  }

  protected void updateNameColor(Player player) {
    UUID id = player.getUniqueId();
    String originalName = player.getName();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id, new DisplayNameConfig());
    List<TextColor> nameColors = displayNameConfig.getNameColors();
    String prefix = displayNameConfig.getPrefix();
    List<TextColor> prefixColors = displayNameConfig.getPrefixColors();
    String suffix = displayNameConfig.getSuffix();
    List<TextColor> suffixColors = displayNameConfig.getSuffixColors();

    TextComponent.Builder displayName = Component.text();
    String fullName = originalName;

    if (prefixColors.size() == 0 && !prefix.isEmpty()) {
      fullName = prefix + " " + fullName;
    }

    if (suffixColors.size() == 0 && !suffix.isEmpty()) {
      fullName = fullName + " " + suffix;
    }

    if (prefixColors.size() != 0 && !prefix.isEmpty()) {
      displayName.append(TextColors.gradient(prefix.trim(), prefixColors)).append(Component.space());
    }

    if (nameColors.size() == 0) {
      displayName.append(Component.text(fullName.trim(), this.defaultNameColor));
    } else {
      displayName.append(TextColors.gradient(fullName.trim(), nameColors));
    }

    if (suffixColors.size() != 0 && !suffix.isEmpty()) {
      displayName.append(Component.space()).append(TextColors.gradient(suffix.trim(), suffixColors));
    }

    Component displayNameComponent = displayName.build();
    player.displayName(displayNameComponent);
    player.playerListName(displayNameComponent);

    String sectionName = "player-name-configs." + id.toString();
    this.config.set(sectionName + ".name", originalName);
    this.config.set(sectionName + ".name-colors", this.colorsToStrings(nameColors));
    this.config.set(sectionName + ".prefix", prefix);
    this.config.set(sectionName + ".prefix-colors", this.colorsToStrings(prefixColors));
    this.config.set(sectionName + ".suffix", suffix);
    this.config.set(sectionName + ".suffix-colors", this.colorsToStrings(suffixColors));
    this.plugin.saveConfig();
  }

  protected int listDisplayName(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    player.sendMessage(Component.text()
        .append(Component.text("Your display name is: ")).color(NamedTextColor.GRAY)
        .append(player.displayName())
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int listNameColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = this.displayNameConfigs.getOrDefault(id, new DisplayNameConfig()).getNameColors();

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

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getNameColors();

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
    displayNameConfig.setNameColors(colors);

    this.displayNameConfigs.put(id, displayNameConfig);
    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added name color ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int addNameColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getNameColors();

    String color = StringArgumentType.getString(context, "color");

    TextColor textColor = TextColors.parseTextColor(color);
    if (textColor == null) {
      player.sendMessage(Component.text("Invalid color: " + color, NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    colors.add(textColor);
    displayNameConfig.setNameColors(colors);
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added name color: ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int removeNameColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getNameColors();

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
    displayNameConfig.setNameColors(colors);
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Removed name color ", NamedTextColor.GRAY))
        .append(Component.text(oldColor.toString(), oldColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int clearNameColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    displayNameConfig.setNameColors(new ArrayList<>());
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Removed all name colors", NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  protected int listPrefix(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(player.getUniqueId(),
        new DisplayNameConfig());

    String prefix = displayNameConfig.getPrefix();
    if (prefix.isEmpty()) {
      player.sendMessage(Component.text("You have no prefix set", NamedTextColor.GRAY));
    } else {
      player.sendMessage(Component.text("Your prefix is: " + prefix, NamedTextColor.GRAY));
    }

    return Command.SINGLE_SUCCESS;
  }

  protected int setPrefix(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(player.getUniqueId(),
        new DisplayNameConfig());

    String prefix = StringArgumentType.getString(context, "value").trim();

    if (prefix.isEmpty()) {
      player.sendMessage(Component.text("Prefix cannot be empty", NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    displayNameConfig.setPrefix(prefix);
    this.displayNameConfigs.put(player.getUniqueId(), displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Set prefix to: " + prefix, NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  protected int clearPrefix(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(player.getUniqueId(),
        new DisplayNameConfig());

    displayNameConfig.setPrefix("");
    this.displayNameConfigs.put(player.getUniqueId(), displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Removed prefix", NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  public int listPrefixColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = this.displayNameConfigs.getOrDefault(id, new DisplayNameConfig()).getPrefixColors();

    if (colors.size() == 0) {
      player.sendMessage(Component.text("You have no prefix colors set", NamedTextColor.GRAY));
      return Command.SINGLE_SUCCESS;
    }

    TextComponent.Builder colorList = Component.text()
        .append(Component.text("Your prefix colors are: ", NamedTextColor.GRAY))
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

  protected int setPrefixColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getPrefixColors();

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
    displayNameConfig.setPrefixColors(colors);

    this.displayNameConfigs.put(id, displayNameConfig);
    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added prefix color ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int addPrefixColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getPrefixColors();

    String color = StringArgumentType.getString(context, "color");

    TextColor textColor = TextColors.parseTextColor(color);
    if (textColor == null) {
      player.sendMessage(Component.text("Invalid color: " + color, NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    colors.add(textColor);
    displayNameConfig.setPrefixColors(colors);
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added prefix color: ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int removePrefixColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getPrefixColors();

    int index = IntegerArgumentType.getInteger(context, "position") - 1;

    if (colors.size() == 0) {
      player.sendMessage(Component.text("You have no prefix colors set", NamedTextColor.RED));
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
    displayNameConfig.setPrefixColors(colors);
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Removed prefix color ", NamedTextColor.GRAY))
        .append(Component.text(oldColor.toString(), oldColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int clearPrefixColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    displayNameConfig.setPrefixColors(new ArrayList<>());
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Removed all prefix colors", NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  protected int listSuffix(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(player.getUniqueId(),
        new DisplayNameConfig());

    String suffix = displayNameConfig.getSuffix();
    if (suffix.isEmpty()) {
      player.sendMessage(Component.text("You have no suffix set", NamedTextColor.GRAY));
    } else {
      player.sendMessage(Component.text("Your suffix is: " + suffix, NamedTextColor.GRAY));
    }

    return Command.SINGLE_SUCCESS;
  }

  protected int setSuffix(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(player.getUniqueId(),
        new DisplayNameConfig());

    String suffix = StringArgumentType.getString(context, "value").trim();

    if (suffix.isEmpty()) {
      player.sendMessage(Component.text("Suffix cannot be empty", NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    displayNameConfig.setSuffix(suffix);
    this.displayNameConfigs.put(player.getUniqueId(), displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Set suffix to: " + suffix, NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  protected int clearSuffix(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(player.getUniqueId(),
        new DisplayNameConfig());

    displayNameConfig.setSuffix("");
    this.displayNameConfigs.put(player.getUniqueId(), displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Removed suffix", NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  public int listSuffixColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();
    List<TextColor> colors = this.displayNameConfigs.getOrDefault(id, new DisplayNameConfig()).getSuffixColors();

    if (colors.size() == 0) {
      player.sendMessage(Component.text("You have no suffix colors set", NamedTextColor.GRAY));
      return Command.SINGLE_SUCCESS;
    }

    TextComponent.Builder colorList = Component.text()
        .append(Component.text("Your suffix colors are: ", NamedTextColor.GRAY))
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

  protected int setSuffixColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getSuffixColors();

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
    displayNameConfig.setSuffixColors(colors);

    this.displayNameConfigs.put(id, displayNameConfig);
    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added suffix color ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int addSuffixColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getSuffixColors();

    String color = StringArgumentType.getString(context, "color");

    TextColor textColor = TextColors.parseTextColor(color);
    if (textColor == null) {
      player.sendMessage(Component.text("Invalid color: " + color, NamedTextColor.RED));
      return Command.SINGLE_SUCCESS;
    }

    colors.add(textColor);
    displayNameConfig.setSuffixColors(colors);
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Added suffix color: ", NamedTextColor.GRAY))
        .append(Component.text(textColor.toString(), textColor))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int removeSuffixColor(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    List<TextColor> colors = displayNameConfig.getSuffixColors();

    int index = IntegerArgumentType.getInteger(context, "position") - 1;

    if (colors.size() == 0) {
      player.sendMessage(Component.text("You have no suffix colors set", NamedTextColor.RED));
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
    displayNameConfig.setSuffixColors(colors);
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text()
        .append(Component.text("Removed suffix color ", NamedTextColor.GRAY))
        .append(Component.text(oldColor.toString(), oldColor))
        .append(Component.text(" at position " + (index + 1), NamedTextColor.GRAY))
        .build());
    return Command.SINGLE_SUCCESS;
  }

  protected int clearSuffixColors(CommandContext<CommandSourceStack> context) {
    Player player = (Player) context.getSource().getSender();
    UUID id = player.getUniqueId();

    DisplayNameConfig displayNameConfig = this.displayNameConfigs.getOrDefault(id,
        new DisplayNameConfig());
    displayNameConfig.setSuffixColors(new ArrayList<>());
    this.displayNameConfigs.put(id, displayNameConfig);

    this.updateNameColor(player);
    player.sendMessage(Component.text("Removed all suffix colors", NamedTextColor.GRAY));
    return Command.SINGLE_SUCCESS;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    this.updateNameColor(player);
  }
}
