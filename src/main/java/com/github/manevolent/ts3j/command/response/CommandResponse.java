package com.github.manevolent.ts3j.command.response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface CommandResponse<T> {

    default void complete()
            throws ExecutionException, InterruptedException {
        get();
    }

    T get(long timeoutMillis) throws TimeoutException, ExecutionException, InterruptedException;
    T get() throws ExecutionException, InterruptedException;

}
