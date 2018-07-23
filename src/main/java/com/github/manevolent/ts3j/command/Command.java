package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.part.CommandParameter;

public interface Command {
    Iterable<CommandParameter> getParameters();
    void appendParameter(CommandParameter parameter);
}
