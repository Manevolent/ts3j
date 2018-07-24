package com.github.manevolent.ts3j.command.part;

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

    /**
     * Converts the command part to a usable command string element.
     * @return Command string element.
     */
    String toString();

}
