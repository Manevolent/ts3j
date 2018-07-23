package com.github.manevolent.ts3j.command.part;

import com.github.manevolent.ts3j.enums.CommandPartType;

public class CommandSingleParameter implements CommandParameter {
    private final String key, value;

    public CommandSingleParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public CommandSingleParameter(String key, boolean value) {
        this.key = key;
        this.value = serialize(value);
    }

    public CommandSingleParameter(String key, byte value) {
        this.key = key;
        this.value = serialize(value);
    }

    public CommandSingleParameter(String key, short value) {
        this.key = key;
        this.value = serialize(value);
    }

    public CommandSingleParameter(String key, int value) {
        this.key = key;
        this.value = serialize(value);
    }

    public CommandSingleParameter(String key, long value) {
        this.key = key;
        this.value = serialize(value);
    }

    public CommandSingleParameter(String key, float value) {
        this.key = key;
        this.value = serialize(value);
    }

    public CommandSingleParameter(String key, double value) {
        this.key = key;
        this.value = serialize(value);
    }

    @Override
    public CommandPartType getType() {
        return CommandPartType.SINGLE_PARAMETER;
    }

    public static String serialize(boolean value) {
        return value ? "1": "0";
    }

    public static String serialize(byte value) {
        return Byte.toString(value);
    }

    public static String serialize(short value) {
        return Short.toString(value);
    }

    public static String serialize(int value) {
        return Integer.toString(value);
    }

    public static String serialize(long value) {
        return Long.toString(value);
    }

    public static String serialize(float value) {
        return Float.toString(value);
    }

    public static String serialize(double value) {
        return Double.toString(value);
    }

}
