package com.mrjoshuasperry.chat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.mrjoshuasperry.chat.names.DisplayNameCommand;

public class Chat extends JavaPlugin {
  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    PluginManager manager = this.getServer().getPluginManager();
    manager.registerEvents(new MessageHandler((YamlConfiguration) this.getConfig()), this);
    manager.registerEvents(new DisplayNameCommand(this), this);
    manager.registerEvents(new NamePing((YamlConfiguration) this.getConfig()), this);
  }
}
