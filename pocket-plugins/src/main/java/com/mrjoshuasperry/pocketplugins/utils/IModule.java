package com.mrjoshuasperry.pocketplugins.utils;

import org.bukkit.configuration.file.YamlConfiguration;

public interface IModule {
    void init(YamlConfiguration config);

    void onDisable();

    String getName();
}