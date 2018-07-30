package com.github.manevolent.ts3j.command;

public class CommandException extends Exception {
    private final int errorId;

    public CommandException(Throwable cause, int errorId) {
        super(cause);
        this.errorId = errorId;
    }

    public CommandException(String message, int errorId) {
        super(message);
        this.errorId = errorId;
    }

    public int getErrorId() {
        return errorId;
    }
}
