package com.github.manevolent.ts3j.command.part;

public class CommandOption implements CommandParameter {
    private final String key;
    private boolean enabled = false;

    public CommandOption(String key) {
        this.key = key;
    }


    @Override
    public void set(boolean value) {
        this.enabled = value;
    }

    @Override
    public String getName() {
        return key;
    }

    @Override
    public void set(char value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(String value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void set(byte value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(short value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValue() {
        return enabled ? "true" : "false";
    }

    @Override
    public String toString() {
        if (enabled)
            return "-" + key;
        else
            return "";
    }
}
