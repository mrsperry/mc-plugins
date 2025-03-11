package com.mrjoshuasperry.pocketplugins.utils;

import org.bukkit.entity.Mob;

public interface IPathfindCallback {
  void execute(Mob mob, boolean success);
}
