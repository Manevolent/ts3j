package com.github.manevolent.ts3j.command.response;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.ComplexCommand;

import java.util.concurrent.TimeoutException;

public class CommandResponse {
    private final Object returnLock = new Object();
    private final Command command;
    private final int returnCode;

    private long dispatchedTime;
    private ComplexCommand returnedCommand;

    public CommandResponse(Command command, int returnCode) {
        this.command = command;
        this.returnCode = returnCode;
    }

    public Command getCommand() {
        return command;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public ComplexCommand getResponse()
            throws InterruptedException, TimeoutException {
        return getResponse(Integer.MAX_VALUE);
    }

    public ComplexCommand getResponse(int millis)
            throws InterruptedException, TimeoutException {
        if (returnCode < 0) return null;

        synchronized (returnLock) {
            long waited, toWait;

            while (getReturnedCommand() == null) {
                waited = System.currentTimeMillis() - getDispatchedTime();
                toWait = millis - waited;

                if (toWait <= 0) throw new TimeoutException();

                returnLock.wait(toWait);
            }

            return getReturnedCommand();
        }
    }

    public long getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(long dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }

    public ComplexCommand getReturnedCommand() {
        return returnedCommand;
    }

    public void setReturnedCommand(ComplexCommand returnedCommand) {
        synchronized (returnLock) {
            if (this.returnCode < 0 ) throw new IllegalArgumentException("not expecting return command");
            if (this.returnedCommand != null) throw new IllegalArgumentException("already set return command");

            this.returnedCommand = returnedCommand;

            returnLock.notifyAll();
        }
    }
}
