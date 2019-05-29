package com.github.manevolent.ts3j.command.response;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.CommandException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface CommandResponse<T> {

    Command getCommand();

    default void complete() throws InterruptedException, CommandException {
        get();
    }

    T get(long timeoutMillis) throws TimeoutException, InterruptedException, CommandException;
    T get() throws InterruptedException, CommandException;

}
