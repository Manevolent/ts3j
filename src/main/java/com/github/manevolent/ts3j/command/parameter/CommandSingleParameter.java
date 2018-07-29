package com.github.manevolent.ts3j.command.parameter;

import com.github.manevolent.ts3j.util.Ts3String;

public class CommandSingleParameter implements CommandParameter {
    private final String key;
    private String value;

    public CommandSingleParameter(String key) {
        this.key = key;
    }

    public CommandSingleParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        if (value == null)
            return "";
        else
            return (key + "=" + Ts3String.escape(value.trim()));
    }

    @Override
    public String getName() {
        return key;
    }

    @Override
    public void set(char value) {
        this.value = Character.toString(value);
    }

    @Override
    public void set(String value) {
        this.value = value;
    }

    @Override
    public void set(boolean value) {
        this.value = (value ? "1" : "0");
    }

    @Override
    public void set(byte value) {
        this.value = Byte.toString(value);
    }

    @Override
    public void set(short value) {
        this.value = Short.toString(value);
    }

    @Override
    public void set(int value) {
        this.value = Integer.toString(value);
    }

    @Override
    public void set(long value) {
        this.value = Long.toString(value);
    }

    @Override
    public void set(float value) {
        this.value = Float.toString(value);
    }

    @Override
    public void set(double value) {
        this.value = Double.toString(value);
    }

    @Override
    public String getValue() {
        return value;
    }
}
