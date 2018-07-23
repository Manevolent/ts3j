package com.github.manevolent.ts3j.command.response;

import com.github.manevolent.ts3j.command.Command;

public class CommandResponse {
    private final Object returnLock = new Object();
    private final Command command;
    private final int returnCode;
    private long dispatchedTime;

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

    public CommandResponse join() throws InterruptedException {
        synchronized (returnLock) {
            returnLock.wait();
        }

        return this;
    }

    public CommandResponse join(int millis) throws InterruptedException {
        synchronized (returnLock) {
            returnLock.wait(millis);
        }

        return this;
    }

    public long getDispatchedTime() {
        return dispatchedTime;
    }

    public void setDispatchedTime(long dispatchedTime) {
        this.dispatchedTime = dispatchedTime;
    }
}
