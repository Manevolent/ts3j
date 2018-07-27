package com.github.manevolent.ts3j.command;

public class CommandProcessException extends Exception {
    public CommandProcessException(Exception cause) {
        super(cause);
    }

    public CommandProcessException(String message) {
        super(message);
    }
}
