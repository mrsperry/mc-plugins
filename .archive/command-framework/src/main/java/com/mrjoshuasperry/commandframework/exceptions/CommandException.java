package com.mrjoshuasperry.commandframework.exceptions;

import org.bukkit.ChatColor;

public abstract class CommandException extends Exception {
    public CommandException(final String message) {
        super(ChatColor.RED + message);
    }
}
