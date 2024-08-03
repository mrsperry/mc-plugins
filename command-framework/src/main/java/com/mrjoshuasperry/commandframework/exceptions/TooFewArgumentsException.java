package com.mrjoshuasperry.commandframework.exceptions;

public final class TooFewArgumentsException extends CommandException {
    public TooFewArgumentsException() {
        super("Too few arguments");
    }
}
