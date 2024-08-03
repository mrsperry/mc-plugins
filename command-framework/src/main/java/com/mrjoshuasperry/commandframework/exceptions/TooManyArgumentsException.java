package com.mrjoshuasperry.commandframework.exceptions;

public final class TooManyArgumentsException extends CommandException {
    public TooManyArgumentsException() {
        super("Too many arguments");
    }
}
