package com.github.manevolent.ts3j.command.response;

import com.github.manevolent.ts3j.command.CommandException;
import com.github.manevolent.ts3j.command.MultiCommand;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public abstract class AbstractCommandResponse<T> implements CommandResponse<T> {
    private final Function<Iterable<MultiCommand>, T> processor;

    private final CompletableFuture<Iterable<MultiCommand>> future = new CompletableFuture<>();

    public AbstractCommandResponse(Function<Iterable<MultiCommand>, T> processor) {
        this.processor = processor;
    }

    /**
     * Completes the command without any post-mapping processing, lowering overhead.
     */
    @Override
    public void complete() throws InterruptedException, CommandException {
        try {
            future.get();
        } catch (ExecutionException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof CommandException)
                throw (CommandException) ex.getCause();
            else
                throw new RuntimeException(ex);
        }
    }

    @Override
    public T get(long timeoutMillis)
            throws TimeoutException, InterruptedException, CommandException {
        try {
            return map(future.get(timeoutMillis, TimeUnit.MILLISECONDS));
        } catch (ExecutionException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof CommandException)
                throw (CommandException) ex.getCause();
            else
                throw new RuntimeException(ex);
        }
    }

    @Override
    public T get() throws InterruptedException, CommandException {
        try {
            return map(future.get());
        } catch (ExecutionException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof CommandException)
                throw (CommandException) ex.getCause();
            else
                throw new RuntimeException(ex);
        }
    }

    public void completeSuccess(Iterable<MultiCommand> commands) {
        future.complete(commands);
    }

    public void completeFailure(Throwable thrown) {
        future.completeExceptionally(thrown);
    }

    /**
     * Maps the result of the command.  Executes post-retrieval on the end-user thread, a nifty trick to to increase
     * parallel execution speed and handling.
     * @param result result to process.
     * @return mapped result.
     */
    private T map(Iterable<MultiCommand> result) {
        return processor.apply(result);
    }
}
