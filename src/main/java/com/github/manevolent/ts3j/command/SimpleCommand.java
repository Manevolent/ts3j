package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.part.CommandParameter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SimpleCommand implements Command {
    private final String name;
    private final List<CommandParameter> parameterList = new LinkedList<>();

    public SimpleCommand(String name) {
        this.name = name;
    }

    @Override
    public Iterable<CommandParameter> getParameters() {
        return Collections.unmodifiableList(parameterList);
    }

    @Override
    public void appendParameter(CommandParameter parameter) {
        parameterList.add(parameter);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name);

        for (CommandParameter parameter : getParameters()) {
            builder.append(" ");
            builder.append(parameter.toString());
        }

        return builder.toString();
    }
}
