package com.mrjoshuasperry.commandframework.exceptions;

public final class PermissionException extends CommandException {
    public PermissionException() {
        super("You do not have permission to use this command");
    }
}
