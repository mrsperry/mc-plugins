package com.timpcunningham.magic;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Magic extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new MagicTest(), this);
    }

    public static Magic getInstance() {
        return JavaPlugin.getPlugin(Magic.class);
    }
}
