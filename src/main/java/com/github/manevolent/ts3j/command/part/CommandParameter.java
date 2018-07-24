package com.github.manevolent.ts3j.command.part;

import java.util.Base64;

public interface CommandParameter {

    String getName();

    void set(char value);
    void set(String value);
    void set(boolean value);
    void set(byte value);
    void set(short value);
    void set(int value);
    void set(long value);
    void set(float value);
    void set(double value);

    default void set(byte[] value) {
        set(Base64.getEncoder().encodeToString(value));
    }

    String getValue();

    /**
     * Converts the command part to a usable command string element.
     * @return Command string element.
     */
    String toString();

}
